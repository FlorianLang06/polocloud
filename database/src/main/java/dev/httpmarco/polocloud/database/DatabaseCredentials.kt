package dev.httpmarco.polocloud.database

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.nosql.mongo.MongoConnectionFactory
import dev.httpmarco.polocloud.database.nosql.redis.RedisConnectionFactory
import dev.httpmarco.polocloud.database.sql.SqlConnectionFactory
import kotlinx.serialization.Serializable

@Serializable
sealed class DatabaseCredentials {

    abstract val address: Address

    abstract fun factory(): DatabaseConnectionFactory<*>

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
    ) : DatabaseRelated() {

        override fun factory(): DatabaseConnectionFactory<*> {
            return SqlConnectionFactory(this)
        }

    }

    @Serializable
    data class MongoDB(
        override val address: Address,
        override val username: String,
        override val password: String,
        override val database: String
    ) : DatabaseRelated() {

        override fun factory(): DatabaseConnectionFactory<*> {
            return MongoConnectionFactory(this)
        }


    }

    @Serializable
    data class Redis(
        override val address: Address,
        val username: String,
        val password: String?
    ) : DatabaseCredentials() {

        override fun factory(): DatabaseConnectionFactory<*> {
            return RedisConnectionFactory(this)
        }

    }

    @Serializable
    data class PostgreSQL(
        override val address: Address,
        override val username: String,
        override val password: String,
        override val database: String
    ) : DatabaseRelated() {
        override fun factory(): DatabaseConnectionFactory<*> {
            return SqlConnectionFactory(this)
        }
    }

    @Serializable
    data class H2(
        val path: String
    ) : DatabaseCredentials() {
        override val address: Address get() = Address("localhost", 0)

        override fun factory(): DatabaseConnectionFactory<*> {
            return SqlConnectionFactory(this)
        }
    }

    @Serializable
    data class Mysql(
        override val address: Address,
        override val username: String,
        override val password: String,
        override val database: String
    ) : DatabaseRelated() {
        override fun factory(): DatabaseConnectionFactory<*> {
            return SqlConnectionFactory(this)
        }
    }
}