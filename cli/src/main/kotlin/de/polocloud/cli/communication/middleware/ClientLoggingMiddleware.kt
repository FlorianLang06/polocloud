package de.polocloud.cli.communication.middleware

import de.polocloud.cli.logger
import de.polocloud.common.communication.client.call.GrpcClientCall
import de.polocloud.common.communication.client.middleware.GrpcClientMiddleware

class ClientLoggingMiddleware : GrpcClientMiddleware {

    override suspend fun <Response : Any> intercept(
        call: GrpcClientCall<Response>,
        next: suspend () -> Response
    ): Response {

        logger.debug("[<-] ${call::class.simpleName}")

        val result = next()

        logger.debug("[->] ${result::class.simpleName}")

        return result
    }
}