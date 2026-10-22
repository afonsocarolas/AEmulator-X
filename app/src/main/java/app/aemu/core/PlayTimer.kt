/* Added for AEmulator X, 2026-10-22: play timer. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

/** A gentle play timer: a few choices, a heads-up near the end, and a "time's up" (it never closes anything). */
object PlayTimer {
    val CHOICES = listOf(15, 30, 45, 60, 90)                  // minutes
    const val WARN_MS = 5 * 60_000L
    const val SNOOZE_MS = 5 * 60_000L

    enum class Stage { OFF, RUNNING, WARN, UP }

    /** Where a timer ending at [endAt] is at [now] (0 = no timer). */
    fun stage(endAt: Long, now: Long): Stage = when {
        endAt <= 0L -> Stage.OFF
        now >= endAt -> Stage.UP
        endAt - now <= WARN_MS -> Stage.WARN
        else -> Stage.RUNNING
    }

    /** "mm:ss" left, or "h:mm:ss" for an hour and more. */
    fun left(endAt: Long, now: Long): String {
        val s = ((endAt - now).coerceAtLeast(0) + 999) / 1000
        val h = s / 3600; val m = s / 60 % 60; val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }
}
