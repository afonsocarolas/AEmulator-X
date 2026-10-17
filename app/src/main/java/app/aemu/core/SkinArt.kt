/* Added for AEmulator X, 2026-10-17: built-in device skins, drawn in code (no photos, no logos). GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.io.File

/**
 * Original illustrations of four phone/tablet shapes of the Android 2.x–4.x era, used as built-in device skins
 * ([VmThemes.BUILT_IN]). Each [Spec] says where the screen and the drawn hardware keys are, as fractions of the
 * picture (the same format as a user's own skin). The pictures are drawn once into the themes folder.
 */
object SkinArt {
    const val PREFIX = "skin-"

    class Spec(val id: String, val w: Int, val h: Int, val screen: SkinRect, val keys: List<SkinKey>, val draw: (Canvas, Spec) -> Unit) {
        val file get() = "$PREFIX$id.img"
        fun px(r: SkinRect) = RectF(r.l * w, r.t * h, r.r * w, r.b * h)
    }

    private fun r(l: Float, t: Float, rr: Float, b: Float) = SkinRect(l, t, rr, b)

    val PEBBLE = Spec("pebble", 1000, 2000, r(0.09f, 0.145f, 0.91f, 0.86f), listOf(
        SkinKey(NavButton.MENU, r(0.10f, 0.885f, 0.30f, 0.955f)),
        SkinKey(NavButton.HOME, r(0.36f, 0.885f, 0.64f, 0.955f)),
        SkinKey(NavButton.BACK, r(0.70f, 0.885f, 0.90f, 0.955f)),
    )) { c, s -> pebble(c, s) }

    val SLATE = Spec("slate", 1000, 2000, r(0.07f, 0.11f, 0.93f, 0.885f), emptyList()) { c, s -> slate(c, s) }

    val SLIDER = Spec("slider", 1300, 1260, r(150f / 1300, 70f / 1260, 945f / 1300, 600f / 1260), listOf(
        SkinKey(NavButton.HOME, r(1040f / 1300, 90f / 1260, 1220f / 1300, 190f / 1260)),
        SkinKey(NavButton.MENU, r(1040f / 1300, 200f / 1260, 1220f / 1300, 300f / 1260)),
        SkinKey(NavButton.CENTER, r(1080f / 1300, 310f / 1260, 1180f / 1300, 410f / 1260)),
        SkinKey(NavButton.BACK, r(1040f / 1300, 420f / 1260, 1220f / 1300, 520f / 1260)),
    )) { c, s -> slider(c, s) }

    val TABLET = Spec("tablet", 1600, 1250, r(110f / 1600, 95f / 1250, 1490f / 1600, 1130f / 1250), emptyList()) { c, s -> tablet(c, s) }

    val ALL = listOf(PEBBLE, SLATE, SLIDER, TABLET)
    fun byFile(name: String?) = ALL.firstOrNull { it.file == name }

    /** Draws [spec]'s picture into [dir] if it is not there yet; returns the file. */
    fun ensure(dir: File, spec: Spec): File? {
        val f = File(dir, spec.file)
        if (f.isFile && f.length() > 0) return f
        return runCatching {
            val bmp = Bitmap.createBitmap(spec.w, spec.h, Bitmap.Config.ARGB_8888)
            spec.draw(Canvas(bmp), spec)
            val tmp = File(dir, spec.file + ".tmp")
            tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
            tmp.renameTo(f); f
        }.getOrNull()
    }

    // ------------------------------------------------------------------ drawing helpers

