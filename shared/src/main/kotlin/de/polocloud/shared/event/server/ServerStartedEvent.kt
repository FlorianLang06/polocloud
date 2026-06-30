package de.polocloud.shared.event.server

import de.polocloud.shared.event.Event
import kotlinx.serialization.Serializable

/**
 * Fired by the node once a service has been started in the cluster.
 *
 * @param serviceName the started service, e.g. `proxy-1`.
 * @param group       the group the service belongs to, e.g. `proxy`.
 */
@Serializable
data class ServerStartedEvent(
    val serviceName: String,
    val group: String,
) : Event
