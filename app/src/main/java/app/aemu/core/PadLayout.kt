/* Added for AEmulator X, 2026-10-21: virtual gamepad editor. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import java.io.File

/**
 * Where the on-screen gamepad's groups sit and how big they are, per firmware. Positions are the group's centre as
 * a fraction of the screen (so a layout survives rotation), sizes a factor of the normal size, [opacity] 0..1.
 * Saved as "x-pad.txt" in the firmware's folder: "opacity 0.6" then "<group> <x> <y> <size> <shown>" lines.
 */
data class PadLayout(val groups: Map<Group, Spot> = DEFAULT, val opacity: Float = 1f) {
    enum class Group { DPAD, FACE, MENU, L, R }
    data class Spot(val x: Float, val y: Float, val size: Float = 1f, val shown: Boolean = true)

    fun spot(g: Group): Spot = groups[g] ?: DEFAULT.getValue(g)
    fun with(g: Group, s: Spot) = copy(groups = groups + (g to s.copy(x = s.x.coerceIn(0f, 1f), y = s.y.coerceIn(0f, 1f),
        size = s.size.coerceIn(MIN_SIZE, MAX_SIZE))))

    fun format(): String = "opacity $opacity\n" + Group.entries.joinToString("") { g ->
        val s = spot(g); "${g.name} ${s.x} ${s.y} ${s.size} ${s.shown}\n"
    }

    companion object {
        const val MIN_SIZE = 0.6f
        const val MAX_SIZE = 1.8f
        const val FILE = "x-pad.txt"

        val DEFAULT = mapOf(
            Group.DPAD to Spot(0.16f, 0.78f),
            Group.FACE to Spot(0.84f, 0.78f),
            Group.MENU to Spot(0.5f, 0.93f),
            Group.L to Spot(0.1f, 0.5f, shown = false),
            Group.R to Spot(0.9f, 0.5f, shown = false),
        )

        fun parse(text: String): PadLayout {
            var opacity = 1f
            val m = HashMap<Group, Spot>()
            for (line in text.lines()) {
                val p = line.trim().split(' ')
                if (p.size == 2 && p[0] == "opacity") p[1].toFloatOrNull()?.let { opacity = it.coerceIn(0.15f, 1f) }
                if (p.size != 5) continue
                val g = Group.entries.firstOrNull { it.name == p[0] } ?: continue
                val x = p[1].toFloatOrNull() ?: continue; val y = p[2].toFloatOrNull() ?: continue
                val size = p[3].toFloatOrNull() ?: continue
                m[g] = Spot(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f), size.coerceIn(MIN_SIZE, MAX_SIZE), p[4] == "true")
            }
            return PadLayout(DEFAULT + m, opacity)
        }

        fun load(dir: File): PadLayout = runCatching { parse(File(dir, FILE).readText()) }.getOrDefault(PadLayout())
        fun save(dir: File, l: PadLayout) { runCatching { File(dir, FILE).writeText(l.format()) } }
    }
}
