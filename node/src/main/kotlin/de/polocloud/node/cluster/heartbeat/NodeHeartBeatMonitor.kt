package de.polocloud.node.cluster.heartbeat

import de.polocloud.node.cluster.election.NodeElectionService
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.proto.NodeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class NodeHeartBeatMonitor(
    private val electionService: NodeElectionService,
    // Heartbeats are saved every 1s (see NodeHeartBeatService), so this used to give a
    // node only ~2-3 missed beats of slack before being declared CRASHED — easily tripped
    // by a GC pause or a brief network blip, and a false positive here isn't harmless: it
    // can trigger a real head re-election. 15s tolerates a much more realistic amount of
    // jitter while still detecting a genuinely dead node well within human-noticeable time.
    private val timeout: Duration = 15.seconds
) {

    private var job: Job? = null

    fun start() {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                checkAll()
                delay(3.seconds)
            }
        }
    }

    private fun checkAll() {
        val now = now()
        val threshold = now - timeout

        NodeRepository.find(NodeState.ONLINE).forEach { node ->
            val latest = NodeHeartBeatRepository
                .find(node.id)
                .maxByOrNull { it.heartBeatAt }

            if (latest == null || latest.heartBeatAt < node.lastConnection) return@forEach

            if (latest.heartBeatAt < threshold) {
                electionService.onNodeCrashed(node)
            }
        }

        val hasActiveHead = NodeRepository.findAll()
            .any { it.head && it.state == NodeState.ONLINE }

        if (!hasActiveHead) {
            electionService.electNewHead()
        }
    }

    fun stop() = job?.cancel()
}