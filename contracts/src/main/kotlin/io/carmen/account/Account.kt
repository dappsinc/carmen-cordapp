package io.carmen.account

import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException
import java.security.PublicKey



/**
 * The state object recording CRM assets between two parties.
 *
 * A state must implement [Account] or one of its descendants.
 *
 * @Param name of the Account.
 * @param type of the Account.
 * @param industry of the Account.
 * @param rating of the Account.
 * @param phone of the Account.
 * @param controller the party who owns the Account.
 */


@CordaSerializable
@BelongsToContract(AccountContract::class)
data class Account(val accountId: String,
                   val accountName: String,
                   val accountType: String,
                   val industry: String,
                   val phone: String,
                   val controller: Party,
                   val processor: Party ) : ContractState, QueryableState {


    override val participants: List<AbstractParty> get() = listOf(controller, processor)

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(AccountSchemaV1)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is AccountSchemaV1 -> AccountSchemaV1.PersistentAccount(
                    accountId = this.accountId,
                    accountName = this.accountName,
                    accountType = this.accountType,
                    industry = this.industry,
                    phone = this.phone,
                    controller = this.controller.toString(),
                    processor = this.processor.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }
}

class AccountContract : Contract {

    companion object {
        val ACCOUNT_CONTRACT_ID = AccountContract::class.java.canonicalName
    }

    interface Commands : CommandData {
        class CreateAccount : TypeOnlyCommandData(), Commands
        class TransferAccount : TypeOnlyCommandData(), Commands
        class ShareAccount : TypeOnlyCommandData(), Commands
        class DeleteAccount : TypeOnlyCommandData(), Commands


    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is Commands.CreateAccount -> verifyCreate(tx, setOfSigners)
            is Commands.TransferAccount -> verifyTransfer(tx, setOfSigners)
            is Commands.ShareAccount -> verifyShare(tx, setOfSigners)
            is Commands.DeleteAccount -> verifyDelete(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "No inputs must be consumed." using (tx.inputStates.isEmpty())
        "Only one out state should be created." using (tx.outputStates.size == 1)
        val output = tx.outputsOfType<Account>().single()
        "Owner only may sign the Account issue transaction." using (output.controller.owningKey in signers)
    }

    private fun verifyTransfer(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val inputAccounts = tx.inputsOfType<Account>()
        //val inputAccountTransfers = tx.inputsOfType<AccountTransfer>()
        "There must be one input Account." using (inputAccounts.size == 1)


        val inputAccount = inputAccounts.single()
        val outputs = tx.outputsOfType<Account>()
        "There must be one output Account." using (outputs.size == 1)


        val output = outputs.single()
        "Must not not change Account data except field value." using (inputAccount == output.copy(controller = inputAccount.controller))
        "Owner only may sign the Account transfer transaction." using (output.controller.owningKey in signers)
    }


    private fun verifyShare(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "Only one Account should be shared." using (tx.inputStates.size == 1)
        "Only one Account should be shared." using (tx.outputStates.size == 1)
        val output = tx.outputsOfType<Account>().single()
        "Owner only may sign the Account share transaction." using (output.controller.owningKey in signers)
    }


    private fun verifyDelete(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "No inputs must be consumed." using (tx.inputStates.size == 1)
        "There should be no output state" using (tx.outputStates.size == 0)
        val output = tx.outputsOfType<Account>().single()
        "Owner only may sign the Account issue transaction." using (output.controller.owningKey in signers)
    }
}