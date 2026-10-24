/* Added for AEmulator X, 2026-10-24: live backgrounds. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import kotlin.math.floor
import kotlin.random.Random

/**
 * The maths of the live library backgrounds; the screen only draws. Everything is a pure function of time ([t],
 * seconds) and a fixed seed, so a scene looks the same after rotation and costs nothing to keep.
 */
object LiveScenes {
    enum class Scene(val id: String, val emoji: String) {
        OFF("off", "🚫"), STARS("stars", "🌌"), SYNTH("synth", "🕹️"), MATRIX("matrix", "💚"), RAIN("rain", "🌧️"), FIREFLIES("fireflies", "✨");
        companion object { fun of(id: String?) = entries.firstOrNull { it.id == id } ?: OFF }
    }

    /** fractional part, always 0..1 */
    fun frac(x: Float) = x - floor(x)

    // ---- starfield: stars fly towards you; each has a direction (x, y in -1..1) and a phase along its trip
    class Star(val x: Float, val y: Float, val phase: Float, val speed: Float)
    fun stars(n: Int = 220, seed: Int = 7): List<Star> { val r = Random(seed)
        return List(n) { Star(r.nextFloat() * 2 - 1, r.nextFloat() * 2 - 1, r.nextFloat(), 0.08f + r.nextFloat() * 0.1f) } }
    /** Depth of a star at [t]: 1 = far away, near 0 = passing by; it starts again far away. */
    fun starDepth(s: Star, t: Float): Float = 1f - frac(s.phase + t * s.speed) * 0.97f

    // ---- synthwave grid: horizontal lines come towards you from the horizon
    /** Where the [i]th grid line is at [t], 0 = horizon, 1 = bottom of the screen (perspective: bunched near the horizon). */
    fun gridLine(i: Int, lines: Int, t: Float): Float { val p = frac((i + t * 0.6f) / lines); return p * p }

    // ---- matrix: columns of falling characters
    class Column(val x: Float, val speed: Float, val offset: Float, val length: Int)
    fun columns(n: Int, seed: Int = 3): List<Column> { val r = Random(seed)
        return List(n) { Column((it + 0.5f) / n, 0.15f + r.nextFloat() * 0.35f, r.nextFloat() * 3f, 8 + r.nextInt(18)) } }
    /** The head of a column at [t], in screen heights (it goes past the bottom, then starts again above the top). */
    fun columnHead(c: Column, t: Float): Float = frac((t * c.speed + c.offset) / 1.6f) * 1.6f - 0.2f
    const val GLYPHS = "ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎ0123456789XAEΛΞ"
    /** Which character sits at row [row] of column [col] at [t] (they flicker now and then). */
    fun glyph(col: Int, row: Int, t: Float): Char = GLYPHS[((col * 31 + row * 17 + (t * 3).toInt() * ((row + col) % 3)) and 0x7fffffff) % GLYPHS.length]

    // ---- rain: streaks falling at a slant
    class Drop(val x: Float, val phase: Float, val speed: Float, val length: Float)
    fun drops(n: Int = 140, seed: Int = 11): List<Drop> { val r = Random(seed)
        return List(n) { Drop(r.nextFloat() * 1.2f - 0.1f, r.nextFloat(), 0.9f + r.nextFloat() * 0.8f, 0.03f + r.nextFloat() * 0.05f) } }
    fun dropY(d: Drop, t: Float): Float = frac(d.phase + t * d.speed) * 1.2f - 0.1f

    // ---- fireflies: wander on slow loops and glow on and off
    class Fly(val x: Float, val y: Float, val rx: Float, val ry: Float, val speed: Float, val phase: Float)
    fun flies(n: Int = 42, seed: Int = 5): List<Fly> { val r = Random(seed)
        return List(n) { Fly(r.nextFloat(), 0.15f + r.nextFloat() * 0.8f, 0.03f + r.nextFloat() * 0.08f, 0.02f + r.nextFloat() * 0.06f,
            0.2f + r.nextFloat() * 0.5f, r.nextFloat() * 6.28f) } }
    /** 0..1 brightness: mostly dim, a soft flash every few seconds. */
    fun glow(f: Fly, t: Float): Float { val p = frac(t * 0.18f * (0.7f + f.speed) + f.phase / 6.28f)
        return if (p < 0.25f) kotlin.math.sin(p / 0.25f * Math.PI).toFloat() else 0.08f }
}
