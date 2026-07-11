package de.polocloud.node.services.queue

import de.polocloud.node.cluster.heartbeat.NodeHeartBeatRepository
import de.polocloud.node.cluster.node.NodeData

/**
 * Reports how loaded a node currently is (0.0..100.0), so [ServiceQueue] can prefer
 * placing new replicas on the least-loaded eligible node instead of blindly round-robining.
 * Kept behind an interface so placement can be unit-tested without a real heartbeat table.
 */
fun interface NodeLoadProvider {
    fun loadOf(node: NodeData): Double
}

/**
 * Real [NodeLoadProvider]: uses [node]'s most recent [de.polocloud.node.cluster.heartbeat.NodeHeartBeat.systemMemoryUsage].
 * Heartbeats are written to the same shared database every node reads [de.polocloud.node.cluster.node.NodeRepository]
 * from, so this reflects a peer's real machine load without any extra RPC. A node with
 * no heartbeat yet (e.g. it only just came online) is treated as idle (0.0) rather than
 * excluded, since that is the common case right after a node joins the cluster.
 */
object HeartbeatNodeLoadProvider : NodeLoadProvider {
    override fun loadOf(node: NodeData): Double =
        NodeHeartBeatRepository.find(node.id).maxByOrNull { it.heartBeatAt }?.systemMemoryUsage ?: 0.0
}