package de.polocloud.common.communication.server.registery

import de.polocloud.common.communication.server.handler.GrpcServerHandler
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry for all gRPC handlers.
 *
 * Maps request types to their corresponding handler.
 */
class GrpcServerHandlerRegistry {

    private val handlers = ConcurrentHashMap<Class<*>, GrpcServerHandler<*, *>>()

    /**
     * Registers a handler for a specific request type.
     */
    fun <Request : Any, Response : Any> register(
        requestType: Class<Request>,
        handler: GrpcServerHandler<Request, Response>
    ) {
        handlers[requestType] = handler
    }

    /**
     * Resolves a handler for the given request instance.
     */
    @Suppress("UNCHECKED_CAST")
    fun <Request : Any, Response : Any> resolve(request: Request): GrpcServerHandler<Request, Response> {
        return handlers[request::class.java] as? GrpcServerHandler<Request, Response>
            ?: throw IllegalStateException(
                "No handler registered for request type: ${request::class.java.name}"
            )
    }

    /**
     * @return all registered handlers (useful for debugging / metrics)
     */
    fun all(): Map<Class<*>, GrpcServerHandler<*, *>> = handlers.toMap()
}