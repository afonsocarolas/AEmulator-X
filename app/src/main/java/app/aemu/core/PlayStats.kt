/* Added for AEmulator X, 2026-10-16: play time per firmware. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import java.io.File

/**
 * How long each firmware has been on screen, and when it was last played. Kept as a small text file in the image's
 * folder ("<total ms> <last played epoch ms>"), not in SharedPreferences: the VM screen runs in its own process and
 * the library reads the value in the main one.
 */
object PlayStats {
    data class Stats(val totalMs: Long = 0, val lastPlayedAt: Long = 0)

    private const val NAME = "playstats.txt"
    private fun file(ctx: Context, id: String) = File(VmPaths(ctx, id).dir, NAME)

    fun read(ctx: Context, id: String): Stats = runCatching { parse(file(ctx, id).readText()) }.getOrDefault(Stats())

    /** Adds [ms] of play ending now. Ignores spans under a second (screen flicker, rotation). */
    @Synchronized fun add(ctx: Context, id: String, ms: Long, now: Long = System.currentTimeMillis()) {
        if (ms < 1000) return
        val f = file(ctx, id)
        if (!f.parentFile.isDirectory) return
        val old = runCatching { parse(f.readText()) }.getOrDefault(Stats())
        val tmp = File(f.path + ".tmp")
        runCatching { tmp.writeText(format(Stats(old.totalMs + ms, now))); tmp.renameTo(f) }
    }

    internal fun parse(text: String): Stats {
        val p = text.trim().split(Regex("\\s+"))
        return Stats(p.getOrNull(0)?.toLongOrNull()?.coerceAtLeast(0) ?: 0, p.getOrNull(1)?.toLongOrNull()?.coerceAtLeast(0) ?: 0)
    }

    internal fun format(s: Stats) = "${s.totalMs} ${s.lastPlayedAt}\n"

    /** "45 s", "12 min", "3 h 05 min" */
    fun duration(ms: Long): String {
        val m = ms / 60_000
        return when {
            ms < 60_000 -> "${ms / 1000} s"
            m < 60 -> "$m min"
            else -> "%d h %02d min".format(m / 60, m % 60)
        }
    }
}