    private fun paint(color: Long) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toInt() }
    private fun Canvas.screen(s: Spec, corner: Float = 6f) {
        val r = s.px(s.screen)
        drawRoundRect(RectF(r.left - 3, r.top - 3, r.right + 3, r.bottom + 3), corner + 3, corner + 3, paint(0xFF050505))
        drawRoundRect(r, corner, corner, paint(0xFF000000))
    }
    private fun Canvas.glassSheen(rect: RectF, radius: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(rect.left, rect.top, rect.right, rect.top + rect.height() * 0.45f,
                0x26FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        }
        drawRoundRect(rect, radius, radius, p)
    }

    /** White rounded "pebble" phone with an earpiece, two sensors, a camera and a physical home button. */
    private fun pebble(c: Canvas, s: Spec) {
        val body = RectF(20f, 20f, s.w - 20f, s.h - 20f)
        c.drawRoundRect(RectF(body.left - 8, body.top - 8, body.right + 8, body.bottom + 8), 190f, 190f, paint(0xFF9E9E9E))
        c.drawRoundRect(body, 180f, 180f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s.h.toFloat(), 0xFFFDFDFD.toInt(), 0xFFE2E2E2.toInt(), Shader.TileMode.CLAMP)
        })
        c.glassSheen(body, 180f)
        c.drawRoundRect(RectF(370f, 140f, 630f, 162f), 11f, 11f, paint(0xFFB0B0B0))       // earpiece
        c.drawCircle(700f, 151f, 13f, paint(0xFF707070)); c.drawCircle(745f, 151f, 13f, paint(0xFF707070))
        c.drawCircle(820f, 151f, 22f, paint(0xFF303030)); c.drawCircle(820f, 151f, 9f, paint(0xFF4A5A70))
        c.screen(s)
        val home = s.px(s.keys[1].area)
        c.drawRoundRect(RectF(home.left + 10, home.top + 22, home.right - 10, home.bottom - 22), 60f, 60f, paint(0xFFD0D0D0))
        c.drawRoundRect(RectF(home.left + 18, home.top + 30, home.right - 18, home.bottom - 30), 52f, 52f, paint(0xFFF2F2F2))
        // capacitive Menu and Back, faint until lit
        val g = paint(0xFFBDBDBD).apply { style = Paint.Style.STROKE; strokeWidth = 6f; strokeCap = Paint.Cap.ROUND }
        val m = s.px(s.keys[0].area); val cy = (m.top + m.bottom) / 2
        c.drawRoundRect(RectF(m.centerX() - 34, cy - 24, m.centerX() + 34, cy + 24), 6f, 6f, g)
        c.drawLine(m.centerX() - 20, cy - 6, m.centerX() + 20, cy - 6, g); c.drawLine(m.centerX() - 20, cy + 8, m.centerX() + 20, cy + 8, g)
        val b = s.px(s.keys[2].area); val bx = b.centerX()
        val path = android.graphics.Path().apply { moveTo(bx + 30, cy + 22); lineTo(bx + 30, cy - 4); quadTo(bx + 30, cy - 18, bx + 12, cy - 18); lineTo(bx - 30, cy - 18) }
        c.drawPath(path, g); c.drawLine(bx - 30, cy - 18, bx - 14, cy - 32, g); c.drawLine(bx - 30, cy - 18, bx - 14, cy - 4, g)
    }

    /** Black glass slab with thin bezels, an earpiece and a front camera (on-screen keys come from the bar). */
    private fun slate(c: Canvas, s: Spec) {
        val body = RectF(30f, 30f, s.w - 30f, s.h - 30f)
        c.drawRoundRect(RectF(body.left - 6, body.top - 6, body.right + 6, body.bottom + 6), 126f, 126f, paint(0xFF3A3A3A))
        c.drawRoundRect(body, 120f, 120f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s.w.toFloat(), s.h.toFloat(), 0xFF262626.toInt(), 0xFF0E0E0E.toInt(), Shader.TileMode.CLAMP)
        })
        c.drawRoundRect(RectF(420f, 105f, 580f, 125f), 10f, 10f, paint(0xFF2E2E2E))       // earpiece
        c.drawCircle(300f, 115f, 16f, paint(0xFF1A1A1A)); c.drawCircle(300f, 115f, 7f, paint(0xFF263445))
        c.screen(s, 4f)
        c.glassSheen(body, 120f)
    }

    /** Landscape slider: screen part on top with side keys and a trackball, a QWERTY keyboard below. */
    private fun slider(c: Canvas, s: Spec) {
        // keyboard half, slid out
        val kb = RectF(60f, 560f, 1240f, 1230f)
        c.drawRoundRect(kb, 70f, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, kb.top, 0f, kb.bottom, 0xFF232323.toInt(), 0xFF141414.toInt(), Shader.TileMode.CLAMP)
        })
        val rows = listOf("1234567890", "QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM.")
        val keyPaint = paint(0xFF2F2F2F); val edge = paint(0xFF0A0A0A)
        val label = paint(0xFFE0E0E0).apply { textSize = 34f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
        rows.forEachIndexed { ri, row ->
            val kw = 98f; val gap = 12f
            val total = row.length * kw + (row.length - 1) * gap
            var x = (s.w - total) / 2f
            val y = 690f + ri * 128f
            row.forEach { ch ->
                c.drawRoundRect(RectF(x, y + 4, x + kw, y + 104), 22f, 22f, edge)
                c.drawRoundRect(RectF(x, y, x + kw, y + 98), 22f, 22f, keyPaint)
                c.drawText(ch.toString(), x + kw / 2, y + 64, label)
                x += kw + gap
            }
        }
        // screen half
        val top = RectF(40f, 20f, 1260f, 640f)
        c.drawRoundRect(RectF(top.left, top.top + 10, top.right, top.bottom + 10), 80f, 80f, paint(0xFF050505))
        c.drawRoundRect(top, 80f, 80f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, top.top, 0f, top.bottom, 0xFF3A3A3A.toInt(), 0xFF1D1D1D.toInt(), Shader.TileMode.CLAMP)
        })
        c.screen(s)
        c.glassSheen(top, 80f)
        // side keys and trackball
        val keyLabel = paint(0xFFCFCFCF).apply { textSize = 30f; textAlign = Paint.Align.CENTER }
        for (k in s.keys) {
            val r = s.px(k.area)
            if (k.button == NavButton.CENTER) {
                c.drawCircle(r.centerX(), r.centerY(), 46f, paint(0xFF0C0C0C))
                c.drawCircle(r.centerX(), r.centerY(), 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(r.centerX() - 12, r.centerY() - 12, 44f, 0xFF8A8A8A.toInt(), 0xFF2A2A2A.toInt(), Shader.TileMode.CLAMP)
                })
                continue
            }
            c.drawRoundRect(RectF(r.left + 6, r.top + 10, r.right - 6, r.bottom - 10), 24f, 24f, paint(0xFF2A2A2A))
            c.drawText(when (k.button) { NavButton.HOME -> "HOME"; NavButton.MENU -> "MENU"; NavButton.BACK -> "BACK"; else -> "" },
                r.centerX(), r.centerY() + 11, keyLabel)
        }
    }

    /** Black-bezel 4:3 tablet with a front camera. */
    private fun tablet(c: Canvas, s: Spec) {
        val body = RectF(20f, 20f, s.w - 20f, s.h - 20f)
        c.drawRoundRect(RectF(body.left - 6, body.top - 6, body.right + 6, body.bottom + 6), 76f, 76f, paint(0xFF444444))
        c.drawRoundRect(body, 70f, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s.h.toFloat(), 0xFF202020.toInt(), 0xFF0A0A0A.toInt(), Shader.TileMode.CLAMP)
        })
        c.drawCircle(s.w / 2f, 55f, 12f, paint(0xFF1A1A1A)); c.drawCircle(s.w / 2f, 55f, 5f, paint(0xFF263445))
        c.screen(s, 2f)
        c.glassSheen(body, 70f)
    }
}
