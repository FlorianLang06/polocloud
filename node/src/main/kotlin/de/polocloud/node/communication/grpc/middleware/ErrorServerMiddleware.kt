package de.polocloud.node.communication.grpc.middleware

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.middleware.GrpcServerMiddleware
import io.grpc.Status

class ErrorServerMiddleware : GrpcServerMiddleware {

    override suspend fun <Request : Any, Response : Any> intercept(
        request: Request,
        context: GrpcServerContext,
        next: suspend () -> Response
    ): Response {
        try {
            return next()
        } catch (ex: IllegalStateException) {
            throw Status.UNAUTHENTICATED
                .withDescription(ex.message)
                .asRuntimeException()
        } catch (ex: Exception) {
            throw Status.INTERNAL
                .withDescription(ex.message)
                .asRuntimeException()
        }
    }
}