package de.polocloud.shared.event.server

import de.polocloud.shared.event.Event
import kotlinx.serialization.Serializable

/**
 * Fired by the node once a service has stopped in the cluster.
 *
 * @param serviceName the stopped service, e.g. `proxy-1`.
 * @param group       the group the service belonged to, e.g. `proxy`.
 */
@Serializable
data class ServerStoppedEvent(
    val serviceName: String,
    val group: String,
) : Event
