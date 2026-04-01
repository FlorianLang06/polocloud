package de.polocloud.common.grpc

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.error.context.ErrorContext
import de.polocloud.common.error.extensions.report
import de.polocloud.common.grpc.error.GrpcError
import io.grpc.BindableService
import io.grpc.Grpc
import io.grpc.Server
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth
import io.grpc.protobuf.services.HealthStatusManager
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
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
    private val caCertFile: File?,
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
            logger.warn("gRPC server is already running on {}", address)
            return
        }

        logger.info("Starting gRPC server on {}", address)

        val builder = NettyServerBuilder.forAddress(address.toInetSocketAddress())
            .addService(healthManager.healthService)

        if (certFile != null && keyFile != null) {
            val sslContext = runCatching {
                val sslBuilder = GrpcSslContexts
                    .forServer(certFile, keyFile)

                if (caCertFile != null) {
                    sslBuilder.trustManager(caCertFile)
                }

                sslBuilder
                    .clientAuth(clientAuth)
                    .build()
            }.getOrElse { e ->
                GrpcError.TlsSetupFailed(e.message ?: "unknown")
                    .report()
                    .throwIfFatal()
                return
            }

            builder.sslContext(sslContext)
            logger.debug("TLS enabled (clientAuth={})", clientAuth)
        }

        services.forEach(builder::addService)
        val server = runCatching {
            builder.build().apply {
                start()
                healthManager.setStatus(
                    "",
                    HealthCheckResponse.ServingStatus.SERVING
                )
                logger.debug("gRPC server started and reporting SERVING")
            }
        }.getOrElse { e ->
            GrpcError.BindFailed(address.toString())
                .also { ErrorContext.from("GrpcEndpoint.start") }
                .report()
                .throwIfFatal()
            return
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
            logger.warn("gRPC server is not running, nothing to close")
            return
        }

        logger.info("Shutting down gRPC server...")
        healthManager.setStatus(
            "",
            HealthCheckResponse.ServingStatus.NOT_SERVING
        )

        if (mode == ShutdownMode.FORCE) {
            logger.warn("Forcing immediate shutdown of gRPC server")
            server.shutdownNow()
            return
        }

        server.shutdown()
        try {
            if (!server.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                GrpcError.ShutdownTimeout(shutdownTimeoutSeconds).report()
                logger.warn(
                    "Server did not terminate in {} seconds, forcing shutdown",
                    shutdownTimeoutSeconds
                )
                server.shutdownNow()
            } else {
                logger.debug("gRPC server terminated gracefully")
            }
        } catch (_: InterruptedException) {
            logger.warn("Shutdown interrupted, forcing immediate shutdown")
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

        private val services = mutableListOf<BindableService>()
        private var caCertFile: File? = null
        private var certFile: File? = null
        private var keyFile: File? = null
        private var clientAuth: ClientAuth = ClientAuth.NONE
        private var shutdownTimeoutSeconds: Long = 5

        /** Registers a gRPC service with this endpoint. */
        fun service(service: BindableService) = apply { services += service }

        /** Registers multiple gRPC services with this endpoint. */
        fun services(vararg s: BindableService) = apply { services += s }

        /**
         * Enables TLS for this gRPC endpoint.
         *
         * @param certFile Server certificate
         * @param keyFile Server private key
         * @param clientAuth Optional client certificate authentication
         */
        fun tls(caCertFile: File, certFile: File, keyFile: File, clientAuth: ClientAuth = ClientAuth.NONE) = apply {
            this.certFile = certFile
            this.keyFile = keyFile
            this.clientAuth = clientAuth
            this.caCertFile = caCertFile
        }

        /** Sets shutdown timeout in seconds for graceful termination. */
        fun shutdownTimeout(seconds: Long) = apply { this.shutdownTimeoutSeconds = seconds }

        /** Builds the [GrpcEndpoint] instance. */
        fun build(): GrpcEndpoint {
            return GrpcEndpoint(
                address,
                services,
                caCertFile,
                certFile,
                keyFile,
                clientAuth,
                shutdownTimeoutSeconds
            )
        }
    }
}