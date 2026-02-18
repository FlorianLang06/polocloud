package dev.httpmarco.polocloud.database

import dev.httpmarco.polocloud.common.Address
import kotlinx.serialization.Serializable

@Serializable
sealed class DatabaseCredentials {

    abstract val address: Address

    @Serializable
    abstract class DatabaseRelated : DatabaseCredentials() {
        abstract val username: String
        abstract val password: String
        abstract val database: String
    }

    @Serializable
    data class MariaDB(
        override val address: Address,
        override val username: String,
        override val password: String,
        override val database: String
    ) : DatabaseRelated()

    @Serializable
    data class MongoDB(
        override val address: Address,
        override val username: String,
        override val password: String,
        override val database: String
    ) : DatabaseRelated()

    @Serializable
    data class Redis(
        override val address: Address,
        val username: String,
        val password: String?
    ) : DatabaseCredentials()

    @Serializable
    data class PostgreSQL(
        override val address: Address,
        override val username: String,
        override val password: String,
        override val database: String
    ) : DatabaseRelated()

    @Serializable
    data class H2(
        val path: String
    ) : DatabaseCredentials() {
        override val address: Address get() = Address("localhost", 0)
    }

    @Serializable
    data class Mysql(
        override val address: Address,
        override val username: String,
        override val password: String,
        override val database: String
    ) : DatabaseRelated()
}