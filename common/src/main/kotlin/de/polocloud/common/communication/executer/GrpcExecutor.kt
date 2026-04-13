package de.polocloud.common.communication.executer

import de.polocloud.common.communication.context.GrpcContext
import de.polocloud.common.communication.handler.GrpcHandler
import de.polocloud.common.communication.middleware.GrpcMiddleware
import de.polocloud.common.communication.registery.GrpcHandlerRegistry

/**
 * Central execution engine for all requests.
 *
 * Responsible for:
 * - resolving handlers
 * - executing middleware pipeline
 * - invoking handler
 */
class GrpcExecutor(
    private val registry: GrpcHandlerRegistry,
    private val middlewares: List<GrpcMiddleware> = emptyList()
) {

    /**
     * Executes a request through the pipeline.
     */
    suspend fun <Request : Any, Response : Any> execute(
        request: Request,
        context: GrpcContext = GrpcContext()
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
        context: GrpcContext,
        handler: GrpcHandler<Request, Response>
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