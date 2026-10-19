/* Added for AEmulator X, 2026-10-19: record and replay touches. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import java.io.File

/**
 * A recorded sequence of touches, replayed with the same timing. Each step is the time since the start and the
 * fingers down at that moment (none = all lifted). Kept per firmware in macro.txt:
 *   "<ms> <id>:<x>:<y> <id>:<x>:<y>"   (a line with only the time = release)
 */
class TouchMacro(val steps: List<Step>) {
    data class Step(val atMs: Long, val points: List<InputService.P>)

    val durationMs: Long get() = steps.lastOrNull()?.atMs ?: 0L
    fun isEmpty() = steps.isEmpty()

    fun format(): String = steps.joinToString("") { s ->
        s.atMs.toString() + s.points.joinToString("") { " ${it.id}:${it.x}:${it.y}" } + "\n"
    }

    /** Replays on the calling thread; [stop] is polled between steps. Always ends with every finger lifted. */
    fun play(touch: (List<InputService.P>) -> Unit, stop: () -> Boolean) {
        val start = System.nanoTime() / 1_000_000
        try {
            for (s in steps) {
                while (true) {
                    if (stop()) return
                    val wait = s.atMs - (System.nanoTime() / 1_000_000 - start)
                    if (wait <= 0) break
                    Thread.sleep(minOf(wait, 50))
                }
                touch(s.points)
            }
        } finally { touch(emptyList()) }
    }

    companion object {
        fun parse(text: String): TouchMacro = TouchMacro(text.lineSequence().mapNotNull { l ->
            val p = l.trim().split(' ').filter { it.isNotEmpty() }
            val t = p.firstOrNull()?.toLongOrNull() ?: return@mapNotNull null
            Step(t, p.drop(1).mapNotNull { f ->
                val q = f.split(':'); if (q.size == 3) runCatching { InputService.P(q[0].toInt(), q[1].toInt(), q[2].toInt()) }.getOrNull() else null
            })
        }.toList())

        fun file(paths: VmPaths) = File(paths.dir, "macro.txt")
        fun load(paths: VmPaths): TouchMacro? = runCatching { parse(file(paths).readText()) }.getOrNull()?.takeIf { !it.isEmpty() }
    }

    /** Collects touches as they happen; [finish] gives the macro (trailing release added). */
    class Recorder {
        private val start = System.nanoTime() / 1_000_000
        private val steps = ArrayList<Step>()
        @Synchronized fun add(points: List<InputService.P>) {
            val t = System.nanoTime() / 1_000_000 - start
            if (steps.lastOrNull()?.points == points) return
            steps.add(Step(t, points.toList()))
        }
        @Synchronized fun finish(): TouchMacro {
            if (steps.isNotEmpty() && steps.last().points.isNotEmpty()) steps.add(Step(steps.last().atMs + 50, emptyList()))
            // start the replay at the first touch, not at the moment recording began
            val t0 = steps.firstOrNull()?.atMs ?: 0
            return TouchMacro(steps.map { it.copy(atMs = it.atMs - t0) })
        }
    }
}
