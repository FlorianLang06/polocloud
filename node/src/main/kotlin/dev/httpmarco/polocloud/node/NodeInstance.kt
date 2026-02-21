package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.common.Closeable
import dev.httpmarco.polocloud.common.ShutdownMode
import dev.httpmarco.polocloud.common.configuration.ConfigSection
import dev.httpmarco.polocloud.common.grpc.GrpcEndpoint
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.node.launch.NodeLaunchConfig
import dev.httpmarco.polocloud.node.cluster.Cluster
import dev.httpmarco.polocloud.node.cluster.node.NodeState
import dev.httpmarco.polocloud.node.configuration.NodeInstanceConfiguration
import kotlin.system.exitProcess

/**
 * Represents a running PoloCloud node instance.
 *
 * Responsibilities:
 * - Load and manage node configuration
 * - Initialize and manage the gRPC endpoint
 * - Join and interact with the cluster
 *
 * Lifecycle:
 * 1. Constructed with bootstrap information
 * 2. Configuration is loaded
 * 3. start() initializes networking and cluster integration
 * 4. close() gracefully shuts down the node
 */
class NodeInstance(
    /**
     * Bootstrap information created during application startup.
     * Contains resolved filesystem paths and runtime launch parameters.
     */
    private val launchConfig: NodeLaunchConfig
) : Closeable {

    /**
     * Node configuration loaded from the local node configuration file.
     */
    val config: NodeInstanceConfiguration

    /**
     * gRPC endpoint used for inter-node communication.
     */
    val endpoint: GrpcEndpoint

    /**
     * Cluster abstraction handling node discovery and cluster state.
     */
    val cluster: Cluster

    init {
        NodeShutdownHandler(this).registerShutdownHook()

        TranslationService.init()

        // Load persisted configuration (or create default if missing)
        config = loadConfiguration()

        // Prepare networking endpoint based on configuration
        endpoint = GrpcEndpoint(detectAddress())

        // Initialize cluster component
        cluster = Cluster(config, launchConfig)
    }

    /**
     * Starts the node instance.
     *
     * This will:
     * - Connect the gRPC endpoint
     * - Perform cluster detection
     * - Mark this node as online in the cluster
     */
    fun start() {
        initializeNetwork()
        initializeCluster()

        Thread.currentThread().join()
    }

    /**
     * Gracefully shuts down the node instance.
     *
     * Intended for:
     * - JVM shutdown hooks
     * - Kubernetes SIGTERM handling
     * - Manual restarts
     *
     * Currently not implemented.
     */
    override fun close(mode : ShutdownMode) {
        if (cluster.state() == NodeState.STOPPING || cluster.state() == NodeState.STOPPED) {
            return
        }

        cluster.markStopping()
        // TODO:
        // 2. Shutdown cluster services
        endpoint.close(mode)

        cluster.markStopped()

        if(!NodeShutdownHandler.shutdownProcess) {
            exitProcess(0)
        }
    }

    /**
     * Establishes the gRPC connection.
     */
    private fun initializeNetwork() {
        endpoint.connect()
    }

    /**
     * Performs cluster discovery and marks the node as online.
     */
    private fun initializeCluster() {
        cluster.detect()
        cluster.markOnline()
    }

    /**
     * Loads the node configuration from disk.
     *
     * If no configuration file exists, a default configuration
     * will be created and persisted automatically.
     */
    private fun loadConfiguration(): NodeInstanceConfiguration {
        return ConfigSection(launchConfig.localNodePath).readOrCreate(
            NodeInstanceConfiguration.serializer(),
            NodeInstanceConfiguration(database = DatabaseCredentials.H2(launchConfig.localDataPath.toString() + "/polocloud.h2.db"))
        )
    }

    private fun detectAddress(): Address {
        val launchAddress = launchConfig.address
        val defaultAddress = config.bindAddress

        val hostname = launchAddress.hostname.takeIf { it.isNotEmpty() } ?: defaultAddress.hostname
        val port = launchAddress.port.takeIf { it != 1 } ?: defaultAddress.port

        return Address(hostname, port)
    }
}