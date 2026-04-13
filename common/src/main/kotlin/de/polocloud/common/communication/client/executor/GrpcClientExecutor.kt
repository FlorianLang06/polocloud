package de.polocloud.common.communication.client.executor

import de.polocloud.common.communication.client.call.GrpcClientCall
import de.polocloud.common.communication.client.middleware.GrpcClientMiddleware
import io.grpc.ManagedChannel

/**
 * Executes client calls through a middleware pipeline.
 */
class GrpcClientExecutor(
    private val channelProvider: () -> ManagedChannel,
    private val middlewares: List<GrpcClientMiddleware> = emptyList()
) {

    suspend fun <Response : Any> execute(call: GrpcClientCall<Response>): Response {
        val channel = channelProvider()

        val pipeline = middlewares.foldRight(
            initial = suspend { call.execute(channel) }
        ) { middleware, next ->
            suspend { middleware.intercept(call, next) }
        }

        return pipeline()
    }
}