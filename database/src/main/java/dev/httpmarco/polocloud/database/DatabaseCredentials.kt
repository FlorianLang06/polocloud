package dev.httpmarco.polocloud.database

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.nosql.mongo.MongoConnectionFactoryPart
import dev.httpmarco.polocloud.database.nosql.redis.RedisConnectionFactoryPart
import dev.httpmarco.polocloud.database.sql.SqlConnectionFactoryPart
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
            return SqlConnectionFactoryPart(this)
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
            return MongoConnectionFactoryPart(this)
        }


    }

    @Serializable
    data class Redis(
        override val address: Address,
        val username: String,
        val password: String?
    ) : DatabaseCredentials() {

        override fun factory(): DatabaseConnectionFactory<*> {
            return RedisConnectionFactoryPart(this)
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
            return SqlConnectionFactoryPart(this)
        }
    }

    @Serializable
    data class H2(
        val path: String
    ) : DatabaseCredentials() {
        override val address: Address get() = Address("localhost", 0)

        override fun factory(): DatabaseConnectionFactory<*> {
            return SqlConnectionFactoryPart(this)
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
            return SqlConnectionFactoryPart(this)
        }
    }
}