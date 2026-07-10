package de.polocloud.shared.property

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PropertiesTest {

    @Test
    fun `get returns stored value or null`() {
        val properties = Properties().set("a", "1")
        assertEquals("1", properties["a"])
        assertNull(properties["missing"])
    }

    @Test
    fun `getOrDefault falls back when absent`() {
        val properties = Properties().set("a", "1")
        assertEquals("1", properties.getOrDefault("a", "x"))
        assertEquals("x", properties.getOrDefault("missing", "x"))
    }

    @Test
    fun `getBoolean parses strictly and honours the default`() {
        val properties = Properties()
            .set("yes", "true")
            .set("no", "false")
            .set("weird", "TRUE")
            .set("garbage", "notabool")
        assertTrue(properties.getBoolean("yes"))
        assertFalse(properties.getBoolean("no"))
        // toBooleanStrictOrNull only accepts lowercase true/false
        assertFalse(properties.getBoolean("weird"))
        assertFalse(properties.getBoolean("garbage"))
        assertTrue(properties.getBoolean("missing", default = true))
        assertFalse(properties.getBoolean("missing"))
    }

    @Test
    fun `getInt parses or defaults`() {
        val properties = Properties().set("n", "42").set("bad", "x")
        assertEquals(42, properties.getInt("n"))
        assertEquals(0, properties.getInt("bad"))
        assertEquals(7, properties.getInt("missing", default = 7))
    }

    @Test
    fun `has and remove behave correctly`() {
        val properties = Properties().set("a", "1")
        assertTrue(properties.has("a"))
        properties.remove("a")
        assertFalse(properties.has("a"))
        assertTrue(properties.isEmpty())
    }

    @Test
    fun `set is chainable and overwrites`() {
        val properties = Properties().set("a", "1").set("a", "2")
        assertEquals("2", properties["a"])
    }

    @Test
    fun `asMap returns an independent snapshot`() {
        val properties = Properties().set("a", "1")
        val snapshot = properties.asMap()
        properties.set("b", "2")
        // The earlier snapshot must not see the later mutation.
        assertEquals(setOf("a"), snapshot.keys)
        assertEquals(setOf("a", "b"), properties.asMap().keys)
    }

    @Test
    fun `of copies the source map`() {
        val source = linkedMapOf("a" to "1")
        val properties = Properties.of(source)
        source["b"] = "2"
        assertFalse(properties.has("b"))
    }

    @Test
    fun `copyWith returns a new instance without mutating the original`() {
        val original = Properties().set("a", "1")
        val copy = original.copyWith("b", "2")
        assertNotSame(original, copy)
        assertFalse(original.has("b"))
        assertTrue(copy.has("b"))
        assertEquals("1", copy["a"])
    }

    @Test
    fun `equality and hashCode are value based`() {
        val a = Properties().set("k", "v")
        val b = Properties().set("k", "v")
        val c = Properties().set("k", "other")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a == c)
    }

    @Test
    fun `iteration order is preserved`() {
        val properties = Properties().set("z", "1").set("a", "2").set("m", "3")
        assertEquals(listOf("z", "a", "m"), properties.asMap().keys.toList())
    }

    @Test
    fun `fallback constant is stable`() {
        assertEquals("fallback", Properties.FALLBACK)
    }
}
