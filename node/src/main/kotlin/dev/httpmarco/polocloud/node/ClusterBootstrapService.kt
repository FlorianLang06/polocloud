package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.common.ShutdownMode
import dev.httpmarco.polocloud.common.utils.publicIpAddress
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.node.join.ClusterNodeApprovalClient
import dev.httpmarco.polocloud.node.join.QuorumService
import dev.httpmarco.polocloud.node.launch.NodeLaunchConfig
import dev.httpmarco.polocloud.node.node.NodeFactory
import dev.httpmarco.polocloud.node.node.data.NodeData

class ClusterBootstrapService(
    private val database: DatabaseConnectionFactory<*>,
    private val security: dev.httpmarco.polocloud.node.security.ClusterSecurity,
    private val bindAddress: Address,
    private val quorumService: QuorumService,
    private val launchConfig: NodeLaunchConfig
) {

    private val clusterKey = DatabaseKey(NodeData::class)

    fun detectAndRegister() {
        database.connect()
        require(database.isValid()) { "Database invalid" }

        val nodes = database.executor().findAll(clusterKey)
        val ip = publicIpAddress() ?: throw IllegalStateException("No public IP")

        if(nodes.isNotEmpty()) {
            // current cluster is already here -> join and share online state
            return
        }

        if(launchConfig.clusterRegistrationToken != null) {
            // we register to an existing cluster, so we need to join it first
            return
        }

        // stand alone here
        this.createInitialNode(ip)
    }

    private fun createInitialNode(ip: String) {
        val node = NodeFactory.createInitial(security, ip, bindAddress.port)
        database.executor().save(clusterKey, node)
    }

    private fun validateExistingNode(nodes: List<NodeData>) {
        val current = nodes.first { it.id == security.localId }
        val ip = publicIpAddress()

        if (ip != null && current.hostname != ip) {
            throw IllegalStateException("IP mismatch detected.")
        }
    }

    private fun registerNewNode(nodes: List<NodeData>, ip: String) {
        val newNode = NodeFactory.create(security, nodes.size + 1, "127.0.0.1", bindAddress.port)

        val approvals = quorumService.requestJoin(
            nodes, newNode,
            ClusterNodeApprovalClient(security)
        )

        security.quorumSignatures.addAll(approvals)
        database.executor().save(clusterKey, newNode)
    }

    fun close(mode: ShutdownMode) {
        database.close(mode)
    }
}