package dev.httpmarco.polocloud.node.database.sql

import dev.httpmarco.polocloud.node.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.node.database.credentials.SqlDatabaseCredentials
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.httpmarco.polocloud.node.database.DatabaseState
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import javax.sql.DataSource

/**
 * Abstract SQL connection factory using HikariCP as the connection pool.
 * Provides methods to connect to a SQL database and manage the connection pool.
 *
 * @see DatabaseConnectionFactory
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
        this.dataSource = createHikariDataSource(
            jdbcUrl = "jdbc:${credentials.driver}://${credentials.hostname}:${credentials.port}/${credentials.database}",
            username = credentials.username,
            password = credentials.password
        )
        state = DatabaseState.CONNECTED
        logger.info("Connected to database at {}", credentials.driver)
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

        val hikariDataSource = HikariDataSource(config)
        logger.info("HikariCP pool '{}' initialized with maxPoolSize={}", config.poolName, config.maximumPoolSize)
        return hikariDataSource
    }

    /**
     * Closes the DataSource and releases all connections.
     */
    override fun close() {
        state = DatabaseState.CLOSED
        dataSource?.let {
            try {
                it.close()
                logger.info("HikariCP pool '{}' closed successfully", it.poolName)
            } catch (e: Exception) {
                logger.error("Failed to close HikariCP pool '{}'", it.poolName, e)
            } finally {
                dataSource = null
            }
        }
    }

}
