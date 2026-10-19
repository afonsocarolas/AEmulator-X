package app.aemu.ui

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class SnowfallTest {
    private fun cal(month: Int) = Calendar.getInstance().apply { set(2026, month, 10) }

    @Test fun whenItSnows() {
        assertTrue(Snowfall.wanted("christmas", decemberSnow = false, cal = cal(Calendar.JULY)))
        assertTrue(Snowfall.wanted("violet", decemberSnow = true, cal = cal(Calendar.DECEMBER)))
        assertFalse(Snowfall.wanted("violet", decemberSnow = false, cal = cal(Calendar.DECEMBER)))
        assertFalse(Snowfall.wanted("violet", decemberSnow = true, cal = cal(Calendar.NOVEMBER)))
    }
    @Test fun flakesLoopWithoutAJump() {
        for (falls in 1..3) {
            val a = Snowfall.position(0.3f, 0.6f, falls, 0.02f, 1f, 0f)
            val b = Snowfall.position(0.3f, 0.6f, falls, 0.02f, 1f, 1f)
            assertEquals(a.x, b.x, 1e-4f); assertEquals(a.y, b.y, 1e-4f)
        }
        val mid = Snowfall.position(0.99f, 0.5f, 2, 0.05f, 0f, 0.37f)
        assertTrue(mid.x in 0f..1f && mid.y in 0f..1f)
    }
}
