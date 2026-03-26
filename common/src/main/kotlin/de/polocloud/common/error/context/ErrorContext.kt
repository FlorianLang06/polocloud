package de.polocloud.common.error.context

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Immutable snapshot of the circumstances surrounding an error.
 * Attached to every [de.polocloud.common.error.PoloError] at the point of creation.
 *
 * @param traceId   Unique ID for correlating related errors across components
 * @param timestamp When the error occurred
 * @param source    Human-readable origin (class/function name, e.g. "ClusterManager.connect")
 * @param meta      Optional key-value pairs for extra debugging context
 */
@Serializable
data class ErrorContext(
    val traceId: String = UUID.randomUUID().toString(),
    val timestamp: Long = Instant.now().epochSecond,
    val source: String = "unknown",
    val meta: Map<String, String> = emptyMap(),
) {
    companion object {
        fun from(source: String, vararg meta: Pair<String, String>) = ErrorContext(
            source = source,
            meta = meta.toMap(),
        )
    }
}