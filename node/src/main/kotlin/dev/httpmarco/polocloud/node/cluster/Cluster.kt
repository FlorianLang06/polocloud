package dev.httpmarco.polocloud.node.cluster

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.common.Closeable
import dev.httpmarco.polocloud.common.ShutdownMode
import dev.httpmarco.polocloud.common.grpc.GrpcEndpoint
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.node.launch.NodeLaunchConfig
import dev.httpmarco.polocloud.node.cluster.node.NodeHeartBeatService
import dev.httpmarco.polocloud.node.cluster.node.NodeStateService
import dev.httpmarco.polocloud.node.cluster.quorum.QuorumService
import dev.httpmarco.polocloud.node.cluster.repository.NodeRepository
import dev.httpmarco.polocloud.node.cluster.security.ClusterSecurity
import dev.httpmarco.polocloud.node.grpc.NodeServiceImpl

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
class Cluster(database: DatabaseConnectionFactory<*>, bindAddress: Address, launchConfig: NodeLaunchConfig) :
    Closeable {

    private val nodeRepository = NodeRepository(database)
    private val security = ClusterSecurity(launchConfig.localSecurityPath)
    private val endpoint = GrpcEndpoint(bindAddress, security.certFile(), security.keyFile(), NodeServiceImpl(nodeRepository))
    private val quorumService = QuorumService()
    private val bootstrapService = ClusterBootstrapService(database, security, bindAddress, quorumService)
    private val stateService = NodeStateService(nodeRepository, security)
    private val heartBeatService = NodeHeartBeatService(security.localId, database)

    fun detect() {
        this.endpoint.connect()
        bootstrapService.detectAndRegister()
        heartBeatService.startScheduler()
    }

    fun markOnline() = stateService.markOnline()
    fun markStopping() = stateService.markStopping()
    fun markStopped() = stateService.markStopped()
    fun state() = stateService.localState()

    override fun close(mode: ShutdownMode) {
        endpoint.close(mode);
        heartBeatService.stopScheduler()
        bootstrapService.close(mode)
    }
}