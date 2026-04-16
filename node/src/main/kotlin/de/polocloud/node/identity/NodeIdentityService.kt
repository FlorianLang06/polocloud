package de.polocloud.node.identity

import de.polocloud.common.Address
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.i18n.api.trInfo
import de.polocloud.node.bootstrap.properties.NodeProperties
import de.polocloud.node.cluster.node.LocalNodeContainer
import de.polocloud.node.cluster.node.NodeFactory
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.cli.session.ICliSessionManager
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.node.communication.grpc.NodeGrpcEndpoint
import de.polocloud.node.communication.registration.cli.CliRegistrationService
import de.polocloud.node.communication.registration.node.RegistrationManager
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.core.context.NodeRuntimeContext
import de.polocloud.node.identity.provider.NodeIdProvider
import de.polocloud.node.security.CertificateDataStorage
import de.polocloud.node.services.ServiceHandler
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId

class NodeIdentityService(
    private val nodeId: NodeIdProvider,
    private val holder: ConfigurationHolder<NodeConfigurations>,
    private val registrationManager: RegistrationManager,
    private val cliRegistrationService: CliRegistrationService,
    private val cliSessionManager: ICliSessionManager,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    lateinit var container: LocalNodeContainer

    fun resolve(launchProperties: NodeProperties): NodeRuntimeContext {
        val localId = nodeId.get()

        CertificateDataStorage.nodeId = localId.toString()
        CertificateDataStorage.initialize()

        val bindAddress = resolveBindAddress(launchProperties)

        val grpc = NodeGrpcEndpoint(
            bindAddress,
            cliRegistrationService,
            cliSessionManager
        )

        val serviceHandler = ServiceHandler()

        if (NodeRepository.count() == 0L) {
            logger.trInfo("cluster", "cluster.node.identity.created")

            container = LocalNodeContainer(NodeFactory.createInitial(bindAddress, launchProperties.group))
            NodeRepository.save(container.data)

            val clusterToken = registrationManager.registrationTokenManger.createInitialCliToken()
            val expire = Instant.ofEpochMilli(clusterToken.expiresAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()

            logger.trInfo(
                "cluster",
                "cluster.node.identity.alert.token",
                "clusterToken" to clusterToken.token,
                "expire" to expire
            )

            grpc.start()

            return NodeRuntimeContext(holder,container, registrationManager, serviceHandler, grpc, null)
        }

        val possibleNode = NodeRepository.find(localId)
        if (possibleNode != null) {
            logger.trInfo("cluster", "cluster.node.identity.detected", "nodeId" to localId.toString())

            container = LocalNodeContainer(possibleNode)
            container.markStarting()

            grpc.start()

            return NodeRuntimeContext(holder, container, registrationManager, serviceHandler, grpc, null)
        }

        if (launchProperties.clusterRegistration == null) {
            logger.trInfo("cluster", "cluster.validation.failed")
            throw IllegalStateException("This node '$localId' is not registered in the cluster and no registration token was provided.")
        }


        registrationManager.tryJoinCluster(launchProperties.clusterRegistration, localId, launchProperties.group)

        grpc.start()

        val headConnection = NodeGrpcClient()
        headConnection.connect(launchProperties.clusterRegistration.address)

        val nodeData = NodeRepository.find(localId)
        container = LocalNodeContainer(nodeData!!)

        return NodeRuntimeContext(holder, container, registrationManager, serviceHandler, grpc, headConnection)
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
    private fun resolveBindAddress(launchProperties: NodeProperties): Address {
        val launchAddress = launchProperties.address
        val defaultAddress = Address(holder.value.general.bindAddress.hostname, holder.value.general.bindAddress.port)

        if (launchAddress != null) {
            val hostname = launchAddress.hostname.takeIf { it.isNotBlank() } ?: defaultAddress.hostname
            val port = launchAddress.port.takeIf { it > 0 } ?: defaultAddress.port

            require(port in 1..65535) { "Port must be between 1 and 65535 but was $port" }

            return Address(hostname, port)
        }
        return defaultAddress
    }
}