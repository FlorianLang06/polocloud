package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.common.ShutdownMode
import dev.httpmarco.polocloud.common.utils.publicIpAddress
import dev.httpmarco.polocloud.database.DatabaseConnectionFactory
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.node.join.ClusterNodeApprovalClient
import dev.httpmarco.polocloud.node.join.QuorumService

class ClusterBootstrapService(
    private val database: DatabaseConnectionFactory<*>,
    private val security: dev.httpmarco.polocloud.node.security.ClusterSecurity,
    private val bindAddress: Address,
    private val quorumService: QuorumService
) {

    private val clusterKey = DatabaseKey(_root_ide_package_.dev.httpmarco.polocloud.node.node.data.NodeData::class)

    fun detectAndRegister() {
        database.connect()
        require(database.isValid()) { "Database invalid" }

        val nodes = database.executor().findAll(clusterKey)
        publicIpAddress() ?: throw IllegalStateException("No public IP")

        when {
            nodes.isEmpty() -> createInitialNode("127.0.0.1")
            nodes.any { it.id == security.localId } -> validateExistingNode(nodes)
            else -> registerNewNode(nodes, "127.0.0.1")
        }
    }

    private fun createInitialNode(ip: String) {
        val node = _root_ide_package_.dev.httpmarco.polocloud.node.node.NodeFactory.createInitial(security, ip, bindAddress.port)
        database.executor().save(clusterKey, node)
    }

    private fun validateExistingNode(nodes: List<dev.httpmarco.polocloud.node.node.data.NodeData>) {
        val current = nodes.first { it.id == security.localId }
        val ip = publicIpAddress()

        if (ip != null && current.hostname != ip) {
            throw IllegalStateException("IP mismatch detected.")
        }
    }

    private fun registerNewNode(nodes: List<dev.httpmarco.polocloud.node.node.data.NodeData>, ip: String) {
        val newNode = _root_ide_package_.dev.httpmarco.polocloud.node.node.NodeFactory.create(security, nodes.size + 1, "127.0.0.1", bindAddress.port)

        val approvals = quorumService.requestJoin(nodes, newNode,
            ClusterNodeApprovalClient(security)
        )

        security.quorumSignatures.addAll(approvals)
        database.executor().save(clusterKey, newNode)
    }

    fun close(mode: ShutdownMode) {
        database.close(mode)
    }
}