package de.polocloud.shared.service

import de.polocloud.shared.property.Properties
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServiceTest {

    private fun service(
        group: String = "lobby",
        index: Int = 1,
        state: ServiceState = ServiceState.RUNNING,
        properties: Properties = Properties(),
    ) = Service(
        id = "id-$index",
        index = index,
        group = group,
        state = state,
        port = 30000,
        host = "127.0.0.1",
        pid = 4242,
        properties = properties,
    )

    @Test
    fun `name joins group and index`() {
        assertEquals("lobby-1", service().name())
        assertEquals("survival-5", service(group = "survival", index = 5).name())
    }

    @Test
    fun `properties default to empty`() {
        assertTrue(service().properties.isEmpty())
    }

    @Test
    fun `isFallback reflects the fallback property`() {
        assertFalse(service().isFallback())
        assertTrue(service(properties = Properties().set(Properties.FALLBACK, "true")).isFallback())
        assertFalse(service(properties = Properties().set(Properties.FALLBACK, "false")).isFallback())
    }

    @Test
    fun `is a PropertyHolder`() {
        val holder: de.polocloud.shared.property.PropertyHolder = service()
        assertEquals(0, holder.properties.asMap().size)
    }

    @Test
    fun `serializes and deserializes round-trip including properties and state`() {
        val original = service(properties = Properties().set(Properties.FALLBACK, "true").set("region", "eu"))
        val encoded = Json.encodeToString(Service.serializer(), original)
        val decoded = Json.decodeFromString(Service.serializer(), encoded)
        assertEquals(original, decoded)
        assertEquals(ServiceState.RUNNING, decoded.state)
        assertTrue(decoded.isFallback())
        assertEquals("eu", decoded.properties["region"])
    }
}
