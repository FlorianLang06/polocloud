package dev.httpmarco.polocloud.node.cluster.quorum

import dev.httpmarco.polocloud.node.cluster.client.ClusterNodeApprovalClient
import dev.httpmarco.polocloud.node.cluster.node.NodeState
import dev.httpmarco.polocloud.node.cluster.node.data.NodeData
import dev.httpmarco.polocloud.node.cluster.security.ClusterSecurity

class QuorumService(
    private val security: ClusterSecurity,
) {

    fun requestJoin(
        nodes: List<NodeData>,
        newNode: NodeData,
        approvalClient: ClusterNodeApprovalClient
    ): List<String> {

        val quorumSize = (nodes.size / 2) + 1
        val approvals = mutableListOf<String>()

        for (node in nodes.filter { it.state == NodeState.ONLINE }) {
            val approval = approvalClient.requestApproval(node, newNode)
            approvals += approval
            if (approvals.size >= quorumSize) break
        }

        if (approvals.size < quorumSize) {
            throw IllegalStateException("Quorum not reached")
        }

        return approvals
    }
}