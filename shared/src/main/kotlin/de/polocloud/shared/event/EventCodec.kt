package de.polocloud.shared.event

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** An [Event] reduced to its wire form: a stable [name] plus JSON [data]. */
data class EncodedEvent(val name: String, val data: String)

/**
 * Serializes [Event]s to/from the `(eventName, eventData)` pair carried by
 * [de.polocloud.proto.EventContext].
 *
 * Shared by the node (encode before broadcast) and the api (decode on receive)
 * so both ends agree on the exact wire representation.
 */
object EventCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** The wire name used for events of the given [type]. */
    fun nameOf(type: Class<out Event>): String = type.simpleName

    /** Encodes [event] into its wire form. Throws if the event is unregistered. */
    @Suppress("UNCHECKED_CAST")
    fun encode(event: Event): EncodedEvent {
        val name = event.javaClass.simpleName
        val serializer = EventRegistry.serializer(name)
            ?: error("No serializer registered for event '$name' — add it to EventRegistry")
        return EncodedEvent(name, json.encodeToString(serializer as KSerializer<Event>, event))
    }

    /** Decodes an event from its wire form, or `null` if [name] is unregistered. */
    fun decode(name: String, data: String): Event? {
        val serializer = EventRegistry.serializer(name) ?: return null
        return json.decodeFromString(serializer, data)
    }
}
