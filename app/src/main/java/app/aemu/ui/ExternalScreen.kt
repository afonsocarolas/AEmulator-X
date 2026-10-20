/* Added for AEmulator X, 2026-10-20: the running system on a TV or monitor. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.app.Activity
import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.WindowManager

/**
 * A second screen (HDMI, USB-C, wireless display) showing the guest screen big: a Presentation window with one view
 * that draws the latest frame scaled to fit, black around it. The phone keeps its own screen as the controller.
 */
class ExternalScreen(private val activity: Activity) {
    private val dm = activity.getSystemService(DisplayManager::class.java)
    private var presentation: Presentation? = null
    private var view: FrameView? = null

    val showing: Boolean get() = presentation?.isShowing == true

    /** Displays that can show a presentation (not the phone's own screen). */
    fun displays(): List<Display> = runCatching { dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).toList() }.getOrDefault(emptyList())

    /** Opens on the first external display; false when there is none. */
    fun show(): Boolean {
        if (showing) return true
        val d = displays().firstOrNull() ?: return false
        val v = FrameView(activity)
        val p = object : Presentation(activity, d) {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                setContentView(v)
            }
        }
        return runCatching { p.show(); presentation = p; view = v; true }.getOrDefault(false)
    }

    fun dismiss() {
        runCatching { presentation?.dismiss() }
        presentation = null; view = null
    }

    /** Hands over the newest frame (copied, so the caller may reuse its bitmap). Any thread. */
    fun post(frame: Bitmap) { view?.update(frame) }

    fun listen(onChange: () -> Unit): DisplayManager.DisplayListener {
        val l = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(id: Int) = onChange()
            override fun onDisplayRemoved(id: Int) = onChange()
            override fun onDisplayChanged(id: Int) {}
        }
        dm.registerDisplayListener(l, null)
        return l
    }

    fun unlisten(l: DisplayManager.DisplayListener) = runCatching { dm.unregisterDisplayListener(l) }

    /** Draws the latest frame aspect-fit on black. */
    private class FrameView(ctx: Context) : View(ctx) {
        private var frame: Bitmap? = null
        private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        private val dst = Rect()

        @Synchronized fun update(src: Bitmap) {
            val f = frame?.takeIf { it.width == src.width && it.height == src.height }
                ?: Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888).also { frame = it }
            Canvas(f).drawBitmap(src, 0f, 0f, null)
            postInvalidate()
        }

        @Synchronized override fun onDraw(c: Canvas) {
            c.drawColor(Color.BLACK)
            val f = frame ?: return
            val fit = ExternalFit.fit(f.width, f.height, width, height)
            dst.set(fit[0], fit[1], fit[0] + fit[2], fit[1] + fit[3])
            c.drawBitmap(f, null, dst, paint)
        }
    }
}

/** Aspect-fit of a [w]×[h] picture in a [boxW]×[boxH] screen: left, top, width, height. */
object ExternalFit {
    fun fit(w: Int, h: Int, boxW: Int, boxH: Int): IntArray {
        if (w <= 0 || h <= 0 || boxW <= 0 || boxH <= 0) return intArrayOf(0, 0, 0, 0)
        val s = minOf(boxW.toDouble() / w, boxH.toDouble() / h)
        val fw = (w * s).toInt(); val fh = (h * s).toInt()
        return intArrayOf((boxW - fw) / 2, (boxH - fh) / 2, fw, fh)
    }
}
