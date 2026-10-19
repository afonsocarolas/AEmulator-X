/* Added for AEmulator X, 2026-10-19: performance overlay numbers. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import java.io.File

/**
 * What the performance overlay shows: frames per second of the guest screen, and the CPU and memory used by
 * AEmulator X with everything it runs (the emulated system's processes run under the app's own uid, so they are
 * the ones /proc lets the app see).
 */
object PerfStats {
    data class Sample(val fps: Int, val cpuPercent: Int, val ramMb: Int)

    /** utime + stime (clock ticks) from a /proc/<pid>/stat line; the name in parentheses may contain spaces. */
    fun ticksOf(stat: String): Long {
        val rest = stat.substringAfterLast(')').trim().split(' ')
        // after ")": state(0) ppid(1) ... utime(11) stime(12)
        return (rest.getOrNull(11)?.toLongOrNull() ?: 0L) + (rest.getOrNull(12)?.toLongOrNull() ?: 0L)
    }

    /** VmRSS in kB from a /proc/<pid>/status text, 0 when absent (zombies, kernel threads). */
    fun rssKbOf(status: String): Long =
        status.lineSequence().firstOrNull { it.startsWith("VmRSS:") }?.split(Regex("\\s+"))?.getOrNull(1)?.toLongOrNull() ?: 0L

    /** Share of the whole phone: [ticks] used in [ms] on [cores] cores at [hz] ticks per second, 0..100. */
    fun cpuPercent(ticks: Long, ms: Long, cores: Int, hz: Int = 100): Int {
        if (ms <= 0 || cores <= 0) return 0
        return (ticks * 1000.0 * 100.0 / (ms * hz.toDouble() * cores)).toInt().coerceIn(0, 100)
    }

    fun fps(frames: Long, ms: Long): Int = if (ms <= 0 || frames < 0) 0 else ((frames * 1000 + ms / 2) / ms).toInt()

    /** Sum of CPU ticks and resident memory of every process of [uid]. Blocking (reads /proc). */
    fun totals(uid: Int): Pair<Long, Long> {
        var ticks = 0L; var rss = 0L
        File("/proc").listFiles()?.forEach { d ->
            if (!d.name.all { it.isDigit() }) return@forEach
            runCatching {
                val status = File(d, "status").readText()
                val u = status.lineSequence().firstOrNull { it.startsWith("Uid:") }?.split(Regex("\\s+"))?.getOrNull(1)?.toIntOrNull()
                if (u != uid) return@runCatching
                rss += rssKbOf(status)
                ticks += ticksOf(File(d, "stat").readText())
            }
        }
        return ticks to rss
    }

    /** Turns successive readings into per-second numbers. */
    class Meter(private val uid: Int, private val cores: Int = Runtime.getRuntime().availableProcessors()) {
        private var lastAt = 0L
        private var lastFrames = 0L
        private var lastTicks = 0L

        /** [frames]: the guest's frame counter so far. Blocking; null on the first call (nothing to compare yet). */
        fun sample(frames: Long, now: Long = System.currentTimeMillis()): Sample? {
            val (ticks, rssKb) = totals(uid)
            val first = lastAt == 0L
            val ms = now - lastAt
            val s = if (first) null else Sample(fps(frames - lastFrames, ms), cpuPercent(ticks - lastTicks, ms, cores), (rssKb / 1024).toInt())
            lastAt = now; lastFrames = frames; lastTicks = ticks
            return s
        }
    }
}
