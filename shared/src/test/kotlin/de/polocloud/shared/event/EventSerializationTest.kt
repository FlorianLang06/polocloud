package de.polocloud.shared.event

import de.polocloud.shared.event.group.GroupUpdatedEvent
import de.polocloud.shared.event.server.ServerStartedEvent
import de.polocloud.shared.event.server.ServerStoppedEvent
import de.polocloud.shared.property.Properties
import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventSerializationTest {

    private val service = Service(
        id = "id-1", index = 1, group = "lobby", state = ServiceState.RUNNING,
        port = 30000, host = "127.0.0.1", pid = 99,
        properties = Properties().set(Properties.FALLBACK, "true"),
    )

    @Test
    fun `every shipped event is registered`() {
        assertNotNull(EventRegistry.serializer("ServerStartedEvent"))
        assertNotNull(EventRegistry.serializer("ServerStoppedEvent"))
        assertNotNull(EventRegistry.serializer("GroupUpdatedEvent"))
    }

    @Test
    fun `unknown event names resolve to null`() {
        assertNull(EventRegistry.serializer("NopeEvent"))
    }

    @Test
    fun `nameOf uses the simple class name`() {
        assertEquals("ServerStartedEvent", EventCodec.nameOf(ServerStartedEvent::class.java))
    }

    @Test
    fun `ServerStartedEvent round-trips through the codec`() {
        val encoded = EventCodec.encode(ServerStartedEvent(service))
        assertEquals("ServerStartedEvent", encoded.name)

        val decoded = EventCodec.decode(encoded.name, encoded.data)
        val event = assertInstanceOf(ServerStartedEvent::class.java, decoded)
        assertEquals(service, event.service)
        assertTrue(event.service.isFallback())
    }

    @Test
    fun `ServerStoppedEvent round-trips through the codec`() {
        val encoded = EventCodec.encode(ServerStoppedEvent(service))
        val decoded = EventCodec.decode(encoded.name, encoded.data)
        val event = assertInstanceOf(ServerStoppedEvent::class.java, decoded)
        assertEquals(service.name(), event.service.name())
    }

    @Test
    fun `GroupUpdatedEvent round-trips with its properties`() {
        val event = GroupUpdatedEvent("lobby", Properties().set(Properties.FALLBACK, "true").set("k", "v"))
        val encoded = EventCodec.encode(event)
        assertEquals("GroupUpdatedEvent", encoded.name)

        val decoded = EventCodec.decode(encoded.name, encoded.data)
        val result = assertInstanceOf(GroupUpdatedEvent::class.java, decoded)
        assertEquals("lobby", result.name)
        assertTrue(result.properties.getBoolean(Properties.FALLBACK))
        assertEquals("v", result.properties["k"])
    }

    @Test
    fun `decode returns null for an unregistered name`() {
        assertNull(EventCodec.decode("NopeEvent", "{}"))
    }

    @Test
    fun `encode throws for an unregistered event`() {
        val bogus = object : Event {}
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) {
            EventCodec.encode(bogus)
        }
    }
}
