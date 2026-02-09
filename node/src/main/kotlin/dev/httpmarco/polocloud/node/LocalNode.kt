package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.configuration.FileConfigSource
import dev.httpmarco.polocloud.common.grpc.GrpcEndpoint
import dev.httpmarco.polocloud.config.ConfigLoader
import dev.httpmarco.polocloud.node.database.sql.SqlConnectionFactoryPart
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
        databaseConnectionFactory.connect(config.database)
    }

    /**
     * Generates the configuration by loading the JSON file.
     *
     * Uses the Config system with Object Mapping to map into [LocalNodeConfiguration].
     */
    private fun generateConfiguration(): LocalNodeConfiguration {
        val loader = ConfigLoader()
            .addSource(FileConfigSource(LOCAL_NODE_PATH))
            .load()

        return loader.root().asObject()
    }
}
