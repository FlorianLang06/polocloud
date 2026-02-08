package dev.httpmarco.polocloud.common.grpc

import dev.httpmarco.polocloud.common.Closeable
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.HealthStatusManager
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Represents a gRPC server endpoint that can host multiple services.
 *
 * This class manages the lifecycle of a gRPC server, including starting,
 * stopping, and health management. It is intended for cloud-native usage
 * and supports graceful shutdown with health status updates.
 *
 * @param port the port number on which the gRPC server will listen.
 * @param services one or more gRPC services to register with this endpoint.
 */
class GrpcEndpoint(
    private val port: Int,
    vararg services: BindableService
) : Closeable {

    private val logger = LoggerFactory.getLogger(GrpcEndpoint::class.java)

    private var server: Server? = null
    private val services = services.toList()
    private val healthManager = HealthStatusManager()

    /**
     * Starts the gRPC server and registers all provided services.
     *
     * If the server is already running, this method does nothing.
     * The health service is also registered automatically, reporting SERVING status.
     */
    fun connect() {
        if (server != null) {
            logger.warn("gRPC server is already running on port {}", port)
            return
        }

        logger.info("Starting gRPC server on port {}", port)
        val builder = ServerBuilder.forPort(port)
            .addService(healthManager.healthService)

        services.forEach { service ->
            logger.debug("Registering gRPC service: {}", service.javaClass.simpleName)
            builder.addService(service)
        }

        server = builder.build().also {
            it.start()
            healthManager.setStatus(
                "",
                io.grpc.health.v1.HealthCheckResponse.ServingStatus.SERVING
            )
            logger.info("gRPC server started and reporting SERVING")
        }
    }

    /**
     * Stops the gRPC server gracefully.
     *
     * Updates the health status to NOT_SERVING, then shuts down the server.
     * If the server does not terminate within 5 seconds, it is forcibly shut down.
     */
    override fun close() {
        val s = server ?: run {
            logger.warn("gRPC server is not running, nothing to close")
            return
        }

        logger.info("Shutting down gRPC server on port {}", port)
        healthManager.setStatus(
            "",
            io.grpc.health.v1.HealthCheckResponse.ServingStatus.NOT_SERVING
        )

        s.shutdown()
        if (!s.awaitTermination(5, TimeUnit.SECONDS)) {
            logger.warn("gRPC server did not terminate in 5 seconds, forcing shutdown")
            s.shutdownNow()
        } else {
            logger.info("gRPC server terminated gracefully")
        }
    }
}
