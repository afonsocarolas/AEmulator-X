package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class X18FeaturesTest {
    private fun badge(id: String) = Achievements.ALL.first { it.id == id }

    @Test fun badgesFollowTheFacts() {
        val none = Achievements.Facts()
        assertTrue(Achievements.ALL.none { it.unlocked(none) })
        val f = Achievements.Facts(firmwares = 6, bootedApis = listOf(2, 8, 19, 29, 23), booted64 = true,
            playMs = 3 * 3_600_000L, counts = mapOf(Achievements.SCREENSHOT to 4, Achievements.WEB to 1))
        assertTrue(badge("first_boot").unlocked(f))
        assertTrue(badge("collector").unlocked(f))
        assertFalse(badge("museum").unlocked(f)); assertEquals(6, badge("museum").progress(f))
        assertTrue(badge("ancient").unlocked(f))
        assertTrue(badge("time_traveler").unlocked(f))
        assertTrue(badge("eras").unlocked(f))            // 1.0–1.1, Froyo, KitKat, Android 10, Marshmallow
        assertTrue(badge("sixty_four").unlocked(f))
        assertFalse(badge("marathon").unlocked(f)); assertEquals(3, badge("marathon").progress(f))
        assertFalse(badge("photographer").unlocked(f))
        assertTrue(badge("broadcaster").unlocked(f))
        assertEquals(Achievements.ALL.size, Achievements.ALL.map { it.id }.toSet().size)
    }

    @Test fun counterFile() {
        assertEquals(mapOf("web" to 2, "screenshot" to 11), Achievements.parseCounts("web 2\nscreenshot 11\nbroken\n"))
    }

    @Test fun qrMatrix() {
        val m = QrCode.matrix("http://192.168.1.5:8080/?k=ab3k9xyz", 200)
        assertEquals(200, m.size)
        assertTrue(m.sumOf { row -> row.count { it } } > 1000)   // plenty of dark modules
        assertFalse(m[0][0])                                     // quiet zone in the corner
    }
}
