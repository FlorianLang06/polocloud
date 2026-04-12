package de.polocloud.common.grpc

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
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
import java.io.File
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
 */
class GrpcEndpoint private constructor(
    private val address: Address,
    private val services: List<BindableService>,
    private val interceptedServices: List<ServerServiceDefinition>,
    private val caCertFiles: List<File>,
    private val certFile: File?,
    private val keyFile: File?,
    private val clientAuth: ClientAuth,
    private val shutdownTimeoutSeconds: Long
) : Closeable {

    private val logger = LoggerFactory.getLogger(GrpcEndpoint::class.java)

    private val serverRef = AtomicReference<Server?>(null)
    private val healthManager = HealthStatusManager()

    /**
     * Starts the gRPC server if not already running.
     *
     * Registers all provided services and starts reporting SERVING health status.
     * Supports optional TLS and client certificate authentication.
     */
    @Synchronized
    fun start() {
        if (serverRef.get() != null) {
            logger.trWarn("grpc", "grpc.start.alreadyRunning", "address" to address)
            return
        }

        logger.trInfo("grpc", "grpc.start.starting", "address" to address)

        val builder = NettyServerBuilder.forAddress(address.toInetSocketAddress())
            .addService(healthManager.healthService)

        if (certFile != null && keyFile != null) {
            val sslContext = runCatching {
                GrpcSslContexts.forServer(certFile, keyFile)
                    .apply {
                        if (caCertFiles.isNotEmpty()) {
                            // Parse each CA File into X509Certificate objects and pass as Iterable.
                            // This is the only trustManager overload that accepts multiple entries.
                            // Clients presenting a cert signed by ANY of these CAs will be accepted.
                            val cf = CertificateFactory.getInstance("X.509")
                            val certs: List<X509Certificate> = caCertFiles.flatMap { file ->
                                FileInputStream(file).use { stream ->
                                    @Suppress("UNCHECKED_CAST")
                                    cf.generateCertificates(stream) as Collection<X509Certificate>
                                }
                            }
                            trustManager(certs)
                        }
                    }
                    .clientAuth(clientAuth)
                    .build()
            }.getOrElse { exception ->
                throw IllegalStateException("Failed to initialize TLS for gRPC server", exception)
            }

            builder.sslContext(sslContext)
            logger.trDebug("grpc", "grpc.start.tls.enabled", "clientAuth" to clientAuth)
        }

        services.forEach(builder::addService)
        interceptedServices.forEach(builder::addService)

        val server = runCatching {
            builder.build().apply {
                val servingStatus = HealthCheckResponse.ServingStatus.SERVING

                start()
                healthManager.setStatus("", servingStatus)

                logger.trInfo("grpc", "grpc.start.success", "servingStatus" to servingStatus)
            }
        }.getOrElse { exception ->
            logger.trError("grpc", "grpc.bind_failed", "address" to address)
            throw IllegalStateException("Failed to start gRPC server at $address", exception)
        }

        serverRef.set(server)
    }

    /**
     * Stops the gRPC server gracefully.
     *
     * Updates the health status to NOT_SERVING, then shuts down the server.
     * If the server does not terminate within [shutdownTimeoutSeconds], it is forcibly shut down.
     *
     * @param mode Shutdown mode, either [ShutdownMode.GRACEFUL] or [ShutdownMode.FORCE]
     */
    override fun close(mode: ShutdownMode) {
        val server = serverRef.getAndSet(null) ?: run {
            logger.trWarn("grpc", "grpc.shutdown.notRunning")
            return
        }

        logger.trInfo("grpc", "grpc.shutdown.starting")
        healthManager.setStatus(
            "",
            HealthCheckResponse.ServingStatus.NOT_SERVING
        )

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
     * Usage example:
     * ```
     * val endpoint = GrpcEndpoint.Builder(address)
     *     .service(RegistrationService())
     *     .tls(certFile, keyFile) // optional TLS
     *     .clientAuth(ClientAuth.REQUIRE) // optional mTLS
     *     .shutdownTimeout(10)
     *     .build()
     * endpoint.start()
     * ```
     */
    class Builder(private val address: Address) {

        private val interceptedServices = mutableListOf<ServerServiceDefinition>()
        private val services = mutableListOf<BindableService>()
        private val caCertFiles = mutableListOf<File>()
        private var certFile: File? = null
        private var keyFile: File? = null
        private var clientAuth: ClientAuth = ClientAuth.NONE
        private var shutdownTimeoutSeconds: Long = 5

        /** Registers a gRPC service with this endpoint. */
        fun service(service: BindableService) = apply { services += service }

        /** Registers multiple gRPC services with this endpoint. */
        fun services(vararg s: BindableService) = apply { services += s }

        fun interceptedService(service: BindableService, vararg interceptors: ServerInterceptor) = apply {
            interceptedServices += ServerInterceptors.intercept(service, *interceptors)
        }

        /**
         * Enables TLS for this gRPC endpoint.
         *
         * @param certFile Server certificate
         * @param keyFile Server private key
         * @param clientAuth Optional client certificate authentication
         * @param caCertFiles  One or more CA certificates to trust.
         *                     Clients signed by **any** of these CAs will be accepted.
         *                     Pass multiple files to support different client types on the same port.
         */
        fun tls(certFile: File, keyFile: File, clientAuth: ClientAuth = ClientAuth.NONE, vararg caCertFiles: File) = apply {
            this.certFile = certFile
            this.keyFile = keyFile
            this.clientAuth = clientAuth
            this.caCertFiles += caCertFiles
        }

        /** Sets shutdown timeout in seconds for graceful termination. */
        fun shutdownTimeout(seconds: Long) = apply { this.shutdownTimeoutSeconds = seconds }

        /** Builds the [GrpcEndpoint] instance. */
        fun build(): GrpcEndpoint {
            return GrpcEndpoint(
                address,
                services,
                interceptedServices,
                caCertFiles,
                certFile,
                keyFile,
                clientAuth,
                shutdownTimeoutSeconds
            )
        }
    }
}