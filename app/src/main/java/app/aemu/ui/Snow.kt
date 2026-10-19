/* Added for AEmulator X, 2026-10-19: Christmas theme snow. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** When the library (and Sleep mode) shows falling snow: the Christmas theme, or any theme in December. */
object Snowfall {
    fun isDecember(cal: Calendar = Calendar.getInstance()): Boolean = cal.get(Calendar.MONTH) == Calendar.DECEMBER

    fun wanted(themeId: String, decemberSnow: Boolean, cal: Calendar = Calendar.getInstance()): Boolean =
        themeId == "christmas" || (decemberSnow && isDecember(cal))

    /**
     * One flake at loop phase [p] (0..1): x, y in 0..1 of the area. Each flake falls a whole number of screen heights
     * per loop, so the loop joins without a jump; it sways sideways a little.
     */
    fun position(x0: Float, y0: Float, falls: Int, sway: Float, phase: Float, p: Float): Offset {
        val y = (y0 + p * falls) % 1f
        val x = (x0 + sway * sin((p * falls * 2 * PI + phase).toFloat())).let { ((it % 1f) + 1f) % 1f }
        return Offset(x, y)
    }
}

private class Flake(val x: Float, val y: Float, val falls: Int, val sway: Float, val phase: Float, val radius: Float, val alpha: Float)

/** Gentle snow over the whole area; [count] flakes, slow (one loop = 40 s), drawn on one Canvas. */
@Composable
fun SnowLayer(modifier: Modifier = Modifier, count: Int = 70, tint: Color = Color.White) {
    val flakes = remember(count) {
        val r = Random(2026)
        List(count) {
            Flake(r.nextFloat(), r.nextFloat(), 1 + r.nextInt(3), 0.01f + r.nextFloat() * 0.03f,
                r.nextFloat() * 6.28f, 1.2f + r.nextFloat() * 2.6f, 0.35f + r.nextFloat() * 0.5f)
        }
    }
    val p by rememberInfiniteTransition(label = "snow").animateFloat(0f, 1f,
        infiniteRepeatable(tween(40_000, easing = LinearEasing)), label = "fall")
    Canvas(modifier) {
        for (f in flakes) {
            val o = Snowfall.position(f.x, f.y, f.falls, f.sway, f.phase, p)
            drawCircle(tint.copy(alpha = tint.alpha * f.alpha), f.radius * density, Offset(o.x * size.width, o.y * size.height))
        }
    }
}

/** Snow in the library when [Snowfall.wanted] says so (reads the theme and the December switch). */
@Composable
fun LibrarySnow(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    if (Snowfall.wanted(XBrand.theme.id, app.aemu.AppPrefs.decemberSnow(ctx))) SnowLayer(modifier)
}
