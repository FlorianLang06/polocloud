package de.polocloud.bridge

import de.polocloud.shared.property.Properties
import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FallbackSelectorTest {

    private fun service(
        name: String,
        group: String,
        onlinePlayers: Int = 0,
        state: ServiceState = ServiceState.RUNNING,
        properties: Properties = Properties(),
    ) = Service(
        id = name,
        index = name.substringAfterLast('-').toInt(),
        group = group,
        state = state,
        port = 30000,
        host = "127.0.0.1",
        pid = 1,
        onlinePlayers = onlinePlayers,
        properties = properties,
    )

    @Test
    fun `no candidates returns null`() {
        assertNull(FallbackSelector.select(services = emptyList(), fallbackGroups = emptyMap()))
    }

    @Test
    fun `non-fallback and non-running services are ignored`() {
        val services = listOf(
            service("survival-1", "survival"),
            service("lobby-1", "lobby", state = ServiceState.STARTING),
        )
        assertNull(FallbackSelector.select(services, fallbackGroups = mapOf("lobby" to 0)))
    }

    @Test
    fun `picks the service with fewest online players among equal priority`() {
        val services = listOf(
            service("lobby-1", "lobby", onlinePlayers = 12),
            service("lobby-2", "lobby", onlinePlayers = 3),
            service("lobby-3", "lobby", onlinePlayers = 7),
        )
        val result = FallbackSelector.select(services, fallbackGroups = mapOf("lobby" to 0))
        assertEquals("lobby-2", result?.name())
    }

    @Test
    fun `prefers the higher priority tier even if it has more players`() {
        val services = listOf(
            service("lobby-1", "lobby", onlinePlayers = 1),
            service("hub-1", "hub", onlinePlayers = 10),
        )
        val result = FallbackSelector.select(
            services,
            fallbackGroups = mapOf("lobby" to 0, "hub" to 5),
        )
        assertEquals("hub-1", result?.name())
    }

    @Test
    fun `falls back to a lower priority tier when the top tier has no running service`() {
        val services = listOf(
            service("lobby-1", "lobby", onlinePlayers = 4),
            service("hub-1", "hub", state = ServiceState.STOPPING),
        )
        val result = FallbackSelector.select(
            services,
            fallbackGroups = mapOf("lobby" to 0, "hub" to 5),
        )
        assertEquals("lobby-1", result?.name())
    }

    @Test
    fun `excludes the given service name`() {
        val services = listOf(
            service("lobby-1", "lobby", onlinePlayers = 1),
            service("lobby-2", "lobby", onlinePlayers = 9),
        )
        val result = FallbackSelector.select(
            services,
            fallbackGroups = mapOf("lobby" to 0),
            excludeServiceName = "lobby-1",
        )
        assertEquals("lobby-2", result?.name())
    }

    @Test
    fun `a service-level fallback flag overrides the group priority`() {
        val services = listOf(
            service("lobby-1", "lobby", onlinePlayers = 5),
            service(
                "event-1", "event", onlinePlayers = 5,
                properties = Properties()
                    .set(Properties.FALLBACK, "true")
                    .set(Properties.FALLBACK_PRIORITY, "10"),
            ),
        )
        // "event" is not a known fallback group, but the service itself carries the flag.
        val result = FallbackSelector.select(services, fallbackGroups = mapOf("lobby" to 0))
        assertEquals("event-1", result?.name())
    }
}