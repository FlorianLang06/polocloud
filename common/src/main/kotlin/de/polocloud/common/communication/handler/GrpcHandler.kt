package de.polocloud.common.communication.handler

import de.polocloud.common.communication.context.GrpcContext

/**
 * Represents a single request handler.
 *
 * @param Request request type
 * @param Response response type
 */
interface GrpcHandler<Request : Any, Response : Any> {

    suspend fun handle(request: Request, context: GrpcContext): Response
}