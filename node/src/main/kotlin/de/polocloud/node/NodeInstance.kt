package de.polocloud.node

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.error.exception.PoloException
import de.polocloud.common.i18n.trError
import de.polocloud.common.i18n.trInfo
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.cli.registration.CliRegistrationService
import de.polocloud.node.cli.session.CliSessionManager
import de.polocloud.node.cli.session.ICliSessionManager
import de.polocloud.node.configuration.NodeConfigurations
import de.polocloud.node.error.NodeError
import de.polocloud.node.generator.LocalIdGenerator
import de.polocloud.node.internal.NodeGrpcClient
import de.polocloud.node.internal.NodeGrpcEndpoint
import de.polocloud.node.launch.NodeLaunchProperties
import de.polocloud.node.nodes.LocalNodeContainer
import de.polocloud.node.nodes.NodeFactory
import de.polocloud.node.registration.RegistrationManager
import de.polocloud.node.repositories.NodeRepository
import de.polocloud.node.security.CertificateDataStorage
import de.polocloud.node.services.ServiceHandler
import org.apache.logging.log4j.LogManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId

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
    val registrationManager: RegistrationManager

    val cliRegistrationService: CliRegistrationService
    val cliSessionManager: ICliSessionManager

    val nodeGrpcEndpoint: NodeGrpcEndpoint
    lateinit var headNodeConnection: NodeGrpcClient

    init {
        TranslationService.init()
        TranslationService.defaultLanguage(configurations.generalConfig.locale)

        Runtime.getRuntime().addShutdownHook(Thread { close(ShutdownMode.GRACEFUL) })

        this.database = this.initializeDatabase()
        this.nodeRepository = NodeRepository(this.database)
        this.cliSessionManager = CliSessionManager()
        this.cliRegistrationService = CliRegistrationService(
            configurations.clusterConfig,
            certificateDataStorage,
            cliSessionManager
        )
        this.registrationManager = RegistrationManager(
            configurations.clusterConfig,
            nodeRepository,
            certificateDataStorage,
            cliRegistrationService
        )
        this.nodeGrpcEndpoint = NodeGrpcEndpoint(
            resolveBindAddress(),
            certificateDataStorage,
            configurations.clusterConfig,
            cliRegistrationService,
            cliSessionManager
        ) { localNodeContainer }

        this.initialize()
    }

    fun initialize() {
        val localId = LocalIdGenerator.generate()

        if (nodeRepository.count() == 0L) {
            // we are the only and new head
            logger.trInfo("cluster", "cluster.node.identity.created")

            this.localNodeContainer = LocalNodeContainer(
                nodeRepository,
                NodeFactory.createInitial(
                    localId,
                    resolveBindAddress(),
                    launchProperties.group
                )
            )
            this.nodeRepository.save(this.localNodeContainer.data)

            val clusterToken = this.registrationManager.registrationTokenManger.createInitialCliToken()
            val expire = Instant.ofEpochMilli(clusterToken.expiresAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()

            logger.trInfo("cluster", "cluster.node.identity.alert.token", "clusterToken" to clusterToken.token, "expire" to expire)

            this.nodeGrpcEndpoint.start()
            return
        }

        val possibleNode = nodeRepository.find(localId)
        if (possibleNode != null) {
            logger.trInfo("cluster", "cluster.node.identity.detected")

            this.nodeGrpcEndpoint.start()
            this.localNodeContainer = LocalNodeContainer(nodeRepository, possibleNode)
            this.localNodeContainer.markStarting()
            return
        }

        // only cluster join chance
        if (launchProperties.clusterRegistration == null) {
            this.logger.trInfo("cluster", "cluster.validation.failed")
            this.close(ShutdownMode.GRACEFUL)
            throw PoloException(NodeError.NotRegisteredInCluster(localId.toString()))
        }

        registrationManager.tryJoinCluster(launchProperties.clusterRegistration, localId, certificateDataStorage)

        this.nodeGrpcEndpoint.start()

        headNodeConnection = NodeGrpcClient(certificateDataStorage)
        headNodeConnection.connect(launchProperties.clusterRegistration.address)

        val nodeData = nodeRepository.find(localId)
        this.localNodeContainer = LocalNodeContainer(nodeRepository, nodeData!!)
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

        // scan local services
        ServiceHandler.initialize()

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

        logger.trInfo("node", "node.shutdown.stopping")

        this.localNodeContainer.markStopping()

        safeClose(logger, "registrationManager") {
            this.registrationManager.close(mode)
        }

        safeClose(logger, "localNodeContainer") {
            this.localNodeContainer.markStopped()
        }

        safeClose(logger, "database") {
            this.database.close(mode)
        }

        logger.trInfo("node", "node.shutdown.stopped")
        LogManager.shutdown()
    }

    private fun initializeDatabase(): DatabaseConnectionFactory<*> {
        val database = configurations.nodeConfig.database.factory()
        database.connect()

        if (!database.isValid()) {
            this.close(ShutdownMode.GRACEFUL)
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

    inline fun safeClose(
        logger: Logger,
        name: String,
        block: () -> Unit
    ) {
        try {
            block()
        } catch (_: Exception) {
            logger.trError(
                "node",
                "node.shutdown.task.error",
                "task" to name
            )
        }
    }
}
