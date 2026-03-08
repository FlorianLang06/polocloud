package de.polocloud.node

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.grpc.GrpcEndpoint
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.node.launch.NodeLaunchConfig
import de.polocloud.node.grpc.NodeServiceImpl
import de.polocloud.node.join.QuorumService
import de.polocloud.node.node.NodeHeartBeatService
import de.polocloud.node.node.NodeStateService
import de.polocloud.node.registration.RegistrationService
import de.polocloud.node.registration.RegistrationTokenStore
import de.polocloud.node.repository.NodeRepository
import de.polocloud.node.security.ClusterSecurity

/**
 * Central cluster lifecycle manager.
 *
 * Responsible for:
 * - Establishing database connectivity
 * - Detecting whether this node is the first cluster node
 * - Registering new nodes in the cluster
 * - Validating cluster identity consistency
 *
 * This class ensures that:
 * - The cluster database is reachable before startup
 * - A node registers itself if it does not yet exist
 * - The cluster token is displayed for initial cluster creation
 *
 * Critical startup failures result in an IllegalStateException.
 */
class Cluster(database: DatabaseConnectionFactory<*>, bindAddress: Address, val launchConfig: NodeLaunchConfig) :
    Closeable {

    private val tokenStore = RegistrationTokenStore()
    private val nodeRepository = NodeRepository(database)
    private val security = ClusterSecurity(launchConfig.localSecurityPath)
    private val endpoint = GrpcEndpoint(
        bindAddress, security.certFile(), security.keyFile(),
        NodeServiceImpl(nodeRepository), RegistrationService(tokenStore, nodeRepository)
    )
    private val quorumService = QuorumService()
    private val bootstrapService = ClusterBootstrapService(
        database,
        security,
        bindAddress,
        quorumService,
        launchConfig
    )
    private val stateService = NodeStateService(nodeRepository, security)
    private val heartBeatService =
        NodeHeartBeatService(security.localId, database)

    fun detect() {
        this.endpoint.connect()
        this.bootstrapService.detectAndRegister()
        this.heartBeatService.startScheduler()
    }

    fun markOnline() = stateService.markOnline()
    fun markStopping() = stateService.markStopping()
    fun markStopped() = stateService.markStopped()
    fun state() = stateService.localState()
    fun token() = tokenStore.currentToken()

    override fun close(mode: ShutdownMode) {
        endpoint.close(mode);
        heartBeatService.stopScheduler()
        bootstrapService.close(mode)
    }
}