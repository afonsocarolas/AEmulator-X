/* Added for AEmulator X, 2026-10-19: achievements. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import app.aemu.R
import java.io.File

/**
 * Badges for what someone has done with AEmulator X. They are worked out from what is already known (the library,
 * boot counts, play time) plus a few counters the running system bumps (screenshots, recordings, browser play…).
 * The counters live in a small file, not in SharedPreferences: the running system is another process (:vm).
 */
object Achievements {
    data class Facts(
        val firmwares: Int = 0,
        /** API level of every firmware that has booted at least once */
        val bootedApis: List<Int> = emptyList(),
        val booted64: Boolean = false,
        val playMs: Long = 0,
        val counts: Map<String, Int> = emptyMap(),
    )

    class Badge(val id: String, val emoji: String, val title: Int, val desc: Int, val target: Int, val value: (Facts) -> Int) {
        fun progress(f: Facts): Int = value(f).coerceIn(0, target)
        fun unlocked(f: Facts): Boolean = value(f) >= target
    }

    /** counter keys bumped by the running system */
    const val SCREENSHOT = "screenshot"; const val RECORD = "record"; const val WEB = "web"; const val RETRO = "retro"
    const val GAME = "game"; const val VNC = "vnc"; const val SNAKE = "snake"

    private fun eras(f: Facts) = f.bootedApis.map { app.aemu.ui.AndroidEras.name(it) }.distinct().size

    val ALL: List<Badge> = listOf(
        Badge("first_boot", "🚀", R.string.ach_first_boot, R.string.ach_first_boot_d, 1) { it.bootedApis.size },
        Badge("collector", "📚", R.string.ach_collector, R.string.ach_collector_d, 5) { it.firmwares },
        Badge("museum", "🏛️", R.string.ach_museum, R.string.ach_museum_d, 10) { it.firmwares },
        Badge("ancient", "⌛", R.string.ach_ancient, R.string.ach_ancient_d, 1) { f -> f.bootedApis.count { it <= 4 } },
        Badge("time_traveler", "🕰️", R.string.ach_time_traveler, R.string.ach_time_traveler_d, 2) { f ->
            (if (f.bootedApis.any { it <= 9 }) 1 else 0) + (if (f.bootedApis.any { it >= 29 }) 1 else 0) },
        Badge("eras", "🌈", R.string.ach_eras, R.string.ach_eras_d, 5) { eras(it) },
        Badge("sixty_four", "💪", R.string.ach_64, R.string.ach_64_d, 1) { if (it.booted64) 1 else 0 },
        Badge("marathon", "⏱️", R.string.ach_marathon, R.string.ach_marathon_d, 10) { (it.playMs / 3_600_000L).toInt() },
        Badge("photographer", "📸", R.string.ach_photographer, R.string.ach_photographer_d, 10) { it.counts[SCREENSHOT] ?: 0 },
        Badge("director", "🎬", R.string.ach_director, R.string.ach_director_d, 1) { it.counts[RECORD] ?: 0 },
        Badge("broadcaster", "🌐", R.string.ach_broadcaster, R.string.ach_broadcaster_d, 1) { it.counts[WEB] ?: 0 },
        Badge("retro", "📺", R.string.ach_retro, R.string.ach_retro_d, 1) { it.counts[RETRO] ?: 0 },
        Badge("gamer", "🕹️", R.string.ach_gamer, R.string.ach_gamer_d, 1) { it.counts[GAME] ?: 0 },
        Badge("dessert", "🐍", R.string.ach_dessert, R.string.ach_dessert_d, 1) { it.counts[SNAKE] ?: 0 },
    )

    private fun countsFile(ctx: Context) = File(ctx.filesDir, "x-achievements.counts")
    private fun seenFile(ctx: Context) = File(ctx.filesDir, "x-achievements.seen")

    internal fun parseCounts(text: String): Map<String, Int> = text.lineSequence().mapNotNull { l ->
        val p = l.trim().split(' '); if (p.size == 2) p[1].toIntOrNull()?.let { p[0] to it } else null }.toMap()

    @Synchronized fun bump(ctx: Context, key: String) {
        runCatching {
            val f = countsFile(ctx)
            val m = (if (f.isFile) parseCounts(f.readText()) else emptyMap()).toMutableMap()
            m[key] = (m[key] ?: 0) + 1
            f.writeText(m.entries.joinToString("") { "${it.key} ${it.value}\n" })
        }
        DailyChallenge.bumpToday(ctx, key)
    }

    /** Everything the badges look at, read now. Blocking (reads the library and play time). */
    fun facts(ctx: Context): Facts {
        val imgs = runCatching { ImageStore.list(ctx) }.getOrDefault(emptyList())
        val booted = imgs.filter { it.bootCount > 0 || it.lastBootMs > 0 }
        return Facts(
            firmwares = imgs.count { it.baseId.isEmpty() },
            bootedApis = booted.map { it.api },
            booted64 = booted.any { it.abi.startsWith("arm64") },
            playMs = imgs.sumOf { PlayStats.read(ctx, it.id).totalMs },
            counts = runCatching { parseCounts(countsFile(ctx).readText()) }.getOrDefault(emptyMap()),
        )
    }

    /** Badges unlocked since the last call (each reported once); the first call ever reports nothing, only remembers. */
    @Synchronized fun newlyUnlocked(ctx: Context, f: Facts = facts(ctx)): List<Badge> {
        val sf = seenFile(ctx)
        val first = !sf.isFile
        val seen = if (first) emptySet() else sf.readLines().map { it.trim() }.toSet()
        val now = ALL.filter { it.unlocked(f) }
        val fresh = now.filter { it.id !in seen }
        if (fresh.isNotEmpty() || first) runCatching { sf.writeText((seen + now.map { it.id }).joinToString("\n")) }
        return if (first) emptyList() else fresh
    }
}
