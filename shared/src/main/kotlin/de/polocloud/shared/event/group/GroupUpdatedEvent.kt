package de.polocloud.shared.event.group

import de.polocloud.shared.event.Event
import de.polocloud.shared.property.Properties
import kotlinx.serialization.Serializable

/**
 * Fired by the node when a group is updated — carries the group name and its full
 * current [properties] so consumers (e.g. the bridge's fallback-group tracking)
 * can stay in sync live instead of re-querying.
 *
 * @param name the group's name.
 * @param properties the group's properties after the update.
 */
@Serializable
data class GroupUpdatedEvent(
    val name: String,
    val properties: Properties = Properties(),
) : Event
