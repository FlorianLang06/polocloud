package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.common.configuration.ConfigSection
import dev.httpmarco.polocloud.common.grpc.GrpcEndpoint
import dev.httpmarco.polocloud.node.database.credentials.DatabaseCredentials
import dev.httpmarco.polocloud.node.database.credentials.DatabaseCredentialsConfigurationAdapter
import dev.httpmarco.polocloud.node.database.credentials.SqlDatabaseCredentials
import dev.httpmarco.polocloud.node.database.sql.SqlConnectionFactoryPart
import java.lang.reflect.Type
import java.util.UUID

/**
 * Singleton representing the local PoloCloud node.
 *
 * Responsible for:
 * 1. Loading configuration
 * 2. Initializing GRPC endpoint
 * 3. Establishing database connection
 */
object LocalNode {

    /** Node configuration, loaded lazily from JSON */
    val config: LocalNodeConfiguration by lazy { generateConfiguration() }

    /** GRPC endpoint for inter-node communication */
    val endpoint: GrpcEndpoint by lazy { GrpcEndpoint(config.bindAddress.port) }

    /** SQL database connection factory */
    val databaseConnectionFactory: SqlConnectionFactoryPart by lazy { SqlConnectionFactoryPart() }

    init {
        // connect GRPC endpoint
        endpoint.connect()

        // connect database using credentials from configuration
        databaseConnectionFactory.globalConnect(config.database)
    }

    /**
     * Generates the configuration by loading the JSON file.
     *
     * Uses the Config system with Object Mapping to map into [LocalNodeConfiguration].
     */
    private fun generateConfiguration(): LocalNodeConfiguration {
        val section = ConfigSection(LOCAL_NODE_PATH).withMapping(
            DatabaseCredentials::class.java,
            DatabaseCredentialsConfigurationAdapter()
        )

        return section.readOrCreate(
            LocalNodeConfiguration(
                UUID.randomUUID().toString(), Address("0.0.0.0", 8325),
                SqlDatabaseCredentials("postgresql", Address("localhost", 5432), "postgres", "test123", "polo")
            )
        )
    }
}
