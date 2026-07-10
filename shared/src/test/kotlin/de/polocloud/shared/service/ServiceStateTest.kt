package de.polocloud.shared.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ServiceStateTest {

    @Test
    fun `fromWire parses known states case-insensitively`() {
        assertEquals(ServiceState.RUNNING, ServiceState.fromWire("RUNNING"))
        assertEquals(ServiceState.RUNNING, ServiceState.fromWire("running"))
        assertEquals(ServiceState.STARTING, ServiceState.fromWire("Starting"))
    }

    @Test
    fun `fromWire falls back to UNKNOWN for unrecognised values`() {
        assertEquals(ServiceState.UNKNOWN, ServiceState.fromWire("SOMETHING_NEW"))
        assertEquals(ServiceState.UNKNOWN, ServiceState.fromWire(""))
    }
}
