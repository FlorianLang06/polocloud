package de.polocloud.common.communication.middleware

import de.polocloud.common.communication.context.GrpcContext

/**
 * Middleware that wraps execution of a request.
 *
 * Works like a pipeline (similar to Ktor / Spring filters / Express middleware).
 */
interface GrpcMiddleware {

    suspend fun <Request : Any, Response : Any> intercept(
        request: Request,
        context: GrpcContext,
        next: suspend () -> Response
    ): Response
}