package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class X25FeaturesTest {
    @Test fun padLayoutRoundTrip() {
        val l = PadLayout().with(PadLayout.Group.FACE, PadLayout.Spot(0.7f, 0.6f, 1.4f))
            .with(PadLayout.Group.L, PadLayout.Spot(0.1f, 0.3f, 1f, shown = true)).copy(opacity = 0.5f)
        val back = PadLayout.parse(l.format())
        assertEquals(l, back)
        assertTrue(back.spot(PadLayout.Group.L).shown)
        assertFalse(PadLayout().spot(PadLayout.Group.R).shown)                     // L/R hidden by default
    }

    @Test fun padLayoutLimits() {
        val l = PadLayout().with(PadLayout.Group.DPAD, PadLayout.Spot(-3f, 9f, 99f))
        val s = l.spot(PadLayout.Group.DPAD)
        assertEquals(0f, s.x); assertEquals(1f, s.y); assertEquals(PadLayout.MAX_SIZE, s.size)
        val junk = PadLayout.parse("opacity 0\nDPAD x y z true\nNOPE 1 1 1 true\n")
        assertEquals(0.15f, junk.opacity)                                         // never fully invisible
        assertEquals(PadLayout.DEFAULT, junk.groups)
    }

    @Test fun museum() {
        val all = DeviceMuseum.ALL
        assertEquals(15, all.size)
        assertEquals(all.sortedBy { it.year }, all)                                // oldest first
        assertEquals("T-Mobile G1", all.first().name)
        assertTrue(all.first().keyboard && all.first().trackball)
        val kitkatPhone = all.first { it.api == 19 }
        assertTrue(DeviceMuseum.matches(kitkatPhone, listOf(19)))
        assertFalse(DeviceMuseum.matches(kitkatPhone, listOf(10, 28)))
    }
}
