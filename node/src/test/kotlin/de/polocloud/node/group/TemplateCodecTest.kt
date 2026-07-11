package de.polocloud.node.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TemplateCodecTest {

    @Test
    fun `encodes an empty list`() {
        assertEquals("[]", TemplateCodec.encode(emptyList()))
    }

    @Test
    fun `encodes entries preserving order`() {
        assertEquals("""["GLOBAL","GLOBAL_SERVER","lobby"]""", TemplateCodec.encode(listOf("GLOBAL", "GLOBAL_SERVER", "lobby")))
    }

    @Test
    fun `decodes a valid json array`() {
        val decoded = TemplateCodec.decode("""["GLOBAL","lobby"]""")
        assertEquals(listOf("GLOBAL", "lobby"), decoded)
    }

    @Test
    fun `blank input decodes to an empty list`() {
        assertTrue(TemplateCodec.decode("").isEmpty())
        assertTrue(TemplateCodec.decode("   ").isEmpty())
    }

    @Test
    fun `malformed input decodes to an empty list instead of throwing`() {
        assertTrue(TemplateCodec.decode("not-json").isEmpty())
        assertTrue(TemplateCodec.decode("""{"a":"b"}""").isEmpty()) // object, not array
    }

    @Test
    fun `round-trips arbitrary lists`() {
        val list = listOf("GLOBAL", "GLOBAL_PROXY", "proxy")
        assertEquals(list, TemplateCodec.decode(TemplateCodec.encode(list)))
    }
}
