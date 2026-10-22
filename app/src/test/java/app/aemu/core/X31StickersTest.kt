package app.aemu.core

import app.aemu.core.Stickers.Sticker
import org.junit.Assert.*
import org.junit.Test

class X31StickersTest {
    @Test fun clampKeepsStickersOnThePicture() {
        val s = Stickers.clamp(Sticker(1, "🔥", true, x = -0.3f, y = 1.4f, size = 5f, angle = -90f))
        assertEquals(0f, s.x); assertEquals(1f, s.y)
        assertEquals(Stickers.MAX_SIZE, s.size)
        assertEquals(270f, s.angle)
        assertEquals(Stickers.MIN_SIZE, Stickers.clamp(Sticker(2, "x", true, size = 0f)).size)
    }

    @Test fun hitPicksTheTopSticker() {
        val a = Sticker(1, "😀", true, x = 0.5f, y = 0.5f, size = 0.2f)
        val b = Sticker(2, "🔥", true, x = 0.52f, y = 0.5f, size = 0.2f)
        assertEquals(2, Stickers.hit(listOf(a, b), 0.51f, 0.5f, 1.5f)?.id)        // drawn last = on top
        assertEquals(1, Stickers.hit(listOf(a), 0.55f, 0.52f, 1.5f)?.id)
        assertNull(Stickers.hit(listOf(a), 0.9f, 0.9f, 1.5f))
        assertNull(Stickers.hit(emptyList(), 0.5f, 0.5f, 1f))
    }

    @Test fun readableOutline() {
        assertTrue(Stickers.isLight(0xFFFFFFFF.toInt()))
        assertTrue(Stickers.isLight(0xFFFFD600.toInt()))
        assertFalse(Stickers.isLight(0xFF000000.toInt()))
        assertFalse(Stickers.isLight(0xFF2979FF.toInt()))
        assertEquals(Stickers.EMOJIS.size, Stickers.EMOJIS.toSet().size)            // no emoji twice
    }
}
