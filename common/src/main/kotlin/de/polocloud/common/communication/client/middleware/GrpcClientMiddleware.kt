package de.polocloud.common.communication.client.middleware

import de.polocloud.common.communication.client.call.GrpcClientCall

/**
 * Middleware for client-side calls.
 */
interface GrpcClientMiddleware {

    suspend fun <Response : Any> intercept(
        call: GrpcClientCall<Response>,
        next: suspend () -> Response
    ): Response
}