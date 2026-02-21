package dev.httpmarco.polocloud.database

import dev.httpmarco.polocloud.common.Closeable
import dev.httpmarco.polocloud.database.nosql.redis.RedisConnectionFactory
import dev.httpmarco.polocloud.database.sql.SqlExecutor
import dev.httpmarco.polocloud.i18n.api.TranslationService
import org.apache.logging.log4j.LogManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Abstract factory responsible for creating and managing a database connection.
 *
 * @param T Type of the database credentials
 *
 * This class encapsulates:
 * - establishing a database connection
 * - providing an SQL executor
 * - basic validation of the connection state
 */
abstract class DatabaseConnectionFactory<C : DatabaseCredentials>(private val credentials : DatabaseCredentials)  : Closeable {

    /**
     * Logger used for database connection lifecycle messages.
     */
    protected val logger: Logger = LoggerFactory.getLogger(DatabaseConnectionFactory::class.java)

    /**
     * Current state of the database connection.
     *
     * Initially set to [DatabaseState.UNKNOWN] and should be updated
     * by concrete implementations when connecting or closing the connection.
     */
    var state = DatabaseState.UNKNOWN

    /**
     * Establishes a connection to the database using the given credentials.
     *
     * @param credentials database access credentials
     */
    abstract fun connect(credentials: C = this.credentials as C)

    /**
     * Returns an SQL executor used to execute queries and updates.
     *
     * @return a database-specific [SqlExecutor] implementation
     */
    abstract fun executor(): DatabaseExecutor

    /**
     * Checks whether the database connection is currently valid.
     *
     * @return true if the connection state is [DatabaseState.CONNECTED], otherwise false
     *
     * If the database is not connected, a log message is emitted to prevent
     * invalid database operations.
     */
    fun isValid(): Boolean {
        if (state != DatabaseState.CONNECTED) {
            logger.info(TranslationService.tr("database", "database.connection.invalid_state", "state" to state.name))
            return false
        }
        return true
    }
}