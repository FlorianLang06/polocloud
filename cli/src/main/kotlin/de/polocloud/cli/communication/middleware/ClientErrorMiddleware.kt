package de.polocloud.cli.communication.middleware

import de.polocloud.common.communication.client.call.GrpcClientCall
import de.polocloud.common.communication.client.middleware.GrpcClientMiddleware
import io.grpc.StatusRuntimeException

class ClientErrorMiddleware : GrpcClientMiddleware {

    override suspend fun <Response : Any> intercept(
        call: GrpcClientCall<Response>,
        next: suspend () -> Response
    ): Response {
        try {
            return next()
        } catch (exception: StatusRuntimeException) {
            throw RuntimeException("gRPC error: ${exception.status}", exception)
        }
    }
}