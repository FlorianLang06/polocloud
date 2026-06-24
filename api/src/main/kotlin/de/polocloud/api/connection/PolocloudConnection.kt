package de.polocloud.api.connection

import de.polocloud.common.Address
import de.polocloud.common.communication.tls.GrpcChannelFactory
import de.polocloud.common.communication.tls.MtlsConfig
import io.grpc.ManagedChannel

/**
 * Owns the standalone mTLS gRPC channel from a service / plugin back to the node.
 *
 * The channel is created lazily on first [channel] access and reused afterwards.
 * TLS material is read from [certificateStorage] — the same certs the node placed
 * in the identity directory when it launched the service. This mirrors how the
 * node and CLI open their mTLS channels.
 */
class PolocloudConnection(
    private val nodeAddress: Address = resolveNodeAddress(),
    private val certificateStorage: ServiceCertificateStorage = ServiceCertificateStorage(),
) {

    private var channel: ManagedChannel? = null

    /**
     * Returns the active mTLS [ManagedChannel], opening it on first use.
     *
     * @throws IllegalStateException if no provisioned identity (certificate + CA) is present.
     */
    @Synchronized
    fun channel(): ManagedChannel {
        channel?.let { return it }

        certificateStorage.initialize()
        check(certificateStorage.isRegistered()) {
            "No provisioned service identity found in '${certificateStorage}'. " +
                "The node must provision a certificate + CA before the API can connect."
        }

        val config = MtlsConfig.mutual(
            cert = certificateStorage.certificateFile(),
            key = certificateStorage.privateKeyFile(),
            caCert = certificateStorage.caCertificateFile(),
        )

        return GrpcChannelFactory.secured(nodeAddress, config).also { channel = it }
    }

    /**
     * Gracefully shuts the channel down. A subsequent [channel] call re-opens it.
     */
    @Synchronized
    fun close() {
        channel?.let { GrpcChannelFactory.shutdown(it) }
        channel = null
    }

    companion object {

        /**
         * Resolves the node address from `polocloud.node.host` / `polocloud.node.port`
         * system properties, the `POLOCLOUD_NODE_HOST` / `POLOCLOUD_NODE_PORT`
         * environment variables, or defaults to `127.0.0.1:4241`.
         *
         * Note: `4241` is the node's **dedicated service/plugin API port**
         * ([de.polocloud.node.communication.grpc.ServiceGrpcEndpoint]) — separate
         * from the CLI/cluster mTLS port (`4240`) and the plaintext registration
         * port (`4239`). The node passes the effective value via
         * `POLOCLOUD_NODE_PORT` when it launches a service.
         */
        fun resolveNodeAddress(): Address {
            val host = System.getProperty("polocloud.node.host")
                ?: System.getenv("POLOCLOUD_NODE_HOST")
                ?: "127.0.0.1"
            val port = (System.getProperty("polocloud.node.port")
                ?: System.getenv("POLOCLOUD_NODE_PORT"))
                ?.toIntOrNull() ?: 4241
            return Address(host, port)
        }
    }
}
