package de.polocloud.node

import de.polocloud.common.Address
import de.polocloud.common.ShutdownMode
import de.polocloud.common.utils.publicIpAddress
import de.polocloud.database.DatabaseConnectionFactory
import de.polocloud.database.DatabaseKey
import de.polocloud.node.exception.InvalidClusterRegistrationStateException
import de.polocloud.node.join.ClusterNodeApprovalClient
import de.polocloud.node.join.QuorumService
import de.polocloud.node.launch.NodeLaunchConfig
import de.polocloud.node.node.NodeFactory
import de.polocloud.node.node.data.NodeData
import de.polocloud.node.registration.RegistrationManager

class ClusterBootstrapService(
    private val database: DatabaseConnectionFactory<*>,
    private val security: de.polocloud.node.security.ClusterSecurity,
    private val bindAddress: Address,
    private val quorumService: QuorumService,
    private val launchConfig: NodeLaunchConfig
) {

    private val clusterKey = DatabaseKey(NodeData::class)

    fun detectAndRegister() {
        require(database.isValid()) { "Database invalid" }

        val nodes = database.executor().findAll(clusterKey)
        val ip = publicIpAddress() ?: throw IllegalStateException("No public IP")

        if (nodes.isEmpty()) {
            this.createInitialNode(ip)
            return
        }

        if (nodes.none { it.id == security.localId }) {
            if (launchConfig.clusterRegistrationToken == null) {
                throw InvalidClusterRegistrationStateException()
            }
            RegistrationManager.registerNode(launchConfig.clusterRegistrationToken, security)
        }

        // join existing cluster, validate identity consistency
        this.joinCluster()
    }

    private fun createInitialNode(ip: String) {
        val node = NodeFactory.createInitial(security, ip, bindAddress.port)
        database.executor().save(clusterKey, node)
    }


    fun joinCluster() {

    }


    fun close(mode: ShutdownMode) {
        database.close(mode)
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

}