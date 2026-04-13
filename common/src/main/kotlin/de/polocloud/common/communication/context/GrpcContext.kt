package de.polocloud.common.communication.context

/**
 * Generic request context passed through the entire gRPC pipeline.
 *
 * This class is intentionally transport-agnostic and can be populated
 * from gRPC, HTTP, CLI, or any other transport layer.
 */
data class GrpcContext(
    val metadata: Map<String, Any?> = emptyMap()
) {

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        return metadata[key] as? T
    }

    fun with(key: String, value: Any?): GrpcContext {
        return copy(metadata = metadata + (key to value))
    }
}