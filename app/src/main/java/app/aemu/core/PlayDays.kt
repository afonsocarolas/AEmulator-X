/* Added for AEmulator X, 2026-10-19: play time per day, for the statistics screen. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * Minutes played per calendar day, over all firmwares ("2026-10-19 5400000" per line, in files/x-playdays.txt). It
 * starts with X19: earlier play only exists as totals per firmware (PlayStats).
 */
object PlayDays {
    private fun file(ctx: Context) = File(ctx.filesDir, "x-playdays.txt")

    internal fun parse(text: String): Map<LocalDate, Long> = text.lineSequence().mapNotNull { l ->
        val p = l.trim().split(' ')
        if (p.size != 2) null else runCatching { LocalDate.parse(p[0]) to p[1].toLong() }.getOrNull()
    }.toMap()

    @Synchronized fun add(ctx: Context, ms: Long, now: Long = System.currentTimeMillis()) {
        if (ms < 1000) return
        runCatching {
            val f = file(ctx)
            val m = (if (f.isFile) parse(f.readText()) else emptyMap()).toMutableMap()
            val day = java.time.Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
            m[day] = (m[day] ?: 0L) + ms
            f.writeText(m.toSortedMap().entries.joinToString("") { "${it.key} ${it.value}\n" })
        }
    }

    fun read(ctx: Context): Map<LocalDate, Long> = runCatching { parse(file(ctx).readText()) }.getOrDefault(emptyMap())

    /** Days in a row with play, ending today (or yesterday, if today has none yet). */
    fun currentStreak(days: Map<LocalDate, Long>, today: LocalDate = LocalDate.now()): Int {
        var d = if ((days[today] ?: 0) > 0) today else today.minusDays(1)
        var n = 0
        while ((days[d] ?: 0) > 0) { n++; d = d.minusDays(1) }
        return n
    }

    fun bestStreak(days: Map<LocalDate, Long>): Int {
        var best = 0; var run = 0; var prev: LocalDate? = null
        for (d in days.filterValues { it > 0 }.keys.sorted()) {
            run = if (prev != null && d == prev.plusDays(1)) run + 1 else 1
            best = maxOf(best, run); prev = d
        }
        return best
    }

    /** The last 7 days, oldest first, with their play time (0 for none). */
    fun lastWeek(days: Map<LocalDate, Long>, today: LocalDate = LocalDate.now()): List<Pair<LocalDate, Long>> =
        (6 downTo 0).map { today.minusDays(it.toLong()) }.map { it to (days[it] ?: 0L) }
}
