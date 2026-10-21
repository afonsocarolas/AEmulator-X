/* Added for AEmulator X, 2026-10-21: daily challenges. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import app.aemu.R
import java.io.File
import java.time.LocalDate

/**
 * One small goal a day ("take 3 screenshots", "play 20 minutes", "boot a KitKat firmware"…), the same all day, and a
 * streak of days with the goal reached. Progress comes from the achievement counters, which also keep a per-day copy
 * (files/x-today.counts, written by [Achievements.bump] from both processes), and from today's play time.
 */
object DailyChallenge {
    /** [key] is a counter (or [PLAY] for minutes played); [group] is the Android version family of era challenges. */
    data class Challenge(val id: String, val emoji: String, val key: String, val target: Int, val group: Int = -1)

    const val PLAY = "play_minutes"
    const val BOOT = "boot"
    const val SNAKE_ROUND = "snake_round"
    fun bootKey(group: Int) = "boot_g$group"

    /** a representative API level of each [DailyPick.factGroup] (for its name) */
    val GROUP_API = intArrayOf(1, 3, 4, 5, 8, 9, 11, 14, 16, 19, 21, 23, 24, 26, 28, 29)

    private val BASIC = listOf(
        Challenge("play15", "⏱️", PLAY, 15),
        Challenge("play30", "⏱️", PLAY, 30),
        Challenge("shots3", "📸", Achievements.SCREENSHOT, 3),
        Challenge("record", "🎬", Achievements.RECORD, 1),
        Challenge("retro", "📺", Achievements.RETRO, 1),
        Challenge("game", "🕹️", Achievements.GAME, 1),
        Challenge("boot2", "🚀", BOOT, 2),
        Challenge("snake", "🐍", SNAKE_ROUND, 1),
    )

    /** Everything possible with this library: the basic ones, plus booting each Android version family you have. */
    fun candidates(apis: Collection<Int>): List<Challenge> =
        BASIC + apis.map { DailyPick.factGroup(it) }.distinct().sorted().map { Challenge("era$it", "🕰️", bootKey(it), 1, it) }

    /** Today's challenge, or null with an empty library. Era ones are picked a little more often (they're the fun ones). */
    fun pick(day: Long, apis: Collection<Int>): Challenge? {
        if (apis.isEmpty()) return null
        val all = candidates(apis)
        val eras = all.filter { it.group >= 0 }
        val pool = all + eras                                          // eras twice
        return pool[DailyPick.index(pool.size, day)]
    }

    fun progress(c: Challenge, today: Map<String, Int>, playMsToday: Long): Int =
        (if (c.key == PLAY) (playMsToday / 60_000L).toInt() else today[c.key] ?: 0).coerceAtMost(c.target)

    // ---- today's counters

    private fun todayFile(ctx: Context) = File(ctx.filesDir, "x-today.counts")

    /** "day N" then "key count" lines; a different day starts from zero. */
    internal fun parseToday(text: String, day: Long): Map<String, Int> {
        val lines = text.lines()
        if (lines.firstOrNull()?.trim() != "day $day") return emptyMap()
        return Achievements.parseCounts(lines.drop(1).joinToString("\n"))
    }

    internal fun bumped(text: String, day: Long, key: String): String {
        val m = parseToday(text, day).toMutableMap()
        m[key] = (m[key] ?: 0) + 1
        return "day $day\n" + m.entries.joinToString("") { "${it.key} ${it.value}\n" }
    }

    @Synchronized fun bumpToday(ctx: Context, key: String, day: Long = DailyPick.today()) {
        runCatching {
            val f = todayFile(ctx)
            f.writeText(bumped(if (f.isFile) f.readText() else "", day, key))
        }
    }

    fun today(ctx: Context, day: Long = DailyPick.today()): Map<String, Int> =
        runCatching { parseToday(todayFile(ctx).readText(), day) }.getOrDefault(emptyMap())

    // ---- streak

    private fun doneFile(ctx: Context) = File(ctx.filesDir, "x-challenge-done.txt")

    fun doneDays(ctx: Context): Set<Long> =
        runCatching { doneFile(ctx).readLines().mapNotNull { it.trim().toLongOrNull() }.toSet() }.getOrDefault(emptySet())

    /** Marks [day] reached; true the first time (to celebrate once). */
    @Synchronized fun markDone(ctx: Context, day: Long): Boolean {
        val done = doneDays(ctx)
        if (day in done) return false
        runCatching { doneFile(ctx).appendText("$day\n") }
        return true
    }

    /** Days in a row with the challenge reached, ending today (or yesterday while today's is still open). */
    fun streak(done: Set<Long>, today: Long): Int {
        var d = if (today in done) today else today - 1
        var n = 0
        while (d in done) { n++; d-- }
        return n
    }

    fun bestStreak(done: Set<Long>): Int {
        var best = 0; var run = 0; var prev = Long.MIN_VALUE
        for (d in done.sorted()) { run = if (d == prev + 1) run + 1 else 1; best = maxOf(best, run); prev = d }
        return best
    }

    fun playMsToday(ctx: Context, day: Long = DailyPick.today()): Long = PlayDays.read(ctx)[LocalDate.ofEpochDay(day)] ?: 0L

    /** The text of a challenge (needs the era name for era ones). */
    fun title(ctx: Context, c: Challenge, eraName: (Int) -> String): String = when {
        c.group >= 0 -> ctx.getString(R.string.x_dc_era, eraName(GROUP_API[c.group]))
        c.key == PLAY -> ctx.getString(R.string.x_dc_play, c.target)
        c.key == Achievements.SCREENSHOT -> ctx.getString(R.string.x_dc_shots, c.target)
        c.key == Achievements.RECORD -> ctx.getString(R.string.x_dc_record)
        c.key == Achievements.RETRO -> ctx.getString(R.string.x_dc_retro)
        c.key == Achievements.GAME -> ctx.getString(R.string.x_dc_game)
        c.key == BOOT -> ctx.getString(R.string.x_dc_boot, c.target)
        else -> ctx.getString(R.string.x_dc_snake)
    }
}
