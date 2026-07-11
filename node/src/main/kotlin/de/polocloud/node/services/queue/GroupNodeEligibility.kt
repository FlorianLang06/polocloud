package de.polocloud.node.services.queue

import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.group.Group
import de.polocloud.proto.NodeState

/**
 * Resolves which online nodes a [Group] is currently allowed to start services on.
 *
 * A group with an empty [Group.nodes] whitelist is unrestricted — every online node is
 * eligible. Otherwise only the online nodes whose [NodeData.name] appears in the
 * whitelist are eligible. Used by [ServiceQueue] to decide both whether the local node
 * may act on a group at all, and — together with the cluster-wide running count — how
 * many of the group's `minOnline` services this node itself is responsible for.
 */
object GroupNodeEligibility {

    fun eligibleOnlineNodes(
        group: Group,
        onlineNodes: List<NodeData> = NodeRepository.find(NodeState.ONLINE),
    ): List<NodeData> {
        if (group.nodes.isEmpty()) return onlineNodes
        return onlineNodes.filter { it.name() in group.nodes }
    }
}
