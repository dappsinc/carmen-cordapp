package io.carmen.case

import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException

// *********
// * Case State *
// *********

@CordaSerializable
@BelongsToContract(CaseContract::class)
data class Case(val caseId: String,
                val description: String,
                val caseNumber: String,
                val caseStatus: CaseStatus,
                val casePriority: CasePriority,
                val submitter: Party,
                val resolver: Party,
                override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {


    override val participants = listOf(submitter, resolver)

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(CaseSchemaV1)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is CaseSchemaV1 -> CaseSchemaV1.PersistentCase(
                    caseId = this.caseId,
                    description = this.description,
                    caseNumber = this.caseNumber,
                    caseStatus = this.caseStatus.toString(),
                    casePriority = this.casePriority.toString(),
                    submitter = this.submitter.toString(),
                    resolver = this.resolver.toString(),
                    linearId = this.linearId.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

}

enum class CaseStatus {
    NEW, UNSTARTED, STARTED, WORKING, ESCALATED, CLOSED, OUTOFSCOPE
}

enum class CasePriority {
    HIGH, MEDIUM, LOW

}



// *****************
// * Contract Code *
// *****************

class CaseContract : Contract {

    companion object {
        val CASE_CONTRACT_ID = CaseContract::class.java.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        val caseInputs = tx.inputsOfType<Case>()
        val caseOutputs = tx.outputsOfType<Case>()
        val caseCommand = tx.commandsOfType<CaseContract.Commands>().single()

        when (caseCommand.value) {
            is Commands.SubmitCase -> requireThat {
                "no inputs should be consumed" using (caseInputs.isEmpty())
                // TODO we might allow several jobs to be proposed at once later
                "one output should be produced" using (caseOutputs.size == 1)

                val caseOutput = caseOutputs.single()
                "the submitter should be different to the resolver" using (caseOutput.resolver != caseOutput.submitter)
                //  "the status should be set as unstarted" using (caseOutput.caseStatus == CaseStatus.UNSTARTED)

                "the resolver and submitter are required signer" using
                        (caseCommand.signers.containsAll(listOf(caseOutput.resolver.owningKey, caseOutput.submitter.owningKey)))
            }

            is Commands.StartCase -> requireThat {
                "one input should be consumed" using (caseInputs.size == 1)
                "one output should bbe produced" using (caseOutputs.size == 1)

                val caseInput = caseInputs.single()
                val caseOutput = caseOutputs.single()
                // "the status should be set to started" using (caseOutput.caseStatus == CaseStatus.STARTED)
                //  "the previous status should not be STARTED" using (caseInput.caseStatus != CaseStatus.STARTED)
                //  "only the job status should change" using (caseOutput == caseInput.copy(caseStatus = CaseStatus.STARTED))
                "the submitter and resolver are required signers" using
                        (caseCommand.signers.containsAll(listOf(caseOutput.resolver.owningKey, caseOutput.submitter.owningKey)))
            }

            is Commands.CloseCase -> requireThat {
                "one input should be produced" using (caseInputs.size == 1)
                "one output should be produced" using (caseOutputs.size == 1)

                val caseInput = caseInputs.single()
                val caseOutput = caseOutputs.single()

                //    "the input status must be set as started" using (caseInputs.single().caseStatus == CaseStatus.STARTED)
                //   "the output status should be set as finished" using (caseOutputs.single().caseStatus == CaseStatus.CLOSED)
                //   "only the status must change" using (caseInput.copy(caseStatus = CaseStatus.CLOSED) == caseOutput)
                "the update must be signed by the contractor of the " using (caseOutputs.single().submitter == caseInputs.single().submitter)
                "the submitter should be signer" using (caseCommand.signers.contains(caseOutputs.single().submitter.owningKey))

            }

            is Commands.EscalateCase -> requireThat {

            }

            is Commands.CloseOutOfScopeCase -> requireThat {
                // This insures we only have one input and one output
                val caseOutput = caseOutputs.single()
                val caseInput = caseInputs.single()

                "Only status should have changed" using (caseOutput.submitter == caseInput.submitter
                        && caseOutput.resolver == caseInput.resolver
                        && caseOutput.description == caseInput.description)
                //     "Status should show rejected" using (caseOutput.caseStatus == CaseStatus.OUTOFSCOPE)
                //      "Job must have been previously started" using (caseInput.caseStatus == CaseStatus.STARTED)

                "Resolver should be a signer" using (caseCommand.signers.contains(caseOutput.resolver.owningKey))
            }

            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class SubmitCase : Commands
        class StartCase : Commands
        class CloseCase : Commands
        class EscalateCase : Commands
        class CloseOutOfScopeCase : Commands

    }
}