/* Added for AEmulator X, 2026-10-20: keyboard / controller keys mapped to touches. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import java.io.File
import kotlin.math.sqrt

/**
 * Keys of a keyboard or controller turned into touches on the guest screen, for touch-only games:
 *  - a tap spot: holding the key holds a finger at (x, y);
 *  - one joystick: W A S D (or the arrow keys) hold a finger at the stick's centre pushed towards their direction.
 * Coordinates are guest screen pixels. Kept per firmware in keymap.txt: "tap <keyCode> <x> <y>", "stick <x> <y> <r>".
 */
class KeyMap(val taps: List<Tap> = emptyList(), val stick: Stick? = null) {
    data class Tap(val key: Int, val x: Int, val y: Int)
    data class Stick(val x: Int, val y: Int, val r: Int)

    fun isEmpty() = taps.isEmpty() && stick == null

    /** true when [key] belongs to this map (it must not reach the guest as a key then) */
    fun handles(key: Int): Boolean = taps.any { it.key == key } || (stick != null && key in STICK_KEYS)

    /** The fingers for the keys held now: the stick is finger 9, the tap spots 10, 11, … */
    fun touches(held: Set<Int>): List<InputService.P> {
        val out = ArrayList<InputService.P>()
        stick?.let { s ->
            var dx = 0f; var dy = 0f
            if (held.any { it in UP }) dy -= 1f
            if (held.any { it in DOWN }) dy += 1f
            if (held.any { it in LEFT }) dx -= 1f
            if (held.any { it in RIGHT }) dx += 1f
            if (dx != 0f || dy != 0f) {
                val len = sqrt(dx * dx + dy * dy)
                out.add(InputService.P(9, s.x + (dx / len * s.r).toInt(), s.y + (dy / len * s.r).toInt()))
            }
        }
        taps.forEachIndexed { i, t -> if (t.key in held) out.add(InputService.P(10 + i, t.x, t.y)) }
        return out
    }

    fun withTap(t: Tap) = KeyMap(taps.filter { it.key != t.key } + t, stick)
    fun withStick(s: Stick?) = KeyMap(taps, s)
    /** removes the tap spot or stick nearest to (x, y) within [radius] */
    fun without(x: Int, y: Int, radius: Int): KeyMap {
        fun d(ax: Int, ay: Int) = (ax - x) * (ax - x) + (ay - y) * (ay - y)
        val s = stick?.takeIf { d(it.x, it.y) <= radius * radius }
        val t = taps.filter { d(it.x, it.y) <= radius * radius }.minByOrNull { d(it.x, it.y) }
        return when {
            t != null && (s == null || d(t.x, t.y) <= d(s.x, s.y)) -> KeyMap(taps - t, stick)
            s != null -> KeyMap(taps, null)
            else -> this
        }
    }

    fun format(): String = buildString {
        stick?.let { append("stick ${it.x} ${it.y} ${it.r}\n") }
        taps.forEach { append("tap ${it.key} ${it.x} ${it.y}\n") }
    }

    companion object {
        // KeyEvent codes: W A S D and the arrow keys (also the controller D-pad)
        val UP = setOf(51, 19); val DOWN = setOf(47, 20); val LEFT = setOf(29, 21); val RIGHT = setOf(32, 22)
        val STICK_KEYS = UP + DOWN + LEFT + RIGHT

        fun parse(text: String): KeyMap {
            var stick: Stick? = null
            val taps = ArrayList<Tap>()
            for (l in text.lineSequence()) {
                val p = l.trim().split(' ')
                when (p.firstOrNull()) {
                    "stick" -> if (p.size == 4) runCatching { stick = Stick(p[1].toInt(), p[2].toInt(), p[3].toInt()) }
                    "tap" -> if (p.size == 4) runCatching { taps.add(Tap(p[1].toInt(), p[2].toInt(), p[3].toInt())) }
                }
            }
            return KeyMap(taps, stick)
        }

        fun file(paths: VmPaths) = File(paths.dir, "keymap.txt")
        fun load(paths: VmPaths): KeyMap = runCatching { parse(file(paths).readText()) }.getOrDefault(KeyMap())
        fun save(paths: VmPaths, m: KeyMap) = runCatching { file(paths).writeText(m.format()) }

        /** "W", "SPACE", "BUTTON_A" */
        fun label(key: Int): String = android.view.KeyEvent.keyCodeToString(key).removePrefix("KEYCODE_")
    }
}
