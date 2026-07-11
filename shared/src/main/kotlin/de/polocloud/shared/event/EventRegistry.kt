package de.polocloud.shared.event

import de.polocloud.shared.event.group.GroupUpdatedEvent
import de.polocloud.shared.event.server.PlayerCountChangedEvent
import de.polocloud.shared.event.server.ServerStartedEvent
import de.polocloud.shared.event.server.ServerStoppedEvent
import kotlinx.serialization.KSerializer

/**
 * Single source of truth mapping a wire event name to its serializer.
 *
 * The name is the event's simple class name and travels in
 * [de.polocloud.proto.EventContext.getEventName]. Register every new [Event]
 * here, otherwise it cannot cross the cluster boundary.
 */
object EventRegistry {

    private val serializers: Map<String, KSerializer<out Event>> = mapOf(
        nameOf<ServerStartedEvent>() to ServerStartedEvent.serializer(),
        nameOf<ServerStoppedEvent>() to ServerStoppedEvent.serializer(),
        nameOf<GroupUpdatedEvent>() to GroupUpdatedEvent.serializer(),
        nameOf<PlayerCountChangedEvent>() to PlayerCountChangedEvent.serializer(),
    )

    /** Returns the serializer registered under [name], or `null` if unknown. */
    fun serializer(name: String): KSerializer<out Event>? = serializers[name]

    private inline fun <reified T : Event> nameOf(): String = T::class.java.simpleName
}
