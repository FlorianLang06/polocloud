package de.polocloud.shared.service

import kotlinx.serialization.Serializable

/**
 * Lifecycle state of a [Service] as exposed through the public API and events.
 *
 * Mirrors the node's internal service states. [UNKNOWN] is a forward-compatible
 * fallback for any wire value a newer node emits that this client does not yet
 * know, so an unrecognised state degrades gracefully instead of throwing.
 */
@Serializable
enum class ServiceState {
    QUEUED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    UNKNOWN;

    companion object {
        /** Parses a raw state string (e.g. from protobuf), tolerating unknown values. */
        fun fromWire(raw: String): ServiceState =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}