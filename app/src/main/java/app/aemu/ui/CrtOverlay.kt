/* Added for AEmulator X, 2026-10-19: retro CRT screen filter. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View

/**
 * A see-through layer over the guest screen that makes it look like an old tube display: thin dark scanlines (one
 * every [CrtLook.LINE_PX] screen pixels), a faint RGB shadow mask and darker, rounded-off corners. It never takes
 * touches, so the screen under it works as before.
 */
class CrtOverlayView(ctx: Context) : View(ctx) {
    private val lines = Paint()
    private val vignette = Paint()

    init {
        isClickable = false; isFocusable = false
        setWillNotDraw(false)
        val tile = Bitmap.createBitmap(3, CrtLook.LINE_PX, Bitmap.Config.ARGB_8888)
        for (y in 0 until CrtLook.LINE_PX) for (x in 0 until 3) tile.setPixel(x, y, CrtLook.pixel(x, y))
        lines.shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val r = maxOf(w, h) * 0.75f
        vignette.shader = RadialGradient(w / 2f, h / 2f, r, intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(150, 0, 0, 0)),
            floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), lines)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignette)
    }
}

/** The repeating 3×LINE_PX pattern: a dark line at the bottom row, and a faint red/green/blue column mask. */
object CrtLook {
    const val LINE_PX = 3

    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

    fun pixel(x: Int, y: Int): Int {
        if (y == LINE_PX - 1) return argb(110, 0, 0, 0)                // the scanline
        return when (x % 3) {                                          // shadow mask: a hint of each primary
            0 -> argb(18, 255, 0, 0)
            1 -> argb(18, 0, 255, 0)
            else -> argb(18, 0, 0, 255)
        }
    }
}
