package dev.httpmarco.polocloud.common.grpc

import dev.httpmarco.polocloud.common.Closeable
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.HealthStatusManager
import java.util.concurrent.TimeUnit

class GrpcEndpoint(
    private val port: Int,
    vararg services: BindableService
) : Closeable {

    private var server: Server? = null
    private val services = services.toList()
    private val healthManager = HealthStatusManager()

    fun connect() {
        if (server != null) return

        val builder = ServerBuilder.forPort(port)
            .addService(healthManager.healthService)


        services.forEach { builder.addService(it) }
        server = builder.build().also { it.start() }
    }

    override fun close() {
        val s = server ?: return

        healthManager.setStatus(
            "",
            io.grpc.health.v1.HealthCheckResponse.ServingStatus.NOT_SERVING
        )

        s.shutdown()
        if (!s.awaitTermination(5, TimeUnit.SECONDS)) {
            s.shutdownNow()
        }
    }
}
