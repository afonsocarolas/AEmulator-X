/* Added for AEmulator X, 2026-10-22: boot logo creator. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Your own boot animation: a word (your name, a nickname…), a small line under it, three colours, a background, an
 * entrance and, if you like, your own picture above the word. Made into a bootanimation.zip like AEmulator X's own
 * ([BootAnimation.generate]): part0 plays the entrance once, part1 loops a soft glow until the system has booted.
 */
object BootLogoMaker {
    enum class Entrance { FADE, ZOOM, SLIDE, TYPE }
    enum class Background { BLACK, NIGHT, GRADIENT }

    data class Spec(
        val text: String = "AEmulator",
        val subtitle: String = "",
        val colors: IntArray = intArrayOf(0xFF7C4DFF.toInt(), 0xFFE040FB.toInt(), 0xFF18FFFF.toInt()),
        val background: Background = Background.BLACK,
        val entrance: Entrance = Entrance.ZOOM,
        val picture: Bitmap? = null,
    ) {
        override fun equals(other: Any?) = other is Spec && text == other.text && subtitle == other.subtitle &&
            colors.contentEquals(other.colors) && background == other.background && entrance == other.entrance && picture === other.picture
        override fun hashCode() = listOf(text, subtitle, colors.toList(), background, entrance).hashCode()
    }

    const val FPS = 24
    const val INTRO = 30
    const val LOOP = 30
    const val MAX_TEXT = 14
    const val MAX_SUB = 28

    /** Colour sets offered in the creator (the first is AEmulator X's own). */
    val PALETTES: List<IntArray> = listOf(
        intArrayOf(0xFF7C4DFF.toInt(), 0xFFE040FB.toInt(), 0xFF18FFFF.toInt()),
        intArrayOf(0xFF1A49FF.toInt(), 0xFF00B8D4.toInt(), 0xFF64FFDA.toInt()),
        intArrayOf(0xFFFF1744.toInt(), 0xFFFF6D00.toInt(), 0xFFFFD600.toInt()),
        intArrayOf(0xFF00C853.toInt(), 0xFF76FF03.toInt(), 0xFFB9F6CA.toInt()),
        intArrayOf(0xFFF50057.toInt(), 0xFFFF6E6E.toInt(), 0xFFFFCCBC.toInt()),
        intArrayOf(0xFFFFFFFF.toInt(), 0xFFB0BEC5.toInt(), 0xFFFFFFFF.toInt()),
        intArrayOf(0xFF33B5E5.toInt(), 0xFF0099CC.toInt(), 0xFF8AD5F0.toInt()),
    )

    /** 0..1 eased (fast start, soft landing). */
    fun ease(t: Float): Float { val x = t.coerceIn(0f, 1f); return 1 - (1 - x) * (1 - x) * (1 - x) }

    /** How many letters a typewriter entrance shows at [t] (0..1); the last 15 % keeps the whole word. */
    fun typed(text: String, t: Float): Int = (text.length * (t / 0.85f)).toInt().coerceIn(0, text.length)

    /** desc.txt of the animation: a [w]×[h] picture, the entrance once, then the loop until boot completes. */
    fun desc(w: Int, h: Int) = "$w $h $FPS\np 1 0 part0\np 0 0 part1\n"

    /** Size of the animation for a [screenW]×[screenH] guest screen: as wide as the screen, a bit taller than wide. */
    fun size(screenW: Int, screenH: Int): Pair<Int, Int> {
        val w = minOf(screenW, screenH).coerceIn(160, 720)
        return w to (w * 1.2f).toInt()
    }

