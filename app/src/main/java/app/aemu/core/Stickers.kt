/* Added for AEmulator X, 2026-10-22: screenshot stickers. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.hypot

/**
 * Stickers on a picture: emojis and words. Positions are fractions of the picture (so they look the same on the small
 * editor preview and the full-size saved picture), [size] is a fraction of the picture's width, [angle] in degrees.
 */
object Stickers {
    data class Sticker(val id: Int, val text: String, val emoji: Boolean, val x: Float = 0.5f, val y: Float = 0.5f,
                       val size: Float = 0.18f, val angle: Float = 0f, val color: Int = 0xFFFFFFFF.toInt())

    const val MIN_SIZE = 0.04f
    const val MAX_SIZE = 0.8f
    const val MAX_TEXT = 40

    val EMOJIS = listOf("😀", "😂", "😍", "😎", "🥳", "😱", "🤯", "😴", "👍", "👏", "💪", "🙌", "❤️", "💜", "🔥", "⭐", "✨", "💯", "💥", "🎉",
        "🏆", "👑", "🎮", "🕹️", "🤖", "📱", "🚀", "⚽", "🍫", "🍭", "🧁", "🍩", "🍦", "🥧", "🌈", "☀️", "🌙", "❄️", "👀", "😈")
    val COLORS = listOf(0xFFFFFFFF, 0xFF000000, 0xFFFFD600, 0xFFFF1744, 0xFF00E676, 0xFF2979FF, 0xFFE040FB, 0xFFFF9100).map { it.toInt() }

    fun clamp(s: Sticker) = s.copy(x = s.x.coerceIn(0f, 1f), y = s.y.coerceIn(0f, 1f), size = s.size.coerceIn(MIN_SIZE, MAX_SIZE),
        angle = ((s.angle % 360f) + 360f) % 360f)

    /** The sticker under a touch at ([x], [y]) (fractions), the top-most one first; null when none is close enough. */
    fun hit(list: List<Sticker>, x: Float, y: Float, aspect: Float): Sticker? = list.asReversed().firstOrNull { s ->
        // distance in picture-width units (y is scaled by the picture's height / width)
        hypot(x - s.x, (y - s.y) * aspect) <= maxOf(s.size * 0.6f, 0.05f) * (if (s.emoji) 1f else 1.2f)
    }

    /** Draws [list] onto [c], a picture of [w] × [h] pixels. */
    fun draw(c: Canvas, w: Int, h: Int, list: List<Sticker>) {
        for (s in list) {
            val px = s.size * w
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = if (s.emoji) px else px * 0.55f
                if (!s.emoji) {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = s.color
                }
            }
            c.save()
            c.translate(s.x * w, s.y * h)
            c.rotate(s.angle)
            val baseline = -(p.ascent() + p.descent()) / 2
            if (!s.emoji) {
                // a dark outline keeps words readable on any screenshot
                val outline = Paint(p).apply {
                    style = Paint.Style.STROKE; strokeWidth = p.textSize * 0.12f; strokeJoin = Paint.Join.ROUND
                    color = if (isLight(s.color)) 0xCC000000.toInt() else 0xCCFFFFFF.toInt()
                }
                c.drawText(s.text, 0f, baseline, outline)
            }
            c.drawText(s.text, 0f, baseline, p)
            c.restore()
        }
    }

    fun isLight(c: Int): Boolean = ((c shr 16 and 0xFF) * 299 + (c shr 8 and 0xFF) * 587 + (c and 0xFF) * 114) / 1000 > 140

    /** A copy of [base] with the stickers on it. */
    fun render(base: Bitmap, list: List<Sticker>): Bitmap {
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        draw(Canvas(out), out.width, out.height, list)
        return out
    }
}
