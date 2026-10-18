package app.aemu.ui

import org.junit.Assert.*
import org.junit.Test

class SleepModeTest {
    @Test fun soundFadesOverTheLastTwoMinutes() {
        assertEquals(1f, SleepTimer.volume(-1f), 0f)       // no timer: full volume
        assertEquals(1f, SleepTimer.volume(600f), 0f)
        assertEquals(1f, SleepTimer.volume(120f), 0f)
        assertEquals(0.5f, SleepTimer.volume(60f), 0.001f)
        assertEquals(0f, SleepTimer.volume(0f), 0f)
    }
    @Test fun breathing478() {
        assertEquals(19, SleepBreathing.CYCLE)
        assertEquals(0, SleepBreathing.at(1f).first)
        assertEquals(1, SleepBreathing.at(5f).first)
        assertEquals(1f, SleepBreathing.at(10.9f).second, 0f)
        assertEquals(2, SleepBreathing.at(12f).first)
        assertEquals(0f, SleepBreathing.at(0f).second, 0.001f)
        assertEquals(0, SleepBreathing.at(19.5f).first)    // the next cycle
    }
}
