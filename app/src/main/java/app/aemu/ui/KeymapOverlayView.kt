/* Added for AEmulator X, 2026-10-20: key mapping editor over the guest screen. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import app.aemu.core.KeyMap

/**
 * Shown only while the key mapping is edited: the tap spots as labelled circles, the joystick as a ring. A tap asks
 * for a key for that spot ([onPick]), a long press removes the nearest spot ([onRemove]). Positions are converted
 * between this view (the size of the guest screen on the phone) and guest pixels ([guestW] × [guestH]).
 */
@SuppressLint("ViewConstructor")
class KeymapOverlayView(ctx: Context, private val onPick: (Int, Int) -> Unit, private val onRemove: (Int, Int) -> Unit) : View(ctx) {
    var map: KeyMap = KeyMap()
        set(v) { field = v; invalidate() }
    var guestW = 1
    var guestH = 1

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 86, 55, 207) }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 4f }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val shade = Paint().apply { color = Color.argb(70, 0, 0, 0) }

    private val gestures = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapUp(e: MotionEvent): Boolean { onPick(gx(e.x), gy(e.y)); return true }
        override fun onLongPress(e: MotionEvent) { onRemove(gx(e.x), gy(e.y)) }
    })

    private fun gx(x: Float) = (x * guestW / width.coerceAtLeast(1)).toInt().coerceIn(0, guestW - 1)
    private fun gy(y: Float) = (y * guestH / height.coerceAtLeast(1)).toInt().coerceIn(0, guestH - 1)
    private fun vx(x: Int) = x.toFloat() * width / guestW
    private fun vy(y: Int) = y.toFloat() * height / guestH

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean = gestures.onTouchEvent(e)

    override fun onDraw(c: Canvas) {
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shade)
        val r = 26f * resources.displayMetrics.density
        text.textSize = 12f * resources.displayMetrics.scaledDensity
        map.stick?.let { s ->
            val sr = s.r.toFloat() * width / guestW
            c.drawCircle(vx(s.x), vy(s.y), sr, ring)
            c.drawCircle(vx(s.x), vy(s.y), r * 0.6f, fill)
            c.drawText("WASD", vx(s.x), vy(s.y) - sr - 8f, text)
        }
        for (t in map.taps) {
            c.drawCircle(vx(t.x), vy(t.y), r, fill)
            c.drawCircle(vx(t.x), vy(t.y), r, ring)
            c.drawText(KeyMap.label(t.key).take(6), vx(t.x), vy(t.y) + text.textSize / 3, text)
        }
    }
}
