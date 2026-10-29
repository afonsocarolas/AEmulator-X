/* Added for AEmulator X, 2026-10-29: boot race. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import java.io.File

/**
 * How fast each firmware boots: every finished boot is kept (files/x-boot-times.txt, "id ms when" per line), so the
 * library can show a leaderboard and the running system can say "new record!". Recovery and charging boots are not
 * counted: they are not the real system starting.
 */
object BootRace {
    data class Entry(val id: String, val ms: Long, val at: Long)
    enum class Outcome { FIRST, RECORD, SLOWER }
    data class Result(val ms: Long, val previousBest: Long?, val outcome: Outcome)
    data class Row(val id: String, val best: Long, val last: Long, val count: Int)

    const val KEEP_PER_FIRMWARE = 50
    /** times outside this are not real boots (a clock change, a boot left waiting for hours) */
    val SANE = 1_000L..30 * 60_000L

    private fun file(ctx: Context) = File(ctx.filesDir, "x-boot-times.txt")

    internal fun parse(text: String): List<Entry> = text.lineSequence().mapNotNull { l ->
        val p = l.trim().split(' ')
        if (p.size != 3) null else {
            val ms = p[1].toLongOrNull(); val at = p[2].toLongOrNull()
            if (ms == null || at == null || ms !in SANE || p[0].isEmpty()) null else Entry(p[0], ms, at)
        }
    }.toList()

    internal fun format(list: List<Entry>) = list.joinToString("") { "${it.id} ${it.ms} ${it.at}\n" }

    /** [list] with [e] added, keeping only the newest [KEEP_PER_FIRMWARE] of each firmware. */
    internal fun added(list: List<Entry>, e: Entry): List<Entry> {
        val mine = (list.filter { it.id == e.id } + e).sortedBy { it.at }.takeLast(KEEP_PER_FIRMWARE)
        return list.filter { it.id != e.id } + mine
    }

    fun outcome(previousBest: Long?, ms: Long): Outcome = when {
        previousBest == null -> Outcome.FIRST
        ms < previousBest -> Outcome.RECORD
        else -> Outcome.SLOWER
    }

    fun read(ctx: Context): List<Entry> = runCatching { parse(file(ctx).readText()) }.getOrDefault(emptyList())

    /** Keeps a finished boot and says how it compares; null when [ms] is not a believable boot time. */
    @Synchronized fun record(ctx: Context, id: String, ms: Long, now: Long = System.currentTimeMillis()): Result? {
        if (ms !in SANE) return null
        val list = read(ctx)
        val best = list.filter { it.id == id }.minOfOrNull { it.ms }
        runCatching { file(ctx).writeText(format(added(list, Entry(id, ms, now)))) }
        return Result(ms, best, outcome(best, ms))
    }

    /**
     * Fastest first. Firmwares that booted before the boot race existed still count with their last known boot time
     * ([older]: id → ms), until they boot again.
     */
    fun leaderboard(entries: List<Entry>, older: Map<String, Long> = emptyMap()): List<Row> {
        val rows = entries.groupBy { it.id }.map { (id, l) -> Row(id, l.minOf { it.ms }, l.maxBy { it.at }.ms, l.size) }
        val seen = rows.map { it.id }.toSet()
        val legacy = older.filter { it.key !in seen && it.value in SANE }.map { Row(it.key, it.value, it.value, 1) }
        return (rows + legacy).sortedBy { it.best }
    }

    /** "23.4 s", or "1 min 05 s" from a minute up. */
    fun seconds(ms: Long): String = if (ms < 60_000) "%.1f s".format(java.util.Locale.US, ms / 1000.0)
        else "%d min %02d s".format(java.util.Locale.US, ms / 60_000, ms / 1000 % 60)
}
