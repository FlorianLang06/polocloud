package de.polocloud.node.communication.grpc.middleware

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.middleware.GrpcServerMiddleware
import org.slf4j.LoggerFactory

class LoggingServerMiddleware : GrpcServerMiddleware {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun <Request : Any, Response : Any> intercept(
        request: Request,
        context: GrpcServerContext,
        next: suspend () -> Response
    ): Response {
        val ip = context.get<String>("clientIp")

        logger.debug("[->] ${request::class.simpleName} from $ip")

        val result = next()

        logger.debug("[<-] ${result::class.simpleName}")

        return result
    }
}