package de.polocloud.node.communication.grpc.middleware

import de.polocloud.common.communication.context.GrpcContext
import de.polocloud.common.communication.middleware.GrpcMiddleware
import io.grpc.Status

class ErrorMiddleware : GrpcMiddleware {

    override suspend fun <Request : Any, Response : Any> intercept(
        request: Request,
        context: GrpcContext,
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