package de.polocloud.common.communication.server.middleware

import de.polocloud.common.communication.server.context.GrpcServerContext

/**
 * Middleware that wraps execution of a request.
 *
 * Works like a pipeline (similar to Ktor / Spring filters / Express middleware).
 */
interface GrpcServerMiddleware {

    suspend fun <Request : Any, Response : Any> intercept(
        request: Request,
        context: GrpcServerContext,
        next: suspend () -> Response
    ): Response
}