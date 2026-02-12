package dev.httpmarco.polocloud.database.sql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseState
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Abstract SQL connection factory using HikariCP as the connection pool.
 * Provides methods to connect to a SQL database and manage the connection pool.
 *
 * @see dev.httpmarco.polocloud.database.DatabaseConnectionFactory
 */
class SqlConnectionFactoryPart : DatabaseConnectionFactory<SqlDatabaseCredentials>() {

    companion object {
        private val logger: Logger = LogManager.getLogger(SqlConnectionFactoryPart::class.java)
    }

    private val executor = SqlExecutor(this)
    var dataSource: HikariDataSource? = null

    /**
     * Connects to the SQL database using the provided credentials.
     *
     * @param credentials The SQL database credentials.
     */
    override fun connect(credentials: SqlDatabaseCredentials) {
        state = DatabaseState.CONNECTING

        logger.info(TranslationService.tr("database", "database.connection.connecting"))

        try {
            this.dataSource = createHikariDataSource(
                jdbcUrl = "jdbc:${credentials.driver}://${credentials.address.asString()}/${credentials.database}",
                username = credentials.username,
                password = credentials.password
            )

            state = DatabaseState.CONNECTED

            logger.info(
                TranslationService.tr(
                    "database",
                    "database.connection.established",
                    "driver" to credentials.driver
                )
            )
        } catch (exception: Exception) {
            state = DatabaseState.FAILED
            logger.error(TranslationService.tr("database", "database.connection.failed"), exception)
        }
    }

    override fun executor() = executor

    /**
     * Creates a HikariCP DataSource with the given configuration.
     *
     * @param jdbcUrl JDBC URL of the database.
     * @param username Database username.
     * @param password Database password.
     * @return Initialized HikariCP DataSource.
     */
    fun createHikariDataSource(
        jdbcUrl: String,
        username: String,
        password: String,
    ): HikariDataSource {
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password

            this.maximumPoolSize = 10
            this.minimumIdle = 2
            this.idleTimeout = 600_000
            this.maxLifetime = 1_800_000
            this.connectionTimeout = 30_000
            this.validationTimeout = 5_000
            this.poolName = "PoloCloudPool"
        }

        logger.info(
            TranslationService.tr(
                "database",
                "database.pool.initializing",
                "pool" to config.poolName
            )
        )

        val hikariDataSource = HikariDataSource(config)

        logger.info(
            TranslationService.tr(
                "database",
                "database.pool.initialized",
                "pool" to config.poolName,
                "maxPoolSize" to config.maximumPoolSize
            )
        )

        return hikariDataSource
    }

    /**
     * Closes the DataSource and releases all connections.
     */
    override fun close() {
        dataSource?.let {

            logger.info(
                TranslationService.tr(
                    "database",
                    "database.pool.shutdown",
                    "pool" to it.poolName
                )
            )

            try {
                it.close()

                state = DatabaseState.CLOSED

                logger.info(
                    TranslationService.tr(
                        "database",
                        "database.pool.close_failed",
                        "pool" to it.poolName
                    )
                )

                logger.info(
                    TranslationService.tr(
                        "database",
                        "database.connection.closed"
                    )
                )
            } catch (e: Exception) {
                logger.error(
                    TranslationService.tr(
                        "database",
                        "database.pool.close_failed",
                        "pool" to it.poolName
                    ),
                    e
                )
            } finally {
                dataSource = null
            }
        }
    }

}
