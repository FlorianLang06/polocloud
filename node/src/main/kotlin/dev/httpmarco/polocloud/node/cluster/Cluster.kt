package dev.httpmarco.polocloud.node.cluster

import dev.httpmarco.polocloud.common.Closeable
import dev.httpmarco.polocloud.common.ShutdownMode
import dev.httpmarco.polocloud.common.utils.publicIpAddress
import dev.httpmarco.polocloud.common.utils.toBytes
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.node.launch.NodeLaunchConfig
import dev.httpmarco.polocloud.node.cluster.exception.LocalNodeFindingException
import dev.httpmarco.polocloud.node.cluster.node.NodeHeartBeatService
import dev.httpmarco.polocloud.node.cluster.node.data.NodeData
import dev.httpmarco.polocloud.node.cluster.node.NodeState
import dev.httpmarco.polocloud.node.cluster.security.ClusterSecurity
import dev.httpmarco.polocloud.node.cluster.security.toBase64
import dev.httpmarco.polocloud.node.configuration.NodeInstanceConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

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
class Cluster(config: NodeInstanceConfiguration, launchConfig: NodeLaunchConfig) : Closeable {

    private val logger: Logger = LoggerFactory.getLogger(Cluster::class.java)
    private val clusterDatabaseKey = DatabaseKey(NodeData::class)
    private val database = config.database.factory()
    private val security = ClusterSecurity(launchConfig.localSecurityPath)
    private val heartBeatService = NodeHeartBeatService(security.localId.toString(), factory = database)

    // the local node state - this is the source of truth for the node's current state and is updated during lifecycle transitions
    @OptIn(ExperimentalAtomicApi::class)
    var localNodeState = AtomicReference(NodeState.OFFLINE)

    init {
        initializeDatabase()
        logIdentity()
    }

    /**
     * Establishes and validates the database connection.
     *
     * @throws IllegalStateException if the database connection is not valid.
     */
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

    /**
     * Logs the detected local node identity.
     */
    private fun logIdentity() {
        logger.info(
            TranslationService.tr(
                "cluster",
                "cluster.node.identity.detected",
                "nodeId" to security.localId
            )
        )
    }

    /**
     * Performs cluster detection and registration logic.
     *
     * Behavior:
     * - If no nodes exist → this node becomes the first cluster node.
     * - If this node exists → validates consistency.
     * - If this node does not exist → registers as new cluster member.
     *
     * @throws IllegalStateException if critical cluster inconsistencies occur.
     */
    fun detect() {
        val executor = database.executor()
        val nodes = executor.findAll(clusterDatabaseKey)

        val publicIp = publicIpAddress()
            ?: throw IllegalStateException("Unable to determine public IP address.")

        when {
            nodes.isEmpty() -> createInitialNode(publicIp)
            nodes.any { it.id == security.localId } -> validateExistingNode(nodes)
            //    else -> registerNewNode(executor, nodes, publicIp)
        }

        heartBeatService.startScheduler()
    }


    /**
     * Registers this node as the first node in a new cluster.
     */
    private fun createInitialNode(ip: String) {
        // todo add version and git commit hash
        val nodeData = NodeData(
            security.localId,
            "node-1",
            ip,
            25565,
            NodeState.STARTING,
            true,
            security.publicKey.toBase64(),
            "1.0.0",
            "01293012ke,0"
        )

        database.executor().save(clusterDatabaseKey, nodeData)

        logger.info(
            TranslationService.tr(
                "cluster",
                "cluster.node.identity.created"
            )
        )

        logger.info(
            TranslationService.tr(
                "cluster",
                "cluster.node.identity.alert.token",
                "clusterToken" to security.publicKey.toBase64()
            )
        )
    }

    /**
     * Validates consistency of an already registered node.
     */
    private fun validateExistingNode(nodes: List<NodeData>) {
        val currentNode = nodes.first { it.id == security.localId }

        // Optional: Validate IP consistency to prevent split-brain
        val currentIp = publicIpAddress()
        if (currentIp != null && currentNode.hostname != currentIp) {
            throw IllegalStateException(
                "Node IP mismatch detected. Possible split-brain condition."
            )
        }
        logger.info(
            TranslationService.tr(
                "cluster",
                "cluster.node.validate.local.success"
            )
        )
    }

    fun markStopping() {
        this.changeState(NodeState.STOPPING, {
            return@changeState it.state == NodeState.ONLINE || it.state == NodeState.CRASHED
        })
    }

    fun markOnline() {
        this.changeState(NodeState.ONLINE) {
            return@changeState it.state == NodeState.STARTING || it.state == NodeState.SYNCING
        }
    }

    fun markStopped() {
        this.changeState(NodeState.STOPPED) {
            return@changeState it.state == NodeState.STOPPING || it.state == NodeState.CRASHED
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun changeState(state: NodeState, predicate: (NodeData) -> Boolean) {
        val localNode = findSelf()

        if (!predicate.invoke(localNode)) {
            logger.warn(
                TranslationService.tr(
                    "cluster",
                    "cluster.node.mark.failed",
                    "currentState" to localNode.state.name,
                    "state" to state.name

                )
            )
        }

        localNodeState.store(state)
        localNode.state = state
        database.executor().save(clusterDatabaseKey, localNode)
        // todo alert to other nodes that this node is now online

        logger.info(
            TranslationService.tr(
                "cluster",
                "cluster.node.mark.success",
                "state" to state.name
            )
        )
    }

    fun findSelf(): NodeData {
        return database.executor().findById(clusterDatabaseKey, security.localId) ?: throw LocalNodeFindingException()
    }

    private fun registerNewNodeWithQuorum(nodes: List<NodeData>, ip: String) {
        val quorumSize = (nodes.size / 2) + 1
        val newNodeData = NodeData(
            id = security.localId,
            name = "node-${nodes.size + 1}",
            hostname = ip,
            port = 25565,
            state = NodeState.STARTING,
            publicKey = security.publicKey.toBase64(),
            head = false,
            version = "1.0.0", //todo
            gitCommitHash = "01293012ke,0" // todo
        )

        val approvals = mutableListOf<String>()
        for (existingNode in nodes.filter { it.state == NodeState.ONLINE }) {
            val approval = requestApprovalFromNode(existingNode, newNodeData)
            if (approval != null) approvals.add(approval)
            if (approvals.size >= quorumSize) break
        }

        if (approvals.size < quorumSize) {
            throw IllegalStateException("Quorum not reached. Node cannot join cluster.")
        }

        security.quorumSignatures.addAll(approvals)
        database.executor().save(clusterDatabaseKey, newNodeData)

        logger.info("Node ${security.localId} joined cluster with quorum (${approvals.size}/${nodes.size})")
    }

    private fun requestApprovalFromNode(existingNode: NodeData, newNode: NodeData): String? {
        // TODO: echte gRPC Kommunikation einbauen
        // z.B. Node überprüft PublicKey und signiert den Join-Request
        val approvalSignature = security.sign(newNode.id.toBytes()) // signed NodeID with Key
        return approvalSignature
    }

    override fun close(mode: ShutdownMode) {
        this.heartBeatService.stopScheduler()
        this.database.close(mode)
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun state() : NodeState {
        return localNodeState.load()
    }
}
