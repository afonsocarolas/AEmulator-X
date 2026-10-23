package app.aemu.core

import app.aemu.core.FirmwareTags.Tag
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class X32FeaturesTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    @Test fun tags() {
        // added yesterday, never booted, 64-bit
        assertEquals(listOf(Tag.NEW, Tag.NEVER_BOOTED, Tag.BIT64),
            FirmwareTags.of(now - day, 0, 0, "arm64-v8a", 29, false, false, now))
        // old, played a lot, quick to boot, Gingerbread
        assertEquals(listOf(Tag.TOP, Tag.FAST, Tag.CLASSIC),
            FirmwareTags.of(now - 30 * day, 12, 20_000, "armeabi-v7a", 10, false, true, now))
        assertEquals(listOf(Tag.CONTAINER), FirmwareTags.of(now - 30 * day, 2, 60_000, "armeabi-v7a", 19, true, false, now))
        assertEquals(2, FirmwareTags.of(now, 0, 0, "arm64-v8a", 8, true, true, now, max = 2).size)
    }

    @Test fun topPlayed() {
        assertEquals("b", FirmwareTags.topPlayed(mapOf("a" to 20 * 60_000L, "b" to 90 * 60_000L)))
        assertNull(FirmwareTags.topPlayed(mapOf("a" to 60_000L)))                  // a minute is not enough
        assertNull(FirmwareTags.topPlayed(emptyMap()))
    }

    @Test fun slideOrderAvoidsRepeats() {
        val items = List(6) { "A$it" } + List(5) { "B$it" } + List(2) { "C$it" }
        repeat(20) { seed ->
            val order = SlideOrder.interleave(items, { it.take(1) }, Random(seed))
            assertEquals(items.toSet(), order.toSet())
            assertTrue(order.zipWithNext().none { (x, y) -> x[0] == y[0] })     // never the same firmware twice in a row
        }
        assertEquals(listOf("A1", "A2").toSet(), SlideOrder.interleave(listOf("A1", "A2"), { "A" }).toSet())
        assertEquals(1f, SlideOrder.zoom(0f), 1e-6f); assertEquals(1.12f, SlideOrder.zoom(SlideOrder.SECONDS.toFloat()), 1e-6f)
    }
}
