package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.common.configuration.ConfigSection
import dev.httpmarco.polocloud.common.grpc.GrpcEndpoint
import dev.httpmarco.polocloud.node.cluster.external.database.credentials.DatabaseCredentials
import dev.httpmarco.polocloud.node.cluster.external.database.credentials.DatabaseCredentialsConfigurationAdapter
import dev.httpmarco.polocloud.node.cluster.external.database.credentials.SqlDatabaseCredentials
import java.util.UUID

/**
 * Singleton representing the local PoloCloud node.
 *
 * Responsible for:
 * 1. Loading configuration
 * 2. Initializing GRPC endpoint
 * 3. Establishing database connection
 */
object NodeInstance {

    /** Node configuration, loaded lazily from JSON */
    val config: NodeInstanceConfiguration by lazy { generateConfiguration() }

    /** GRPC endpoint for inter-node communication */
    val endpoint: GrpcEndpoint by lazy { GrpcEndpoint(config.bindAddress.port) }

    init {
        // connect GRPC endpoint
        endpoint.connect()
    }

    /**
     * Generates the configuration by loading the JSON file.
     *
     * Uses the Config system with Object Mapping to map into [NodeInstanceConfiguration].
     */
    private fun generateConfiguration(): NodeInstanceConfiguration {
        val section = ConfigSection(LOCAL_NODE_PATH).withMapping(
            DatabaseCredentials::class.java,
            DatabaseCredentialsConfigurationAdapter()
        )

        return section.readOrCreate(
            NodeInstanceConfiguration(
                UUID.randomUUID().toString(), Address("0.0.0.0", 8325),
                SqlDatabaseCredentials("postgresql", Address("localhost", 5432), "postgres", "test123", "polo")
            )
        )
    }
}
