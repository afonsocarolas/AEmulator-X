package app.aemu.core

import app.aemu.ui.ExternalFit
import org.junit.Assert.*
import org.junit.Test

class X20FeaturesTest {
    private val W = 51; private val A = 29; private val S = 47; private val D = 32
    private val SPACE = 62; private val UP = 19
    private val map = KeyMap(listOf(KeyMap.Tap(SPACE, 400, 700)), KeyMap.Stick(100, 600, 80))

    @Test fun stickDirections() {
        assertEquals(listOf(InputService.P(9, 100, 520)), map.touches(setOf(W)))
        assertEquals(listOf(InputService.P(9, 180, 600)), map.touches(setOf(D)))
        assertEquals(listOf(InputService.P(9, 100, 520)), map.touches(setOf(UP)))           // arrow keys too
        val diag = map.touches(setOf(W, D)).single()
        assertEquals(156, diag.x); assertEquals(544, diag.y)                               // 80 / √2 each way
        assertTrue(map.touches(setOf(W, S)).isEmpty())                                      // opposite keys cancel
    }

    @Test fun tapsHoldAFinger() {
        assertEquals(listOf(InputService.P(10, 400, 700)), map.touches(setOf(SPACE)))
        assertEquals(2, map.touches(setOf(SPACE, A)).size)
        assertTrue(map.touches(emptySet()).isEmpty())
        assertTrue(map.handles(SPACE) && map.handles(W) && !map.handles(66))
        assertFalse(KeyMap(listOf(KeyMap.Tap(SPACE, 1, 1))).handles(W))                     // no stick: WASD stay keys
    }

    @Test fun editAndSave() {
        val m = map.withTap(KeyMap.Tap(SPACE, 10, 10)).withTap(KeyMap.Tap(66, 300, 300))
        assertEquals(2, m.taps.size); assertEquals(10, m.taps.first { it.key == SPACE }.x)  // a key keeps one spot
        assertEquals(1, m.without(305, 298, 30).taps.size)                                  // nearest spot removed
        assertNull(m.without(100, 600, 30).stick)                                          // the stick too
        assertSame(m.taps, m.without(900, 900, 30).taps)
        val back = KeyMap.parse(m.format())
        assertEquals(m.taps, back.taps); assertEquals(m.stick, back.stick)
        assertTrue(KeyMap.parse("junk\ntap x 1 2\n").isEmpty())
    }

    @Test fun externalFit() {
        assertArrayEquals(intArrayOf(660, 0, 600, 1080), ExternalFit.fit(480, 864, 1920, 1080))   // portrait on a TV
        assertArrayEquals(intArrayOf(0, 0, 1920, 1080), ExternalFit.fit(1280, 720, 1920, 1080))
        assertArrayEquals(intArrayOf(0, 0, 0, 0), ExternalFit.fit(0, 0, 1920, 1080))
    }
}
