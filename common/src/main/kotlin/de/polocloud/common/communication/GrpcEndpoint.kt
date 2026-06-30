package de.polocloud.common.communication

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.communication.tls.ClientAuthMode
import de.polocloud.common.communication.tls.MtlsConfig
import de.polocloud.i18n.api.trDebug
import de.polocloud.i18n.api.trError
import de.polocloud.i18n.api.trInfo
import de.polocloud.i18n.api.trWarn
import io.grpc.*
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth
import io.grpc.protobuf.services.HealthStatusManager
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Represents a gRPC server endpoint that can host multiple services.
 *
 * This class manages the lifecycle of a gRPC server, including starting,
 * stopping, health status reporting, TLS/mTLS support, and graceful shutdown.
 *
 * It is intended for cloud-native usage and supports both optional TLS
 * and optional client certificate authentication.
 *
 * ---
 * **Preferred usage — via [MtlsConfig]:**
 * ```kotlin
 * val endpoint = GrpcEndpoint.Builder(address)
 *     .service(MyService())
 *     .tls(MtlsConfig.mutual(certFile, keyFile, caCertFile))
 *     .build()
 * endpoint.start()
 * ```
 *
 * **Multi-CA usage (CLI + Node on same port):**
 * ```kotlin
 * GrpcEndpoint.Builder(address)
 *     .tls(MtlsConfig.mutual(certFile, keyFile, nodeCaFile, cliCaFile))
 *     .build()
 * ```
 */
class GrpcEndpoint private constructor(
    private val address: Address,
    private val services: List<BindableService>,
    private val interceptedServices: List<ServerServiceDefinition>,
    private val mtlsConfig: MtlsConfig?,
    private val shutdownTimeoutSeconds: Long,
) : Closeable {

    private val logger = LoggerFactory.getLogger(GrpcEndpoint::class.java)

    private val serverRef = AtomicReference<Server?>(null)
    private val healthManager = HealthStatusManager()

    /**
     * Starts the gRPC server if not already running.
     */
    @Synchronized
    fun start() {
        if (serverRef.get() != null) {
            logger.trWarn("grpc", "grpc.start.alreadyRunning", "address" to address)
            return
        }

        logger.trDebug("grpc", "grpc.start.starting", "address" to address)

        val builder = NettyServerBuilder.forAddress(address.toInetSocketAddress())
            .addService(healthManager.healthService)

        mtlsConfig?.let { cfg ->
            val sslContext = runCatching {
                val cf = CertificateFactory.getInstance("X.509")
                val trustedCerts: List<X509Certificate> = cfg.caCerts.flatMap { file ->
                    FileInputStream(file).use { stream ->
                        @Suppress("UNCHECKED_CAST")
                        cf.generateCertificates(stream) as Collection<X509Certificate>
                    }
                }

                GrpcSslContexts.forServer(cfg.certFile, cfg.keyFile)
                    .trustManager(trustedCerts)
                    .clientAuth(cfg.clientAuth.toNetty())
                    .build()
            }.getOrElse { ex ->
                throw IllegalStateException("Failed to initialize TLS for gRPC server at $address", ex)
            }

            builder.sslContext(sslContext)
            logger.trDebug("grpc", "grpc.start.tls.enabled", "clientAuth" to cfg.clientAuth)
        }

        services.forEach(builder::addService)
        interceptedServices.forEach(builder::addService)

        val server = runCatching {
            builder.build().apply {
                start()
                healthManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING)
                logger.trInfo("grpc", "grpc.start.success",
                    "servingStatus" to HealthCheckResponse.ServingStatus.SERVING)
            }
        }.getOrElse { ex ->
            logger.trError("grpc", "grpc.bind_failed", "address" to address)
            throw IllegalStateException("Failed to start gRPC server at $address", ex)
        }

        serverRef.set(server)
    }

    /**
     * Stops the gRPC server gracefully.
     */
    override fun close(mode: ShutdownMode) {
        val server = serverRef.getAndSet(null) ?: run {
            logger.trWarn("grpc", "grpc.shutdown.notRunning")
            return
        }

        logger.trInfo("grpc", "grpc.shutdown.starting")
        healthManager.setStatus("", HealthCheckResponse.ServingStatus.NOT_SERVING)

        if (mode == ShutdownMode.FORCE) {
            logger.trWarn("grpc", "grpc.shutdown.force")
            server.shutdownNow()
            return
        }

        server.shutdown()
        try {
            if (!server.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                logger.trWarn("grpc", "grpc.shutdown.timeout", "timeout" to shutdownTimeoutSeconds)
                server.shutdownNow()
            } else {
                logger.trDebug("grpc", "grpc.shutdown.success")
            }
        } catch (_: InterruptedException) {
            logger.trWarn("grpc", "grpc.shutdown.interrupted")
            server.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    /**
     * Builder for [GrpcEndpoint].
     *
     * **Preferred pattern — [MtlsConfig]-based:**
     * ```kotlin
     * GrpcEndpoint.Builder(address)
     *     .service(MyService())
     *     .tls(MtlsConfig.mutual(certFile, keyFile, caCertFile))
     *     .shutdownTimeout(10)
     *     .build()
     * ```
     */
    class Builder(private val address: Address) {

        private val services = mutableListOf<BindableService>()
        private val interceptedServices = mutableListOf<ServerServiceDefinition>()
        private var mtlsConfig: MtlsConfig? = null
        private var shutdownTimeoutSeconds: Long = 5

        /** Registers a single gRPC service with this endpoint. */
        fun service(service: BindableService) = apply { services += service }

        /** Registers multiple gRPC services with this endpoint. */
        fun services(vararg s: BindableService) = apply { services += s }

        /** Registers a service with one or more server-side interceptors. */
        fun interceptedService(service: BindableService, vararg interceptors: ServerInterceptor) = apply {
            interceptedServices += ServerInterceptors.intercept(service, *interceptors)
        }

        /**
         * Configures TLS/mTLS using an [MtlsConfig] value object.
         *
         * This is the **preferred** way to enable TLS. The config encapsulates
         * cert, key, trusted CAs, and client-auth mode in one place.
         *
         * ```kotlin
         * // Full mTLS — node-to-node or service-to-node
         * .tls(MtlsConfig.mutual(certFile, keyFile, caCertFile))
         *
         * // Multi-CA — accept both CLI and node clients on the same port
         * .tls(MtlsConfig.mutual(certFile, keyFile, nodeCaFile, cliCaFile))
         * ```
         */
        fun tls(config: MtlsConfig) = apply { this.mtlsConfig = config }

        /** Sets the graceful shutdown timeout in seconds (default: 5). */
        fun shutdownTimeout(seconds: Long) = apply { this.shutdownTimeoutSeconds = seconds }

        /** Builds the [GrpcEndpoint] instance. */
        fun build(): GrpcEndpoint = GrpcEndpoint(
            address,
            services.toList(),
            interceptedServices.toList(),
            mtlsConfig,
            shutdownTimeoutSeconds,
        )
    }
}

/**
 * Maps the transport-agnostic [ClientAuthMode] to Netty's [ClientAuth].
 * This extension is `internal` so Netty never leaks into caller code.
 */
internal fun ClientAuthMode.toNetty(): ClientAuth = when (this) {
    ClientAuthMode.NONE     -> ClientAuth.NONE
    ClientAuthMode.OPTIONAL -> ClientAuth.OPTIONAL
    ClientAuthMode.REQUIRE  -> ClientAuth.REQUIRE
}