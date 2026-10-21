/* Added for AEmulator X, 2026-10-21: screenshot frames. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

/**
 * Screenshots in a frame: a phone around the screen, or a Polaroid photo with a handwritten caption. The PNG keeps a
 * transparent background, so it looks right on any chat or wallpaper.
 */
object ScreenshotFrame {
    enum class Style(val id: String) { NONE("none"), PHONE("phone"), POLAROID("polaroid");
        companion object { fun of(id: String?) = entries.firstOrNull { it.id == id } ?: NONE }
    }

    /** Size of the framed picture and where the screen goes in it: outW, outH, left, top. */
    fun layout(style: Style, w: Int, h: Int): IntArray {
        val s = minOf(w, h)
        return when (style) {
            Style.NONE -> intArrayOf(w, h, 0, 0)
            Style.PHONE -> {
                val side = (s * 0.06f).toInt(); val top = (s * 0.16f).toInt(); val bottom = (s * 0.16f).toInt()
                val pad = (s * 0.04f).toInt()                           // room for the shadow
                intArrayOf(w + 2 * side + 2 * pad, h + top + bottom + 2 * pad, side + pad, top + pad)
            }
            Style.POLAROID -> {
                val border = (s * 0.06f).toInt(); val bottom = (s * 0.24f).toInt()
                val pad = (s * 0.04f).toInt()
                intArrayOf(w + 2 * border + 2 * pad, h + border + bottom + 2 * pad, border + pad, border + pad)
            }
        }
    }

    /** [src] framed with [frame] (a new bitmap; [src] is left as it is). [caption] is written under a Polaroid. */
    fun apply(src: Bitmap, frame: Style, caption: String = ""): Bitmap {
        if (frame == Style.NONE) return src
        val w = src.width; val h = src.height; val s = minOf(w, h).toFloat()
        val (ow, oh, left, top) = layout(frame, w, h).let { listOf(it[0], it[1], it[2], it[3]) }
        val out = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val pad = s * 0.04f
        val body = RectF(pad, pad, ow - pad, oh - pad)
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 0, 0, 0); maskFilter = BlurMaskFilter(pad * 0.8f, BlurMaskFilter.Blur.NORMAL) }
        val screen = RectF(left.toFloat(), top.toFloat(), (left + w).toFloat(), (top + h).toFloat())
        when (frame) {
            Style.PHONE -> {
                val r = s * 0.1f
                c.drawRoundRect(RectF(body).apply { offset(0f, pad * 0.35f) }, r, r, shadow)
                val metal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(0f, body.top, body.right, body.bottom, Color.rgb(58, 58, 64), Color.rgb(18, 18, 22), Shader.TileMode.CLAMP)
                }
                c.drawRoundRect(body, r, r, metal)
                val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = s * 0.006f; color = Color.argb(70, 255, 255, 255) }
                c.drawRoundRect(RectF(body).apply { inset(rim.strokeWidth, rim.strokeWidth) }, r, r, rim)
                // speaker slot and camera above the screen, home pill below
                val gray = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(70, 70, 78) }
                val cx = ow / 2f; val upper = (body.top + screen.top) / 2
                c.drawRoundRect(RectF(cx - s * 0.08f, upper - s * 0.01f, cx + s * 0.08f, upper + s * 0.01f), s * 0.01f, s * 0.01f, gray)
                c.drawCircle(cx + s * 0.14f, upper, s * 0.015f, gray)
                val lower = (screen.bottom + body.bottom) / 2
                val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = s * 0.008f; color = Color.rgb(90, 90, 98) }
                c.drawRoundRect(RectF(cx - s * 0.06f, lower - s * 0.045f, cx + s * 0.06f, lower + s * 0.045f), s * 0.03f, s * 0.03f, pill)
                c.drawBitmap(src, left.toFloat(), top.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
                c.drawRect(screen, Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.BLACK })
            }
            Style.POLAROID -> {
                c.drawRect(RectF(body).apply { offset(0f, pad * 0.35f) }, shadow)
                c.drawRect(body, Paint().apply { color = Color.rgb(252, 251, 247) })
                c.drawBitmap(src, left.toFloat(), top.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
                c.drawRect(screen, Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.argb(40, 0, 0, 0) })
                if (caption.isNotBlank()) {
                    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(45, 45, 60); textAlign = Paint.Align.CENTER
                        typeface = Typeface.create("casual", Typeface.NORMAL)
                        textSize = s * 0.065f
                    }
                    var n = caption.length
                    var t = caption
                    while (text.measureText(t) > body.width() * 0.9f && n > 4) { n--; t = caption.take(n) + "…" }
                    c.drawText(t, ow / 2f, (screen.bottom + body.bottom) / 2 + text.textSize / 3, text)
                }
            }
            Style.NONE -> {}
        }
        return out
    }
}
