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

    /**
     * Shutdown handler responsible for graceful shutdown logic and JVM shutdown hook registration.
     */
    val shutdownHandler = NodeShutdownHandler(this)

    init {
        TranslationService.init()

        // Load persisted configuration (or create default if missing)
        config = loadConfiguration()

        // Prepare networking endpoint based on configuration
        endpoint = GrpcEndpoint(resolveBindAddress())

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
    @Synchronized
    fun start() {
        if (cluster.localNodeState != NodeState.OFFLINE) {
            // Node is already started or in the process of starting, ignore subsequent start calls
            throw IllegalStateException("Node is already starting or started. Current state: ${cluster.localNodeState}")
        }

        try {
            initializeNetwork()
            initializeCluster()
        } catch (ex: Exception) {
            // If any initialization step fails, ensure we attempt to clean up resources before rethrowing the exception
            close(ShutdownMode.GRACEFUL)
            throw ex
        }
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
    @Synchronized
    override fun close(mode: ShutdownMode) {
        if (cluster.localNodeState == NodeState.OFFLINE) {
            // Node was never started, nothing to do
            return
        }

        if (cluster.localNodeState == NodeState.STOPPING || cluster.localNodeState == NodeState.STOPPED) {
            return
        }

        cluster.markStopping()
        endpoint.close(mode)
        cluster.markStopped()
        cluster.close(mode)

        if (!shutdownHandler.running) {
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

    /**
     * Resolves the effective bind address of this node.
     *
     * <p>Resolution priority:</p>
     * <ol>
     *     <li>Address provided via {@link NodeLaunchConfig}</li>
     *     <li>Bind address from persisted configuration</li>
     * </ol>
     *
     * @return the resolved {@link Address} used for the gRPC endpoint
     */
    private fun resolveBindAddress(): Address {
        val launchAddress = launchConfig.address
        val defaultAddress = config.bindAddress

        val hostname = launchAddress.hostname.takeIf { it.isNotBlank() }
            ?: defaultAddress.hostname

        val port = launchAddress.port.takeIf { it > 0 }
            ?: defaultAddress.port

        return Address(hostname, port)
    }
}