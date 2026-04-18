package de.polocloud.services.sdk.communication

import de.polocloud.common.Address
import de.polocloud.common.communication.tls.GrpcChannelFactory
import de.polocloud.common.communication.tls.MtlsConfig
import io.grpc.ManagedChannel
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

/**
 * Manages the outbound mTLS gRPC channel from a **service instance** to its **node**.
 *
 * The channel is opened lazily on the first [channel] call and torn down via [close].
 *
 * Usage inside any [de.polocloud.services.sdk.Service]:
 * ```kotlin
 * class LobbyService : Service() {
 *     override fun onBoot() {
 *         val stub = NodeServiceGrpcKt.NodeServiceCoroutineStub(nodeConnection.channel())
 *         val info = runBlocking { stub.getNodeInformation(NodeInformationRequest.newBuilder().build()) }
 *         println("Connected to: ${info.nodeName}")
 *     }
 * }
 * ```
 */
class NodeConnection internal constructor(
    private val nodeAddress: Address,
    private val mtlsConfig: MtlsConfig,
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(NodeConnection::class.java)
    private val channelRef = AtomicReference<ManagedChannel?>(null)

    /**
     * Returns the active [ManagedChannel] to the node, opening it if necessary.
     *
     * Thread-safe — concurrent callers will race to create the channel but only
     * one will win; the loser discards its channel immediately.
     */
    fun channel(): ManagedChannel {
        channelRef.get()?.let { return it }

        val fresh = GrpcChannelFactory.secured(nodeAddress, mtlsConfig)

        return if (channelRef.compareAndSet(null, fresh)) {
            logger.debug("Opened mTLS channel to node at {}", nodeAddress)
            fresh
        } else {
            // Another thread won the race — discard ours
            GrpcChannelFactory.shutdown(fresh, timeoutSeconds = 1)
            channelRef.get()!!
        }
    }

    /**
     * Gracefully shuts down the channel.
     * Safe to call multiple times.
     */
    override fun close() {
        channelRef.getAndSet(null)?.let { ch ->
            GrpcChannelFactory.shutdown(ch)
            logger.debug("Closed mTLS channel to node at {}", nodeAddress)
        }
    }
}