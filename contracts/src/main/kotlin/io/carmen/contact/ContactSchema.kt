package io.carmen.contact

import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Index
import javax.persistence.Table

/**
 * The family of schemas for [ContactSchema].
 */

object ContactSchema

/**
 * First version of an [ContactSchema] schema.
 */


object ContactSchemaV1 : MappedSchema(ContactSchema.javaClass, 1, listOf(PersistentContact::class.java)) {
    @Entity
    @Table(name = "contacts", indexes = arrayOf(Index(name = "idx_contact_controller", columnList = "controller"),
            Index(name = "idx_contact_lastName", columnList = "lastName")))
    class PersistentContact(
            @Column(name = "contactId")
            var contactId: String,

            @Column(name = "firstName")
            var firstName: String,

            @Column(name = "lastName")
            var lastName: String,

            @Column(name = "phone")
            var phone: String,

            @Column(name = "email")
            var email: String,

            @Column(name = "controller")
            var controller: String,

            @Column(name = "processor")
            var processor: String,

            @Column(name = "linearId")
            var linearId: String

    ) : PersistentState() {
        constructor() : this("", "default-constructor-required-for-hibernate", "", "", "", "", "", "")
    }

}