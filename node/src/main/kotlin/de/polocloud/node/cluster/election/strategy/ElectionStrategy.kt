package de.polocloud.node.cluster.election.strategy

import de.polocloud.node.cluster.node.NodeData

fun interface ElectionStrategy {
    fun elect(candidates: List<NodeData>): NodeData?
}