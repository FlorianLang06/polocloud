package de.polocloud.node.cluster.election

import de.polocloud.node.cluster.election.strategy.ElectionStrategy
import de.polocloud.node.cluster.election.strategy.OldestNodeStrategy
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.proto.NodeState
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Clock.System.now

class NodeElectionService(
    private val strategy: ElectionStrategy = OldestNodeStrategy
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun onNodeCrashed(failed: NodeData) {
        if (failed.state == NodeState.ONLINE) {
            failed.state = NodeState.CRASHED
            NodeRepository.save(failed)
            logger.warn("Node ${failed.name()} marked as CRASHED")
        }

        if (!failed.head) return
        electNewHead(excluding = failed.id)
    }

    fun onHeadNodeLeft(leaving: NodeData) {
        if (!leaving.head) return
        electNewHead(excluding = leaving.id)
    }

    fun electNewHead(excluding: UUID? = null) {
        val candidates = NodeRepository.findAll()
            .filter { it.id != excluding }

        if (excluding != null) {
            NodeRepository.find(excluding)?.let { old ->
                old.head = false
                old.electedAt = null
                NodeRepository.save(old)
            }
        }

        val winner = strategy.elect(candidates) ?: run {
            logger.error("No candidates for election — cluster has no head!")
            return
        }

        promoteToHead(winner)
    }

    private fun promoteToHead(node: NodeData) {
        NodeRepository.findAll()
            .filter { it.head && it.id != node.id }
            .forEach {
                it.head = false
                NodeRepository.save(it)
            }

        node.head = true
        node.electedAt = now()
        NodeRepository.save(node)

        logger.info("Node ${node.name()} elected as new head")
    }
}