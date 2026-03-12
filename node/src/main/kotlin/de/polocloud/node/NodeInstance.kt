package de.polocloud.node

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.configuration.ConfigSection
import de.polocloud.common.i18n.trInfo
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.launch.NodeLaunchConfig
import de.polocloud.node.configuration.NodeInstanceConfiguration
import de.polocloud.node.node.NodeState
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.slf4j.LoggerFactory
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
    val launchConfig: NodeLaunchConfig
) : Closeable {

    private val logger = LoggerFactory.getLogger(NodeInstance::class.java)

    /**
     * Node configuration loaded from the local node configuration file.
     */
    val config: NodeInstanceConfiguration

    /**
     * Cluster abstraction handling node discovery and cluster state.
     */
    val cluster: Cluster

    /**
     * Shutdown handler responsible for graceful shutdown logic and JVM shutdown hook registration.
     */
    val shutdownHandler = NodeShutdownHandler(this)

    /**
     * Database connection factory resolved from launch parameters or persisted configuration.
     */
    val database: DatabaseConnectionFactory<*>

    init {
        TranslationService.init()

        // Load persisted configuration (or create default if missing)
        config = loadConfiguration()

        TranslationService.defaultLanguage(config.language)
        TranslationService.preloadAsync("database")

        database = resolveDatabaseCredentials().factory()

        // Initialize cluster component
        cluster = Cluster(database, resolveBindAddress(), launchConfig)
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
        if (cluster.state() != NodeState.OFFLINE) {
            // Node is already started or in the process of starting, ignore subsequent start calls
            throw IllegalStateException("Node is already starting or started. Current state: ${cluster.state()}")
        }

        try {
            this.initializeDatabase()
            this.initializeCluster()
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
        if (cluster.state() == NodeState.OFFLINE) {
            // Node was never started, nothing to do
            return
        }

        if (cluster.state() == NodeState.STOPPING || cluster.state() == NodeState.STOPPED) {
            return
        }

        cluster.markStopping()

        // finally close database and other resources
        cluster.close(mode)

        if (!shutdownHandler.running) {
            exitProcess(0)
        }
    }

    /**
     * Performs cluster discovery and marks the node as online.
     */
    private fun initializeCluster() {
        cluster.detect()
        cluster.markOnline()

        logger.trInfo("cluster", "cluster.node.started", "version" to PolocloudVersion.CURRENT.toDisplayString())
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
            NodeInstanceConfiguration(
                database = DatabaseCredentials.H2(
                    launchConfig.localDataPath.toString() + "/polocloud.h2.db"
                )
            )
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

        if (launchAddress != null) {

            val hostname = launchAddress.hostname.takeIf { it.isNotBlank() } ?: defaultAddress.hostname
            val port = launchAddress.port.takeIf { it > 0 } ?: defaultAddress.port

            require(port in 1..65535) { "Port must be between 1 and 65535 but was $port" }

            return Address(hostname, port)
        }
        return config.bindAddress
    }

    private fun initializeDatabase() {
        database.connect()

        if (!database.isValid()) {
            logger.error(
                TranslationService.tr(
                    "cluster",
                    "cluster.node.database.failed"
                )
            )
            throw IllegalStateException("Cluster database connection is not valid.")
        }
    }

    fun resolveDatabaseCredentials(): DatabaseCredentials {
        if (launchConfig.database != null) {
            return launchConfig.database
        }
        return config.database
    }
}