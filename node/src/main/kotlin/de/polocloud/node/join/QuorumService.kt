package de.polocloud.node.join

import de.polocloud.node.node.NodeState
import de.polocloud.node.node.data.NodeData

class QuorumService {

    fun requestJoin(
        nodes: List<de.polocloud.node.node.data.NodeData>,
        newNode: de.polocloud.node.node.data.NodeData,
        approvalClient: de.polocloud.node.join.ClusterNodeApprovalClient
    ): List<String> {

        val quorumSize = (nodes.size / 2) + 1
        val approvals = mutableListOf<String>()

        for (node in nodes.filter { it.state == _root_ide_package_.de.polocloud.node.node.NodeState.ONLINE }) {
            val approval = approvalClient.requestApproval(node, newNode)

           // if (approval.approved) {
             //   approvals += approval.signature
            //}

            if (approvals.size >= quorumSize) break
        }

        if (approvals.size < quorumSize) {
            throw IllegalStateException("Quorum not reached")
        }

        return approvals
    }
}