package dev.httpmarco.polocloud.node.cluster

import dev.httpmarco.polocloud.common.utils.publicIpAddress
import dev.httpmarco.polocloud.common.utils.toBytes
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.node.NodeInstance
import dev.httpmarco.polocloud.node.cluster.exception.LocalNodeFindingException
import dev.httpmarco.polocloud.node.cluster.node.NodeData
import dev.httpmarco.polocloud.node.cluster.node.NodeState
import dev.httpmarco.polocloud.node.cluster.security.ClusterSecurity
import dev.httpmarco.polocloud.node.cluster.security.toBase64
import org.slf4j.Logger
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
object Cluster {

    private val logger: Logger = LoggerFactory.getLogger(Cluster::class.java)
    private val clusterDatabaseKey = DatabaseKey("nodes", NodeData::class)
    private val security = ClusterSecurity()
    private val database = NodeInstance.config.database.factory()

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
    }



    /**
     * Registers this node as the first node in a new cluster.
     */
    private fun createInitialNode(ip: String) {
        val nodeData = NodeData(security.localId, "node-1", ip, 25565, NodeState.STARTING, true, security.publicKey.toBase64())

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

    fun markOnline() {
        val localNode = database.executor().findById(clusterDatabaseKey, security.localId) ?: throw LocalNodeFindingException()

        if (localNode.state == NodeState.CRASHED || localNode.state == NodeState.OFFLINE || localNode.state == NodeState.STOPPING) {
            logger.warn(
                TranslationService.tr(
                    "cluster",
                    "cluster.node.mark.online.failed",
                    "currentState" to localNode.state.name
                )
            )
            return
        }
        localNode.state = NodeState.ONLINE
        database.executor().save(clusterDatabaseKey, localNode)
        // todo alert to other nodes that this node is now online

        logger.info(
            TranslationService.tr(
                "cluster",
                "cluster.node.mark.online.success"
            )
        )
    }

    private fun registerNewNodeWithQuorum(nodes: List<NodeData>, ip: String) {
        val quorumSize = (nodes.size / 2) + 1
        val newNodeData = NodeData(
            id = security.localId,
            name = "node-${nodes.size + 1}",
            hostname = ip,
            port = 25565,
            state = NodeState.STARTING,
            publicKey = security.publicKey.toBase64()
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
}
