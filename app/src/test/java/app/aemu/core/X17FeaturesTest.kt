package app.aemu.core

import app.aemu.ui.AndroidEras
import app.aemu.ui.CrtLook
import org.junit.Assert.*
import org.junit.Test

class X17FeaturesTest {
    @Test fun shakeNeedsTwoJoltsThenRests() {
        val d = ShakeDetector()
        assertFalse(d.sample(0f, 0f, 9.81f, 0))            // resting
        assertFalse(d.sample(30f, 0f, 9.81f, 100))         // first jolt
        assertFalse(d.sample(30f, 0f, 9.81f, 150))         // same jolt still going
        assertTrue(d.sample(-30f, 0f, 9.81f, 400))         // second jolt: shake
        assertFalse(d.sample(30f, 0f, 9.81f, 900))         // cooling down
        assertFalse(d.sample(30f, 0f, 9.81f, 1100))
        assertFalse(d.sample(30f, 0f, 9.81f, 3000))        // a new first jolt after the rest
        assertTrue(d.sample(30f, 0f, 9.81f, 3300))
        assertFalse(ShakeDetector().sample(12f, 0f, 9.81f, 50))   // a bump is not a jolt
    }

    @Test fun browserKeysAndQueries() {
        assertEquals('a'.code, WebPlayServer.keysym("a"))
        assertEquals(0xff0d, WebPlayServer.keysym("Enter"))
        assertEquals(0xff1b, WebPlayServer.keysym("Escape"))
        assertNull(WebPlayServer.keysym("CapsLock"))
        assertEquals(InputService.KEY_BACK, VncKeys.linux(WebPlayServer.keysym("Escape")!!))
        assertEquals(mapOf("k" to "ab c", "x" to "10"), WebPlayServer.query("k=ab+c&x=10"))
        val t = WebPlayServer.newToken()
        assertEquals(8, t.length)
        assertNotEquals(t, WebPlayServer.newToken())
        assertTrue(WebPlayPage.html("G1", t).contains(t))
    }

    @Test fun androidEras() {
        assertEquals(2008, AndroidEras.year(1)); assertEquals("Android 1.0–1.1", AndroidEras.name(2))
        assertEquals(2010, AndroidEras.year(8)); assertEquals("Froyo", AndroidEras.name(8))
        assertEquals(2013, AndroidEras.year(19)); assertEquals("KitKat", AndroidEras.name(19))
        assertEquals(2019, AndroidEras.year(29)); assertEquals("Android 10", AndroidEras.name(29))
        assertEquals(2026, AndroidEras.year(37)); assertEquals("Android 17", AndroidEras.name(37))
    }

    @Test fun crtPattern() {
        assertEquals(110, CrtLook.pixel(0, CrtLook.LINE_PX - 1) ushr 24)   // the dark scanline row
        assertTrue((CrtLook.pixel(0, 0) ushr 24) < 40)                     // the rest stays almost clear
    }
}
