package de.polocloud.shared.event.server

import de.polocloud.shared.event.Event
import de.polocloud.shared.service.Service
import kotlinx.serialization.Serializable

/**
 * Fired by the node once a service has been started in the cluster.
 *
 * @param service the started service, including its address (host/port).
 */
@Serializable
data class ServerStartedEvent(
    val service: Service,
) : Event