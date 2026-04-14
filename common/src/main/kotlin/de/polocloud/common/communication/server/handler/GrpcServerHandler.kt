package de.polocloud.common.communication.server.handler

import de.polocloud.common.communication.server.context.GrpcServerContext

/**
 * Represents a single request handler.
 *
 * @param Request request type
 * @param Response response type
 */
interface GrpcServerHandler<Request : Any, Response : Any> {

    suspend fun handle(request: Request, context: GrpcServerContext): Response
}