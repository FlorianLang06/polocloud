package de.polocloud.node.communication.grpc.middleware

import de.polocloud.common.communication.context.GrpcContext
import de.polocloud.common.communication.middleware.GrpcMiddleware
import org.slf4j.LoggerFactory

class LoggingMiddleware : GrpcMiddleware {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun <Request : Any, Response : Any> intercept(
        request: Request,
        context: GrpcContext,
        next: suspend () -> Response
    ): Response {
        val ip = context.get<String>("clientIp")

        logger.debug("[->] ${request::class.simpleName} from $ip")

        val result = next()

        logger.debug("[<-] ${result::class.simpleName}")

        return result
    }
}