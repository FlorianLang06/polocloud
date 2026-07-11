package de.polocloud.node.event

import de.polocloud.common.Address
import de.polocloud.common.communication.tls.GrpcChannelFactory
import de.polocloud.common.communication.tls.MtlsConfig
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.cluster.node.NodeRepository
import de.polocloud.node.security.NodeCertificateStorage
import de.polocloud.proto.NodeServiceGrpcKt
import de.polocloud.proto.NodeState
import de.polocloud.proto.RelayEventRequest
import de.polocloud.shared.event.EncodedEvent
import io.grpc.ManagedChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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
 * The mTLS channel to each peer is opened once and cached per node id ([channels]) rather
 * than reconnected on every relay: with frequent events (e.g. player-count updates every
 * few seconds per running service), reconnecting per call would mean a fresh TCP+TLS
 * handshake per event per peer.
 *
 * @param localNodeId id of this node, excluded from the fan-out.
 * @param peers supplies the target nodes — injectable for testing.
 * @param send delivers one encoded event to one peer — injectable for testing; defaults to
 *             the cached-channel gRPC transport ([relayViaGrpc]).
 */
class ClusterEventRelay(
    private val localNodeId: String,
    private val peers: () -> List<NodeData> = {
        runCatching { NodeRepository.find(NodeState.ONLINE) }.getOrDefault(emptyList())
    },
    send: (suspend (NodeData, EncodedEvent) -> Unit)? = null,
) {

    private val logger = LoggerFactory.getLogger(ClusterEventRelay::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channels = ConcurrentHashMap<UUID, ManagedChannel>()
    private val send: suspend (NodeData, EncodedEvent) -> Unit = send ?: ::relayViaGrpc

    /** Registers this relay as the cluster event fan-out hook. */
    fun install() {
        ClusterEventService.peerRelay = ::dispatch
    }

    /** Removes the hook, cancels any in-flight relays and closes every cached channel. */
    fun close() {
        ClusterEventService.peerRelay = null
        scope.cancel()
        channels.values.forEach { runCatching { GrpcChannelFactory.shutdown(it) } }
        channels.clear()
    }

    private fun dispatch(encoded: EncodedEvent) {
        val others = peers().filter { it.id.toString() != localNodeId }
        others.forEach { node ->
            scope.launch {
                runCatching { send(node, encoded) }
                    .onFailure {
                        logger.warn("Failed to relay event '{}' to node {}: {}", encoded.name, node.name(), it.message)
                        // Drop the cached channel on failure so a stale connection (peer
                        // restarted, address changed, cert rotated, ...) doesn't keep
                        // failing forever instead of being re-established on the next event.
                        channels.remove(node.id)?.let { channel -> runCatching { GrpcChannelFactory.shutdown(channel) } }
                    }
            }
        }
    }

    /** Real transport: a unary call to the peer's `NodeService.RelayEvent` over its cached channel. */
    private suspend fun relayViaGrpc(node: NodeData, encoded: EncodedEvent) {
        val stub = NodeServiceGrpcKt.NodeServiceCoroutineStub(channelFor(node))
        val request = RelayEventRequest.newBuilder()
            .setEventName(encoded.name)
            .setEventData(encoded.data)
            .build()
        withTimeout(RELAY_TIMEOUT_MILLIS) { stub.relayEvent(request) }
    }

    private fun channelFor(node: NodeData): ManagedChannel = channels.computeIfAbsent(node.id) {
        val config = MtlsConfig.mutual(
            cert = NodeCertificateStorage.certificateFile(),
            key = NodeCertificateStorage.privateKeyFile(),
            caCert = NodeCertificateStorage.caCertificateFile(),
        )
        GrpcChannelFactory.secured(Address(node.hostname, node.port), config)
    }

    private companion object {
        const val RELAY_TIMEOUT_MILLIS = 3_000L
    }
}
