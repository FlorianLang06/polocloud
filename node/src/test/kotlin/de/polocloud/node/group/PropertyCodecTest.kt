package de.polocloud.node.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PropertyCodecTest {

    @Test
    fun `encodes an empty map`() {
        assertEquals("{}", PropertyCodec.encode(emptyMap()))
    }

    @Test
    fun `encodes entries preserving order`() {
        assertEquals("""{"a":"1","b":"2"}""", PropertyCodec.encode(linkedMapOf("a" to "1", "b" to "2")))
    }

    @Test
    fun `decodes a valid json object`() {
        val decoded = PropertyCodec.decode("""{"fallback":"true","region":"eu"}""")
        assertEquals("true", decoded["fallback"])
        assertEquals("eu", decoded["region"])
    }

    @Test
    fun `blank input decodes to an empty map`() {
        assertTrue(PropertyCodec.decode("").isEmpty())
        assertTrue(PropertyCodec.decode("   ").isEmpty())
    }

    @Test
    fun `malformed input decodes to an empty map instead of throwing`() {
        assertTrue(PropertyCodec.decode("not-json").isEmpty())
        assertTrue(PropertyCodec.decode("""{"a":123}""").isEmpty()) // value not a string
    }

    @Test
    fun `round-trips arbitrary maps`() {
        val map = mapOf("k1" to "v1", "k2" to "v2")
        assertEquals(map, PropertyCodec.decode(PropertyCodec.encode(map)))
    }

    @Test
    fun `decoded map is mutable`() {
        val decoded = PropertyCodec.decode("""{"a":"1"}""")
        decoded["b"] = "2"
        assertEquals("2", decoded["b"])
    }
}
