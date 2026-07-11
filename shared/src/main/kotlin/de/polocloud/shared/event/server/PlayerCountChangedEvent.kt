package de.polocloud.shared.event.server

import de.polocloud.shared.event.Event
import de.polocloud.shared.service.Service
import kotlinx.serialization.Serializable

/**
 * Fired by the node whenever a running service's [Service.onlinePlayers] or
 * [Service.maxPlayers] changes, as observed by the node's ping loop
 * ([de.polocloud.node.services.ping.ServicePingFactory]).
 *
 * Not fired on every ping — only when the count actually differs from what was last
 * reported — so a sign/monitor system can react live without polling [service] on an
 * interval of its own.
 *
 * @param service the service with its current, already-updated player counts.
 */
@Serializable
data class PlayerCountChangedEvent(
    val service: Service,
) : Event
