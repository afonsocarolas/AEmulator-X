/* Added for AEmulator X, 2026-10-19: screen filters (retro CRT, Game Boy, sepia, night, black & white). GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View

/** The filters a running system's screen can wear. */
enum class ScreenFilter(val id: String) {
    NONE("none"), CRT("crt"), GAMEBOY("gameboy"), SEPIA("sepia"), NIGHT("night"), BW("bw");

    companion object {
        fun of(id: String?): ScreenFilter? = entries.firstOrNull { it.id == id }
    }
}

/**
 * A see-through layer over the guest screen. In GPU mode the screen is a hardware layer the app cannot recolour, so
 * the filters are coloured glass over it: CRT scanlines and dark corners, Game Boy green with an LCD dot grid, sepia,
 * a warm night tint. Black & white converts the pixels themselves ([bwPaint] on the screen's container), which only
 * works when the screen is drawn by the app (Phone GPU off). It never takes touches.
 */
class CrtOverlayView(ctx: Context) : View(ctx) {
    var filter: ScreenFilter = ScreenFilter.NONE
        set(v) { field = v; visibility = if (v == ScreenFilter.NONE || v == ScreenFilter.BW) GONE else VISIBLE; invalidate() }

    private val lines = Paint()
    private val lcd = Paint()
    private val vignette = Paint()
    private val tint = Paint()

    init {
        isClickable = false; isFocusable = false
        setWillNotDraw(false)
        lines.shader = tile(3, CrtLook.LINE_PX) { x, y -> CrtLook.pixel(x, y) }
        lcd.shader = tile(3, 3) { x, y -> CrtLook.lcdPixel(x, y) }
        visibility = GONE
    }

    private fun tile(w: Int, h: Int, px: (Int, Int) -> Int): BitmapShader {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (y in 0 until h) for (x in 0 until w) b.setPixel(x, y, px(x, y))
        return BitmapShader(b, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val r = maxOf(w, h) * 0.75f
        vignette.shader = RadialGradient(w / 2f, h / 2f, r, intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(150, 0, 0, 0)),
            floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        when (filter) {
            ScreenFilter.CRT -> { c.drawRect(0f, 0f, w, h, lines); c.drawRect(0f, 0f, w, h, vignette) }
            ScreenFilter.GAMEBOY -> { tint.color = CrtLook.GAMEBOY; c.drawRect(0f, 0f, w, h, tint); c.drawRect(0f, 0f, w, h, lcd) }
            ScreenFilter.SEPIA -> { tint.color = CrtLook.SEPIA; c.drawRect(0f, 0f, w, h, tint); c.drawRect(0f, 0f, w, h, vignette) }
            ScreenFilter.NIGHT -> { tint.color = CrtLook.NIGHT; c.drawRect(0f, 0f, w, h, tint) }
            else -> {}
        }
    }

    companion object {
        /** Grey-scale paint for the screen's container (black & white with Phone GPU off). */
        fun bwPaint() = Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) }
    }
}

/** Colours and repeating patterns of the filters (plain ints, so they can be checked off-device). */
object CrtLook {
    const val LINE_PX = 3
    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

    val GAMEBOY = argb(110, 120, 160, 20)
    val SEPIA = argb(85, 112, 66, 20)
    val NIGHT = argb(70, 255, 140, 30)

    fun pixel(x: Int, y: Int): Int {
        if (y == LINE_PX - 1) return argb(110, 0, 0, 0)                // the scanline
        return when (x % 3) {                                          // shadow mask: a hint of each primary
            0 -> argb(18, 255, 0, 0)
            1 -> argb(18, 0, 255, 0)
            else -> argb(18, 0, 0, 255)
        }
    }

    /** Game Boy LCD: a thin dark-green gap between dots */
    fun lcdPixel(x: Int, y: Int): Int = if (x == 2 || y == 2) argb(60, 15, 56, 15) else 0
}
