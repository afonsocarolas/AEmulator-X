package app.aemu.ui

import app.aemu.R
import org.junit.Assert.*
import org.junit.Test

class TimeModeTest {
    @Test fun dayAndNightHours() {
        assertTrue(TimeMode.isNight(0)); assertTrue(TimeMode.isNight(6))
        assertFalse(TimeMode.isNight(7)); assertFalse(TimeMode.isNight(13)); assertFalse(TimeMode.isNight(18))
        assertTrue(TimeMode.isNight(19)); assertTrue(TimeMode.isNight(23))
        assertEquals("☀️", TimeMode.icon(15)); assertEquals("🌙", TimeMode.icon(22))
    }
    @Test fun greetings() {
        assertEquals(R.string.x_time_morning, TimeMode.greeting(8))
        assertEquals(R.string.x_time_afternoon, TimeMode.greeting(15))
        assertEquals(R.string.x_time_evening, TimeMode.greeting(19))
        assertEquals(R.string.x_time_night, TimeMode.greeting(2))
    }
}