    /** One frame: [t] 0..1 through the entrance (1 = done), [glow] 0..1 for the loop's breathing. */
    fun frame(spec: Spec, w: Int, h: Int, t: Float, glow: Float): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val (a, b, cc) = Triple(spec.colors[0], spec.colors[1], spec.colors[2])
        when (spec.background) {
            Background.BLACK -> c.drawColor(0xFF000000.toInt())
            Background.NIGHT -> c.drawPaint(Paint().apply {
                shader = RadialGradient(w / 2f, h * 0.45f, maxOf(w, h) * 0.75f, intArrayOf(0xFF1A1440.toInt(), 0xFF000000.toInt()), null, Shader.TileMode.CLAMP)
            })
            Background.GRADIENT -> {
                c.drawColor(0xFF000000.toInt())
                c.drawPaint(Paint().apply {
                    shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), intArrayOf(dim(a), dim(b), dim(cc)), null, Shader.TileMode.CLAMP)
                })
            }
        }
        val e = ease(t)
        val alpha = (255 * when (spec.entrance) { Entrance.TYPE -> 1f; else -> e }).toInt().coerceIn(0, 255)
        val scale = if (spec.entrance == Entrance.ZOOM) 0.55f + 0.45f * e else 1f
        val slide = if (spec.entrance == Entrance.SLIDE) (1 - e) * h * 0.25f else 0f
        val cx = w / 2f
        val hasPic = spec.picture != null
        val textY = if (hasPic) h * 0.68f else h * 0.55f
        // the picture: a rounded square above the word
        spec.picture?.let { pic ->
            val side = w * 0.42f * scale
            val dst = RectF(cx - side / 2, h * 0.36f - side / 2 + slide, cx + side / 2, h * 0.36f + side / 2 + slide)
            val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.alpha = alpha }
            val save = c.save()
            val clip = android.graphics.Path().apply { addRoundRect(dst, side * 0.18f, side * 0.18f, android.graphics.Path.Direction.CW) }
            c.clipPath(clip)
            val s = minOf(pic.width, pic.height)
            c.drawBitmap(pic, Rect((pic.width - s) / 2, (pic.height - s) / 2, (pic.width + s) / 2, (pic.height + s) / 2), dst, p)
            c.restoreToCount(save)
        }
        // the word: gradient letters with a glow behind them, shrunk to fit
        val shown = if (spec.entrance == Entrance.TYPE) spec.text.take(typed(spec.text, t)) else spec.text
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER
            textSize = w * (if (hasPic) 0.16f else 0.2f) * scale
        }
        val full = paint.measureText(spec.text.ifEmpty { " " })
        if (full > w * 0.9f) paint.textSize *= w * 0.9f / full
        val y = textY + slide
        val glowPaint = Paint(paint).apply {
            color = b; this.alpha = (alpha * (0.2f + 0.5f * glow)).toInt()
            maskFilter = BlurMaskFilter(w * 0.04f, BlurMaskFilter.Blur.NORMAL)
        }
        c.drawText(shown, cx, y, glowPaint)
        paint.shader = LinearGradient(cx - full / 2, 0f, cx + full / 2, 0f, intArrayOf(a, b, cc), null, Shader.TileMode.CLAMP)
        paint.alpha = alpha
        c.drawText(shown, cx, y, paint)
        if (spec.subtitle.isNotBlank()) {
            val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = w * 0.05f; textAlign = Paint.Align.CENTER; color = 0xFFFFFFFF.toInt(); letterSpacing = 0.2f
                this.alpha = (alpha * 0.8f * (if (spec.entrance == Entrance.TYPE) ease(((t - 0.85f) / 0.15f)) else 1f)).toInt()
            }
            c.drawText(spec.subtitle.uppercase(), cx, y + paint.textSize * 0.55f + sub.textSize, sub)
        }
        return bmp
    }

    private fun dim(c: Int): Int {
        val r = (c shr 16 and 0xFF) * 35 / 100; val g = (c shr 8 and 0xFF) * 35 / 100; val bl = (c and 0xFF) * 35 / 100
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }

    /** The bootanimation.zip, entries stored uncompressed (old bootanimation builds cannot read deflated ones). */
    fun zip(spec: Spec, w: Int, h: Int): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(name: String, bytes: ByteArray) {
                val e = ZipEntry(name).apply {
                    method = ZipEntry.STORED; size = bytes.size.toLong(); compressedSize = bytes.size.toLong()
                    crc = CRC32().apply { update(bytes) }.value
                }
                zip.putNextEntry(e); zip.write(bytes); zip.closeEntry()
            }
            fun png(b: Bitmap) = ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.PNG, 100, it); b.recycle() }.toByteArray()
            put("desc.txt", desc(w, h).toByteArray())
            for (i in 0 until INTRO) put("part0/%03d.png".format(i), png(frame(spec, w, h, (i + 1f) / INTRO, 0.5f)))
            for (i in 0 until LOOP) {
                val glow = 0.5f + 0.5f * kotlin.math.sin(2 * Math.PI * i / LOOP).toFloat()
                put("part1/%03d.png".format(i), png(frame(spec, w, h, 1f, glow)))
            }
        }
        return out.toByteArray()
    }
}
