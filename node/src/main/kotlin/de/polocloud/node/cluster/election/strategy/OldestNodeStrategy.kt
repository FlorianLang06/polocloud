package de.polocloud.node.cluster.election.strategy

import de.polocloud.node.cluster.node.NodeData
import de.polocloud.proto.NodeState

object OldestNodeStrategy : ElectionStrategy {

    /**
     * Picks the longest-tenured online candidate, by [NodeData.firstConnection] (when it
     * first joined the cluster) — not [NodeData.lastConnection], which is bumped on every
     * request a node makes and reflects recency of contact, not cluster seniority. Sorting
     * by that would favor whichever online node happened to be quietest recently, which is
     * unrelated to (and unstable compared to) how long it's been a cluster member.
     */
    override fun elect(candidates: List<NodeData>): NodeData? {
        return candidates
            .filter { it.state == NodeState.ONLINE && !it.head }
            .minByOrNull { it.firstConnection }
    }
}