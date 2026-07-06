package de.polocloud.shared.event.server

import de.polocloud.shared.event.Event
import de.polocloud.shared.service.Service
import kotlinx.serialization.Serializable

/**
 * Fired by the node once a service has stopped in the cluster.
 *
 * @param service the stopped service, including the address (host/port) it had.
 */
@Serializable
data class ServerStoppedEvent(
    val service: Service,
) : Event