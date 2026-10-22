package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class X28FeaturesTest {
    @Test fun timerStages() {
        val end = 1_000_000L
        assertEquals(PlayTimer.Stage.OFF, PlayTimer.stage(0L, 5))
        assertEquals(PlayTimer.Stage.RUNNING, PlayTimer.stage(end, end - 6 * 60_000L))
        assertEquals(PlayTimer.Stage.WARN, PlayTimer.stage(end, end - 5 * 60_000L))
        assertEquals(PlayTimer.Stage.WARN, PlayTimer.stage(end, end - 1))
        assertEquals(PlayTimer.Stage.UP, PlayTimer.stage(end, end))
    }

    @Test fun timerText() {
        assertEquals("30:00", PlayTimer.left(30 * 60_000L, 0))
        assertEquals("0:01", PlayTimer.left(500, 0))                                 // rounds up: never "0:00" while running
        assertEquals("1:30:00", PlayTimer.left(90 * 60_000L, 0))
        assertEquals("0:00", PlayTimer.left(0, 10))
        assertTrue(PlayTimer.CHOICES.all { it > 0 } && PlayTimer.CHOICES == PlayTimer.CHOICES.sorted())
    }
}
