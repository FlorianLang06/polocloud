package de.polocloud.common.communication.tls

import de.polocloud.common.Address
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import java.io.FileInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

/**
 * Factory for creating gRPC [ManagedChannel] instances with TLS or mTLS.
 *
 * Centralizes all SSL/TLS channel-building logic so that **any module**
 * (node, service-sdk, CLI tool, …) can open an authenticated channel to a
 * gRPC endpoint without duplicating Netty/SSL boilerplate.
 *
 * ---
 * **Usage — mTLS channel (service → node):**
 * ```kotlin
 * val config = MtlsConfig.mutual(
 *     cert   = certFile,
 *     key    = keyFile,
 *     caCert = caCertFile,
 * )
 * val channel = GrpcChannelFactory.create(nodeAddress, config)
 * ```
 *
 * **Usage — plaintext channel (registration bootstrap):**
 * ```kotlin
 * val channel = GrpcChannelFactory.plaintext(registrationAddress)
 * ```
 *
 * Channels created here should be shut down by the caller when no longer needed:
 * ```kotlin
 * channel.shutdown()
 * channel.awaitTermination(5, TimeUnit.SECONDS)
 * ```
 */
object GrpcChannelFactory {

    /**
     * Creates a [ManagedChannel] secured with the TLS material in [config].
     *
     * The channel presents [MtlsConfig.certFile] + [MtlsConfig.keyFile] as its
     * client identity and trusts the CAs listed in [MtlsConfig.caCerts].
     *
     * @param address target host and port
     * @param config  TLS/mTLS material
     * @return a ready-to-use [ManagedChannel]; the caller owns the lifecycle
     */
    fun secured(address: Address, config: MtlsConfig): ManagedChannel {
        val cf = CertificateFactory.getInstance("X.509")
        val trustedCerts: List<X509Certificate> = config.caCerts.flatMap { file ->
            FileInputStream(file).use { stream ->
                @Suppress("UNCHECKED_CAST")
                cf.generateCertificates(stream) as Collection<X509Certificate>
            }
        }

        val sslContext = GrpcSslContexts.forClient()
            .keyManager(config.certFile, config.keyFile)
            .trustManager(trustedCerts)
            .build()

        return NettyChannelBuilder
            .forAddress(address.hostname, address.port)
            .sslContext(sslContext)
            .build()
    }

    /**
     * Creates an **unencrypted** plaintext [ManagedChannel].
     *
     * Only use this for the initial registration bootstrap where the connecting
     * party does not yet have a signed certificate.
     *
     * @param address target host and port
     * @param shutdownTimeoutSeconds graceful shutdown timeout passed to the channel
     */
    fun plaintext(address: Address): ManagedChannel =
        NettyChannelBuilder
            .forAddress(address.hostname, address.port)
            .usePlaintext()
            .build()

    /**
     * Shuts down [channel] gracefully, waiting up to [timeoutSeconds] before
     * forcing termination.
     *
     * Convenience helper so callers do not need to duplicate the shutdown pattern.
     */
    fun shutdown(channel: ManagedChannel, timeoutSeconds: Long = 5) {
        channel.shutdown()
        if (!channel.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
            channel.shutdownNow()
        }
    }
}