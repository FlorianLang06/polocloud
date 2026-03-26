package de.polocloud.node

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.i18n.trInfo
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.configuration.NodeConfigurations
import de.polocloud.node.generator.LocalIdGenerator
import de.polocloud.node.launch.NodeLaunchProperties
import de.polocloud.node.nodes.LocalNodeContainer
import de.polocloud.node.nodes.NodeFactory
import de.polocloud.node.registration.RegistrationManager
import de.polocloud.node.repositories.NodeRepository
import de.polocloud.node.security.CertificateDataStorage
import de.polocloud.node.shutdown.ShutdownHook
import org.slf4j.LoggerFactory

class NodeInstance(
    /**
     * Bootstrap information created during application startup.
     * Contains resolved filesystem paths and runtime launch parameters.
     */
    val launchProperties: NodeLaunchProperties,
    val configurations: NodeConfigurations,
) : Closeable {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Database connection factory resolved from launch parameters or persisted configuration.
     */
    val database: DatabaseConnectionFactory<*>

    lateinit var localNodeContainer: LocalNodeContainer

    val certificateDataStorage = CertificateDataStorage()
    val nodeRepository: NodeRepository
    val registrationManager : RegistrationManager

    init {
        TranslationService.init()
        TranslationService.defaultLanguage(configurations.generalConfig.locale)

        this.database = this.initializeDatabase()
        this.nodeRepository = NodeRepository(this.database)
        this.registrationManager = RegistrationManager(configurations.clusterConfig, nodeRepository, certificateDataStorage.keyPair)

        this.initialize()
    }

    fun initialize() {
        val localId = LocalIdGenerator.generate()

        if (nodeRepository.count() == 0L) {
            // we are the only and new head
            this.localNodeContainer = LocalNodeContainer(nodeRepository, NodeFactory.createInitial(resolveBindAddress()))
            this.nodeRepository.save(this.localNodeContainer.data)
            return
        }

        val possibleNode = nodeRepository.find(localId)
        if (possibleNode != null) {
            this.localNodeContainer = LocalNodeContainer(nodeRepository, possibleNode)
            return
        }

        // only cluster join chance
        if (launchProperties.clusterRegistration == null) {
            this.logger.trInfo("cluster", "cluster.validation.failed")
            this.close(ShutdownMode.GRACEFUL)
            throw IllegalStateException("Node is not registered in the cluster and no registration token was provided. Cannot start.")
        }

        registrationManager.tryJoinCluster(launchProperties.clusterRegistration, localId, certificateDataStorage)
    }

    @Synchronized
    fun start() {
        if (!localNodeContainer.isStarting()) {
            // Node is already started or in the process of starting, ignore subsequent start calls
            throw IllegalStateException("Node is already starting or started. Current state: ${localNodeContainer.state()}")
        }
        this.localNodeContainer.markStarting()

        // allow other nodes to connect
        registrationManager.allowRequests()

        this.localNodeContainer.markOnline()

        logger.trInfo("cluster", "cluster.node.started", "version" to PolocloudVersion.CURRENT.toDisplayString())
    }

    @Synchronized
    override fun close(mode: ShutdownMode) {
        if (localNodeContainer.isOffline() || localNodeContainer.inShutdownProcess()) {
            // Node was never started, nothing to do
            // or already stopped
            return
        }

        this.localNodeContainer.markStopping()
        ShutdownHook.shutdown(mode)
        this.localNodeContainer.markStopped()
        this.database.close(mode)
    }

    private fun initializeDatabase(): DatabaseConnectionFactory<*> {
        val database = configurations.nodeConfig.database.factory()
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
        return database
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
        val launchAddress = launchProperties.address
        val defaultAddress = configurations.generalConfig.bindAddress

        if (launchAddress != null) {

            val hostname = launchAddress.hostname.takeIf { it.isNotBlank() } ?: defaultAddress.hostname
            val port = launchAddress.port.takeIf { it > 0 } ?: defaultAddress.port

            require(port in 1..65535) { "Port must be between 1 and 65535 but was $port" }

            return Address(hostname, port)
        }
        return defaultAddress
    }
}