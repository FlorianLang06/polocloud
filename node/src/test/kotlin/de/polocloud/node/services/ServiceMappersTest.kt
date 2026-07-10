package de.polocloud.node.services

import de.polocloud.proto.ServiceState as ProtoServiceState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ServiceMappersTest {

    private fun local(
        state: ServiceState = ServiceState.RUNNING,
        index: Int = 1,
        group: String = "lobby",
        port: Int = 30000,
    ): LocalService {
        val service = LocalService(Service(UUID.randomUUID(), index, group, state, "127.0.0.1", port))
        service.host = "10.0.0.5"
        service.properties["fallback"] = "true"
        return service
    }

    @Test
    fun `ServiceProtoMapper maps fields, host and properties`() {
        val service = local()
        val data = ServiceProtoMapper.toProto(service)

        assertEquals(service.id.toString(), data.id)
        assertEquals(1, data.index)
        assertEquals("lobby", data.group)
        assertEquals("RUNNING", data.state)
        assertEquals(30000, data.port)
        assertEquals("10.0.0.5", data.host)
        assertEquals(-1L, data.pid) // no process attached
        assertEquals("true", data.propertiesMap["fallback"])
    }

    @Test
    fun `ServiceEventMapper maps to the shared model with properties`() {
        val service = local(state = ServiceState.STARTING)
        val shared = ServiceEventMapper.toShared(service)

        assertEquals("lobby-1", shared.name())
        assertEquals(de.polocloud.shared.service.ServiceState.STARTING, shared.state)
        assertEquals("10.0.0.5", shared.host)
        assertEquals(-1L, shared.pid)
        assertTrue(shared.isFallback())
    }

    @Test
    fun `ServiceProcessProtoMapper maps state and properties`() {
        val service = local(state = ServiceState.RUNNING, index = 3, group = "proxy", port = 25565)
        val proto = ServiceProcessProtoMapper.toProto(service)

        assertEquals(service.id.toString(), proto.uuid)
        assertEquals(3, proto.index)
        assertEquals("proxy", proto.plan)
        assertEquals(25565, proto.boundPort)
        assertEquals(ProtoServiceState.RUNNING, proto.state)
        assertEquals("true", proto.propertiesMap["fallback"])
    }

    @Test
    fun `ServiceProcessProtoMapper maps every node state to a proto state`() {
        assertEquals(ProtoServiceState.LOADING, stateOf(ServiceState.QUEUED))
        assertEquals(ProtoServiceState.BOOTING, stateOf(ServiceState.STARTING))
        assertEquals(ProtoServiceState.RUNNING, stateOf(ServiceState.RUNNING))
        assertEquals(ProtoServiceState.UNCONTROLLED, stateOf(ServiceState.STOPPING))
        assertEquals(ProtoServiceState.UNCONTROLLED, stateOf(ServiceState.STOPPED))
    }

    private fun stateOf(state: ServiceState): ProtoServiceState =
        ServiceProcessProtoMapper.toProto(local(state = state)).state
}
