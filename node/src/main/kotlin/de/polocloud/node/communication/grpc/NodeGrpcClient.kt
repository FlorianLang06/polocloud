package de.polocloud.node.communication.grpc

import de.polocloud.common.Address
import de.polocloud.common.communication.tls.GrpcChannelFactory
import de.polocloud.common.communication.tls.MtlsConfig
import de.polocloud.node.security.NodeCertificateStorage
import io.grpc.ManagedChannel

/**
 * Manages the outbound mTLS gRPC channel from this node to another node.
 *
 * TLS material is read from [NodeCertificateStorage] — the same certs that
 * the node received during registration are used to authenticate outbound calls.
 *
 * The channel is created lazily on [connect] and must be released via [disconnect]
 * when no longer needed (e.g. on node shutdown or cluster topology change).
 */
class NodeGrpcClient {

    private var channel: ManagedChannel? = null

    /**
     * Opens a secured mTLS channel to [address].
     *
     * Calling [connect] while already connected is a no-op — disconnect first
     * if you need to switch target.
     */
    fun connect(address: Address) {
        if (channel != null) return

        val config = MtlsConfig.mutual(
            cert = NodeCertificateStorage.certificateFile(),
            key = NodeCertificateStorage.privateKeyFile(),
            caCert = NodeCertificateStorage.caCertificateFile(),
        )

        channel = GrpcChannelFactory.secured(address, config)
    }

    /**
     * Returns the active [ManagedChannel], or throws if not connected.
     */
    fun channel(): ManagedChannel =
        channel ?: error("NodeGrpcClient is not connected — call connect() first")

    /**
     * Gracefully shuts down the channel.
     */
    fun disconnect() {
        channel?.let {
            GrpcChannelFactory.shutdown(it)
            channel = null
        }
    }
}