package dev.httpmarco.polocloud.node.cluster

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.common.Closeable
import dev.httpmarco.polocloud.common.ShutdownMode
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.node.launch.NodeLaunchConfig
import dev.httpmarco.polocloud.node.cluster.node.NodeHeartBeatService
import dev.httpmarco.polocloud.node.cluster.node.NodeStateService
import dev.httpmarco.polocloud.node.cluster.quorum.QuorumService
import dev.httpmarco.polocloud.node.cluster.security.ClusterSecurity
import dev.httpmarco.polocloud.node.configuration.NodeInstanceConfiguration
import org.slf4j.LoggerFactory

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
class Cluster(bindAddress: Address, val config: NodeInstanceConfiguration, val launchConfig: NodeLaunchConfig) : Closeable {

    private val logger = LoggerFactory.getLogger(Cluster::class.java)
    private val database =  resolveDatabaseCredentials().factory()
    private val security = ClusterSecurity(launchConfig.localSecurityPath)
    private val quorumService = QuorumService(security)
    private val bootstrapService = ClusterBootstrapService(database, security, bindAddress, quorumService)
    private val stateService = NodeStateService(database, security)
    private val heartBeatService = NodeHeartBeatService(security.localId, database)

    init {
        initializeDatabase()
    }

    fun detect() {
        bootstrapService.detectAndRegister()
        heartBeatService.startScheduler()
    }

    fun markOnline() = stateService.markOnline()
    fun markStopping() = stateService.markStopping()
    fun markStopped() = stateService.markStopped()
    fun state() = stateService.localState()

    override fun close(mode: ShutdownMode) {
        heartBeatService.stopScheduler()
        bootstrapService.close(mode)
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