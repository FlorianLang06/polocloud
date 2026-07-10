package de.polocloud.api.services

import de.polocloud.proto.ServiceData
import de.polocloud.shared.property.Properties
import de.polocloud.shared.service.ServiceState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServiceMapperTest {

    @Test
    fun `toApi maps every field including properties and state`() {
        val data = ServiceData.newBuilder()
            .setId("id-1")
            .setIndex(2)
            .setGroup("lobby")
            .setState("RUNNING")
            .setPort(30001)
            .setHost("127.0.0.1")
            .setPid(4321)
            .putProperties(Properties.FALLBACK, "true")
            .build()

        val service = ServiceMapper.toApi(data)

        assertEquals("id-1", service.id)
        assertEquals(2, service.index)
        assertEquals("lobby", service.group)
        assertEquals("lobby-2", service.name())
        assertEquals(ServiceState.RUNNING, service.state)
        assertEquals(30001, service.port)
        assertEquals("127.0.0.1", service.host)
        assertEquals(4321, service.pid)
        assertTrue(service.isFallback())
    }

    @Test
    fun `unknown state degrades to UNKNOWN`() {
        val data = ServiceData.newBuilder().setId("x").setGroup("g").setState("WAT").build()
        assertEquals(ServiceState.UNKNOWN, ServiceMapper.toApi(data).state)
    }

    @Test
    fun `absent properties map to an empty holder`() {
        val data = ServiceData.newBuilder().setId("x").setGroup("g").setState("RUNNING").build()
        assertTrue(ServiceMapper.toApi(data).properties.isEmpty())
    }
}
