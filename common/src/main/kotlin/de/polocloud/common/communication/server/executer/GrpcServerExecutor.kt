package de.polocloud.common.communication.server.executer

import de.polocloud.common.communication.server.context.GrpcServerContext
import de.polocloud.common.communication.server.handler.GrpcServerHandler
import de.polocloud.common.communication.server.middleware.GrpcServerMiddleware
import de.polocloud.common.communication.server.registery.GrpcServerHandlerRegistry

/**
 * Central execution engine for all requests.
 *
 * Responsible for:
 * - resolving handlers
 * - executing middleware pipeline
 * - invoking handler
 */
class GrpcServerExecutor(
    private val registry: GrpcServerHandlerRegistry,
    private val middlewares: List<GrpcServerMiddleware> = emptyList()
) {

    /**
     * Executes a request through the pipeline.
     */
    suspend fun <Request : Any, Response : Any> execute(
        request: Request,
        context: GrpcServerContext = GrpcServerContext()
    ): Response {

        val handler = registry.resolve<Request, Response>(request)

        val pipeline = buildPipeline(request, context, handler)

        return pipeline()
    }

    /**
     * Builds middleware chain around handler.
     */
    private fun <Request : Any, Response : Any> buildPipeline(
        request: Request,
        context: GrpcServerContext,
        handler: GrpcServerHandler<Request, Response>
    ): suspend () -> Response {

        val terminal: suspend () -> Response = {
            handler.handle(request, context)
        }

        return middlewares.foldRight(terminal) { middleware, next ->
            suspend {
                middleware.intercept(request, context, next)
            }
        }
    }
}