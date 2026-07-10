package de.polocloud.node.event

import de.polocloud.common.Address
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.communication.grpc.NodeGrpcClient
import de.polocloud.proto.NodeServiceGrpcKt
import de.polocloud.proto.NodeState
import de.polocloud.proto.RelayEventRequest
import de.polocloud.shared.event.EncodedEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory

/**
 * Forwards this node's locally-fired cluster events to every other online node.
 *
 * Installed as [ClusterEventService.peerRelay] while the node is running: each origin
 * event triggers a best-effort unary `RelayEvent` fan-out to peers. The receiving node
 * re-broadcasts it to its own local subscribers only (via [ClusterEventService.broadcast]),
 * without relaying again — so events reach the whole cluster without loops.
 *
 * A single-node cluster has no peers, so this is a no-op. Per-peer failures are isolated
 * and never affect local delivery.
 *
 * @param localNodeId id of this node, excluded from the fan-out.
 * @param peers supplies the target nodes — injectable for testing.
 * @param send delivers one encoded event to one peer — injectable for testing.
 */
class ClusterEventRelay(
    private val localNodeId: String,
    private val peers: () -> List<NodeData> = {
        runCatching { NodeRepository.find(NodeState.ONLINE) }.getOrDefault(emptyList())
    },
    private val send: suspend (NodeData, EncodedEvent) -> Unit = ::relayViaGrpc,
) {

    private val logger = LoggerFactory.getLogger(ClusterEventRelay::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Registers this relay as the cluster event fan-out hook. */
    fun install() {
        ClusterEventService.peerRelay = ::dispatch
    }

    /** Removes the hook and cancels any in-flight relays. */
    fun close() {
        ClusterEventService.peerRelay = null
        scope.cancel()
    }

    private fun dispatch(encoded: EncodedEvent) {
        val others = peers().filter { it.id.toString() != localNodeId }
        others.forEach { node ->
            scope.launch {
                runCatching { send(node, encoded) }
                    .onFailure { logger.warn("Failed to relay event '{}' to node {}: {}", encoded.name, node.name(), it.message) }
            }
        }
    }

    private companion object {
        const val RELAY_TIMEOUT_MILLIS = 3_000L

        /** Real transport: a short-lived mTLS unary call to the peer's `NodeService.RelayEvent`. */
        suspend fun relayViaGrpc(node: NodeData, encoded: EncodedEvent) {
            val client = NodeGrpcClient()
            try {
                client.connect(Address(node.hostname, node.port))
                val stub = NodeServiceGrpcKt.NodeServiceCoroutineStub(client.channel())
                val request = RelayEventRequest.newBuilder()
                    .setEventName(encoded.name)
                    .setEventData(encoded.data)
                    .build()
                withTimeout(RELAY_TIMEOUT_MILLIS) { stub.relayEvent(request) }
            } finally {
                client.disconnect()
            }
        }
    }
}
