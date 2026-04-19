package de.polocloud.node.cluster.election.strategy

import de.polocloud.node.cluster.node.NodeData
import de.polocloud.proto.NodeState

object OldestNodeStrategy : ElectionStrategy {

    override fun elect(candidates: List<NodeData>): NodeData? {
        return candidates
            .filter { it.state == NodeState.ONLINE && !it.head }
            .minByOrNull { it.lastConnection }
    }
}