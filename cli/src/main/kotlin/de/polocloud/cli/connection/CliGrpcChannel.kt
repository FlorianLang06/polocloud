package de.polocloud.cli.connection

import de.polocloud.common.Address
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import org.slf4j.LoggerFactory

/**
 * Holds the mTLS gRPC channel to the cluster after a successful registration.
 *
 * The channel is created with:
 * - Client certificate = CLI cert signed by the cluster's CLI CA
 * - Trust anchor       = CLI CA certificate received during registration
 *
 * This mirrors exactly how node↔node connections work in [NodeGrpcClient].
 *
 * Use [isConnected] to check channel liveness and [close] to shut down cleanly.
 */
class CliGrpcChannel(
    private val certificateStorage: CliCertificateStorage,
)  {

    private val logger = LoggerFactory.getLogger(CliGrpcChannel::class.java)

    private var channel: ManagedChannel? = null

    val isConnected: Boolean
        get() = channel != null && !channel!!.isShutdown && !channel!!.isTerminated

    /**
     * Opens the mTLS channel to [address].
     * Must only be called after [CliCertificateStorage.isRegistered] returns true.
     */
    fun connect(address: Address) {
        check(certificateStorage.isRegistered()) {
            "Cannot connect: CLI is not registered. Call CliRegistrationClient.register() first."
        }

        logger.info("Opening mTLS channel to cluster at $address")

        val sslContext = GrpcSslContexts.forClient()
            .keyManager(
                certificateStorage.certificateFile(),
                certificateStorage.privateKeyFile()
            )
            .trustManager(
                certificateStorage.caCertificateFile()
            )
            .build()

        channel = NettyChannelBuilder
            .forAddress(address.hostname, address.port)
            .sslContext(sslContext)
            .build()

        logger.info("mTLS channel established")
    }

    /**
     * Returns the underlying [ManagedChannel].
     * Use this to create gRPC stubs for specific services.
     *
     * @throws IllegalStateException if the channel is not connected
     */
    fun channel(): ManagedChannel =
        checkNotNull(channel) { "Channel is not connected. Call connect() first." }

    /**
     * Shuts down the channel gracefully.
     */
    fun close() {
        channel?.shutdown()
        channel = null
        logger.info("mTLS channel closed")
    }
}