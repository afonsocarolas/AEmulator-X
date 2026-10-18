/* Added for AEmulator X, 2026-10-18: Time mode (day and night interface). GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import app.aemu.R
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Time mode: the app is light in the day (7:00 to 18:59) and dark at night, following the phone's clock and switching
 * by itself while it is open. The library also greets with a sun or a moon that fits the time.
 */
object TimeMode {
    const val DAY_START = 7
    const val NIGHT_START = 19

    fun isNight(hour: Int): Boolean = hour < DAY_START || hour >= NIGHT_START

    /** The greeting for [hour]: morning 5-11, afternoon 12-16, evening 17-20, night 21-4. */
    fun greeting(hour: Int): Int = when (hour) {
        in 5..11 -> R.string.x_time_morning
        in 12..16 -> R.string.x_time_afternoon
        in 17..20 -> R.string.x_time_evening
        else -> R.string.x_time_night
    }

    /** Sun in the day, moon at night (same hours as the theme). */
    fun icon(hour: Int): String = if (isNight(hour)) "🌙" else "☀️"

    fun hourNow(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
}

/** The current hour, updated every 30 s, so the theme and greeting change on their own at 7:00 and 19:00. */
@Composable
fun currentHour(): Int {
    val h by produceState(TimeMode.hourNow()) {
        while (true) { delay(30_000); value = TimeMode.hourNow() }
    }
    return h
}
