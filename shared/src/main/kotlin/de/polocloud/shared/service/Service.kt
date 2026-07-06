package de.polocloud.shared.service

import kotlinx.serialization.Serializable

/**
 * A service in the cluster, as exposed through the public API and carried on
 * cluster events.
 *
 * Lives in `shared` (not `api`) so both the node (which publishes it on
 * [de.polocloud.shared.event.server.ServerStartedEvent] /
 * [de.polocloud.shared.event.server.ServerStoppedEvent]) and the api/bridge
 * (which consume it) can use the exact same type without depending on each other.
 *
 * Obtain instances via [de.polocloud.api.Polocloud.serviceService] or from the
 * server lifecycle events.
 */
@Serializable
data class Service(
    val id: String,
    val index: Int,
    val group: String,
    val state: ServiceState,
    val port: Int,
    /** Host the service is reachable on, e.g. `127.0.0.1`. */
    val host: String,
    val pid: Long,
) {

    /** Cluster-wide service name, e.g. `lobby-1`. */
    fun name() = "$group-$index"
}