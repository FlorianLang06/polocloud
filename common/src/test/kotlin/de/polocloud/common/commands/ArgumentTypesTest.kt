package de.polocloud.common.commands

import de.polocloud.common.commands.type.DoubleArgument
import de.polocloud.common.commands.type.IntArgument
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.common.commands.type.LongArgument
import de.polocloud.common.commands.type.StringArrayArgument
import de.polocloud.common.commands.type.TextArgument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArgumentTypesTest {

    private val ctx = InputContext()

    @Test
    fun `keyword argument only matches its exact key`() {
        val argument = KeywordArgument("list")
        assertTrue(argument.predication("list"))
        assertTrue(argument.predication("LIST"))
        assertFalse(argument.predication("info"))
        assertEquals("", argument.buildResult("list", ctx))
        assertEquals(listOf("list"), argument.defaultArgs(ctx))
    }

    @Test
    fun `text argument rejects blank input`() {
        val argument = TextArgument("name")
        assertTrue(argument.predication("lobby"))
        assertFalse(argument.predication("   "))
        assertEquals("lobby", argument.buildResult("lobby", ctx))
    }

    @Test
    fun `string array argument echoes the joined input`() {
        val argument = StringArrayArgument("value")
        assertEquals("hello world", argument.buildResult("hello world", ctx))
    }

    @Test
    fun `int argument parses and validates bounds`() {
        val argument = IntArgument("memory", minValue = 128, maxValue = 1024)
        assertTrue(argument.predication("512"))
        assertEquals(512, argument.buildResult("512", ctx))
        assertFalse(argument.predication("abc"))
        assertFalse(argument.predication("64"))
        assertFalse(argument.predication("2048"))
    }

    @Test
    fun `long argument parses`() {
        val argument = LongArgument("online")
        assertTrue(argument.predication("9999999999"))
        assertEquals(9999999999L, argument.buildResult("9999999999", ctx))
        assertFalse(argument.predication("x"))
    }

    @Test
    fun `double argument parses`() {
        val argument = DoubleArgument("threshold")
        assertTrue(argument.predication("0.8"))
        assertEquals(0.8, argument.buildResult("0.8", ctx))
        assertFalse(argument.predication("nope"))
    }
}
