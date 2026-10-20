/* Added for AEmulator X, 2026-10-20: gesture shortcuts. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Recognises one drawn stroke: O (Home), < (Back), ^ (Recents), S (Screenshot), ✓ (Instant replay), Z (GIF).
 * A small template matcher in the spirit of the "$1 recognizer": the stroke is resampled to [N] points, centred and
 * scaled to a unit box (keeping its shape, without rotating it, so < and ^ stay different), then compared point by
 * point with each template; the closest one wins when it is close enough.
 */
object GestureShapes {
    data class P(val x: Float, val y: Float)
    class Shape(val symbol: String, val action: VoiceCommands.Action, val points: List<P>)

    private const val N = 48
    /** mean distance (unit box) above which nothing is recognised */
    const val MAX_DISTANCE = 0.2f

    private fun poly(vararg xy: Float) = xy.toList().chunked(2) { P(it[0], it[1]) }
    private fun circle(cw: Boolean) = (0..64).map { i ->
        val a = -PI / 2 + (if (cw) 1 else -1) * 2 * PI * i / 64
        P((0.5 + 0.5 * cos(a)).toFloat(), (0.5 + 0.5 * sin(a)).toFloat())
    }

    val SHAPES: List<Shape> by lazy {
        listOf(
            Shape("O", VoiceCommands.Action.HOME, circle(true)),
            Shape("O", VoiceCommands.Action.HOME, circle(false)),
            Shape("<", VoiceCommands.Action.BACK, poly(1f, 0f, 0f, 0.5f, 1f, 1f)),
            Shape("^", VoiceCommands.Action.RECENTS, poly(0f, 1f, 0.5f, 0f, 1f, 1f)),
            Shape("S", VoiceCommands.Action.SCREENSHOT, poly(0.9f, 0.1f, 0.5f, 0f, 0.1f, 0.15f, 0.15f, 0.4f, 0.5f, 0.5f,
                0.85f, 0.6f, 0.9f, 0.85f, 0.5f, 1f, 0.1f, 0.9f)),
            Shape("✓", VoiceCommands.Action.REPLAY, poly(0f, 0.6f, 0.35f, 1f, 1f, 0f)),
            Shape("Z", VoiceCommands.Action.GIF, poly(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)),
        ).map { Shape(it.symbol, it.action, normalize(it.points)) }
    }

    /** The shape drawn, or null for a dot, a scribble or anything not close to a template. */
    fun recognize(stroke: List<P>): Shape? {
        if (stroke.size < 4) return null
        val minX = stroke.minOf { it.x }; val maxX = stroke.maxOf { it.x }
        val minY = stroke.minOf { it.y }; val maxY = stroke.maxOf { it.y }
        if (maxOf(maxX - minX, maxY - minY) < 1e-3f) return null
        val s = normalize(stroke)
        var best: Shape? = null; var bestD = Float.MAX_VALUE
        for (t in SHAPES) {
            val d = distance(s, t.points)
            if (d < bestD) { bestD = d; best = t }
        }
        return best?.takeIf { bestD <= MAX_DISTANCE }
    }

    internal fun distance(a: List<P>, b: List<P>): Float =
        a.indices.sumOf { hypot((a[it].x - b[it].x).toDouble(), (a[it].y - b[it].y).toDouble()) }.toFloat() / a.size

    internal fun normalize(pts: List<P>): List<P> {
        val r = resample(pts)
        val minX = r.minOf { it.x }; val maxX = r.maxOf { it.x }
        val minY = r.minOf { it.y }; val maxY = r.maxOf { it.y }
        val size = maxOf(maxX - minX, maxY - minY).coerceAtLeast(1e-6f)
        val cx = (minX + maxX) / 2; val cy = (minY + maxY) / 2
        return r.map { P((it.x - cx) / size, (it.y - cy) / size) }
    }

    /** [N] points evenly spaced along the stroke. */
    internal fun resample(pts: List<P>): List<P> {
        val total = pts.zipWithNext { a, b -> hypot(b.x - a.x, b.y - a.y) }.sum()
        if (total <= 0f) return List(N) { pts.first() }
        val step = total / (N - 1)
        val out = ArrayList<P>(N).apply { add(pts.first()) }
        var acc = 0f
        var prev = pts.first()
        var i = 1
        while (i < pts.size && out.size < N) {
            val cur = pts[i]
            val d = hypot(cur.x - prev.x, cur.y - prev.y)
            if (d > 0f && acc + d >= step) {
                val t = (step - acc) / d
                val q = P(prev.x + t * (cur.x - prev.x), prev.y + t * (cur.y - prev.y))
                out.add(q); prev = q; acc = 0f
            } else { acc += d; prev = cur; i++ }
        }
        while (out.size < N) out.add(pts.last())
        return out
    }
}
