package de.polocloud.common.error

import kotlinx.serialization.Serializable

/**
 * Structured error code with module prefix and numeric ID.
 * Each module defines its own codes as top-level constants.
 *
 * Format: `<PREFIX>-<ID>` (e.g. `NODE-001`, `CFG-002`)
 */
@Serializable
data class ErrorCode(
    val prefix: String,
    val id: Int
) {
    /**
     * Formatted representation, e.g. `NODE-001`
     */
    override fun toString(): String = "$prefix-${id.toString().padStart(3, '0')}"
}