package de.polocloud.common.communication.tls

import de.polocloud.common.Address
import io.grpc.LoadBalancerRegistry
import io.grpc.ManagedChannel
import io.grpc.internal.PickFirstLoadBalancerProvider
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
     * Builds a channel with the thread context classloader pinned to gRPC's own
     * classloader.
     *
     * gRPC discovers its [NameResolver][io.grpc.NameResolverProvider] and
     * [LoadBalancer][io.grpc.LoadBalancerProvider] providers (e.g. `dns`,
     * `pick_first`) via `ServiceLoader` and static registries that are initialized
     * lazily on first channel build. Inside an isolated plugin classloader (e.g. a
     * Velocity proxy) the thread context classloader is the host's, which cannot see
     * the providers bundled in the plugin jar — so the registries come up empty and
     * calls fail with "Could not find policy 'pick_first'" / unsupported transport.
     *
     * Pinning the context classloader during the build forces those singleton
     * registries to initialize against the classloader that actually holds the
     * providers; once initialized they stay cached for every later call.
     */
    private fun buildWithGrpcClassLoader(build: () -> ManagedChannel): ManagedChannel {
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = NettyChannelBuilder::class.java.classLoader
        try {
            ensurePickFirstRegistered()
            return build()
        } finally {
            thread.contextClassLoader = previous
        }
    }

    /**
     * Registers gRPC's default `pick_first` load-balancer policy if it is missing.
     *
     * `pick_first` is the default policy every channel falls back to. It ships in
     * grpc-core's `META-INF/services/io.grpc.LoadBalancerProvider`, but shadowJar's
     * `mergeServiceFiles` can drop that entry when several grpc jars provide the same
     * service file — leaving the registry without it and every call failing with
     * "Could not find policy 'pick_first'". The provider class itself is always on the
     * classpath, so we register it directly (referencing the class, no ServiceLoader),
     * which is immune to how the fat jar happens to merge service files.
     */
    private fun ensurePickFirstRegistered() {
        val registry = LoadBalancerRegistry.getDefaultRegistry()
        if (registry.getProvider("pick_first") != null) return
        runCatching { registry.register(PickFirstLoadBalancerProvider()) }
    }

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

        // Use the SocketAddress overload (direct-address resolver) instead of the
        // host/port one: the latter relies on the NameResolver registry's default
        // scheme, which resolves to "unix" inside an isolated plugin classloader
        // (e.g. a Velocity proxy) where gRPC's hard-coded DNS provider fails to
        // load — producing `unix:///host:port` and an unsupported-transport error.
        // overrideAuthority keeps the `host:port` authority the host/port overload
        // used, so TLS verification is unchanged.
        return buildWithGrpcClassLoader {
            NettyChannelBuilder
                .forAddress(address.toInetSocketAddress())
                .overrideAuthority(address.asString())
                .sslContext(sslContext)
                .build()
        }
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
        buildWithGrpcClassLoader {
            NettyChannelBuilder
                .forAddress(address.toInetSocketAddress())
                .overrideAuthority(address.asString())
                .usePlaintext()
                .build()
        }

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