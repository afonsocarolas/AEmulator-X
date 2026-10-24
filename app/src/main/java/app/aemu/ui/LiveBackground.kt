/* Added for AEmulator X, 2026-10-24: live backgrounds. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import app.aemu.core.LiveScenes
import app.aemu.core.LiveScenes.Scene
import kotlin.math.sin

/** Which live background is chosen (Settings › Appearance › Live background). */
object LiveBackgrounds {
    private const val KEY = "x_live_bg"
    fun get(ctx: android.content.Context): Scene = Scene.of(app.aemu.AppPrefs.prefs(ctx).getString(KEY, null))
    fun set(ctx: android.content.Context, s: Scene) = app.aemu.AppPrefs.prefs(ctx).edit().putString(KEY, s.id).apply()
}

/** The chosen scene, moving, with a dark veil over it so the library stays easy to read; false when none is chosen. */
@Composable
fun LiveBackgroundLayer(): Boolean {
    val ctx = LocalContext.current
    val resumes = rememberResumeCount()
    val scene = remember(resumes) { LiveBackgrounds.get(ctx) }
    if (scene == Scene.OFF) return false
    LiveScene(scene, Modifier.fillMaxSize())
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
    return true
}

@Composable
fun LiveScene(scene: Scene, modifier: Modifier) {
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(scene) {
        var start = 0L
        while (true) withFrameNanos { now -> if (start == 0L) start = now; t = (now - start) / 1e9f }
    }
    val stars = remember { LiveScenes.stars() }
    val cols = remember { LiveScenes.columns(28) }
    val drops = remember { LiveScenes.drops() }
    val flies = remember { LiveScenes.flies() }
    Canvas(modifier) {
        when (scene) {
            Scene.STARS -> drawStars(stars, t)
            Scene.SYNTH -> drawSynth(t)
            Scene.MATRIX -> drawMatrix(cols, t)
            Scene.RAIN -> drawRain(drops, t)
            Scene.FIREFLIES -> drawFireflies(flies, t)
            Scene.OFF -> {}
        }
    }
}

private fun DrawScope.drawStars(stars: List<LiveScenes.Star>, t: Float) {
    drawRect(Brush.radialGradient(listOf(Color(0xFF14123A), Color(0xFF02020A)), Offset(size.width / 2, size.height / 2), maxOf(size.width, size.height)))
    val cx = size.width / 2; val cy = size.height / 2; val k = minOf(size.width, size.height) * 0.5f
    for (s in stars) {
        val z = LiveScenes.starDepth(s, t)
        val z2 = (z + 0.04f).coerceAtMost(1f)                       // a little earlier on its trip: the tail
        val p = Offset(cx + s.x / z * k, cy + s.y / z * k)
        val q = Offset(cx + s.x / z2 * k, cy + s.y / z2 * k)
        if (p.x < -20 || p.y < -20 || p.x > size.width + 20 || p.y > size.height + 20) continue
        val b = (1f - z).coerceIn(0f, 1f)
        drawLine(Color(0xFFB3E5FC).copy(alpha = b * 0.7f), q, p, strokeWidth = 1f + b * 3f, cap = StrokeCap.Round)
        drawCircle(Color.White.copy(alpha = 0.3f + b * 0.7f), 0.6f + b * 2.4f, p)
    }
}

private fun DrawScope.drawSynth(t: Float) {
    val horizon = size.height * 0.55f
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1A0533), Color(0xFF5B1259), Color(0xFFE5486E)), 0f, horizon), size = Size(size.width, horizon))
    // the sun, with the stripes old synthwave posters have
    val r = minOf(size.width, size.height) * 0.22f
    val sun = Offset(size.width / 2, horizon - r * 0.35f)
    drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFE066), Color(0xFFFF4E8A)), sun.y - r, sun.y + r), r, sun)
    for (i in 0 until 6) {
        val y = sun.y + r * (0.15f + i * 0.14f)
        drawRect(Color(0xFF5B1259), Offset(sun.x - r, y), Size(r * 2, r * (0.03f + i * 0.012f)))
    }
    drawRect(Color(0xFF12001F), Offset(0f, horizon), Size(size.width, size.height - horizon))
    val line = Color(0xFFFF2BD6)
    val lines = 16
    for (i in 0 until lines) {
        val f = LiveScenes.gridLine(i, lines, t)
        val y = horizon + (size.height - horizon) * f
        drawLine(line.copy(alpha = 0.25f + 0.75f * f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f + 2f * f)
    }
    for (i in -12..12) {
        val xb = size.width / 2 + i * size.width / 8f
        drawLine(Color(0xFF18FFFF).copy(alpha = 0.55f), Offset(size.width / 2 + i * size.width / 80f, horizon), Offset(xb, size.height), strokeWidth = 1.5f)
    }
    drawRect(Brush.verticalGradient(listOf(Color(0x99FF2BD6), Color.Transparent), horizon - 4, horizon + 40), Offset(0f, horizon - 4), Size(size.width, 44f))
}

private fun DrawScope.drawMatrix(cols: List<LiveScenes.Column>, t: Float) {
    drawRect(Color(0xFF000A02))
    val cell = size.width / cols.size
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = cell * 0.9f; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.MONOSPACE
    }
    drawIntoCanvas { c ->
        cols.forEachIndexed { ci, col ->
            val head = LiveScenes.columnHead(col, t) * size.height
            for (k in 0 until col.length) {
                val y = head - k * cell
                if (y < -cell || y > size.height + cell) continue
                val row = (y / cell).toInt()
                paint.color = if (k == 0) 0xFFE8FFE8.toInt() else android.graphics.Color.argb((235 * (1f - k.toFloat() / col.length)).toInt(), 0, 230, 70)
                c.nativeCanvas.drawText(LiveScenes.glyph(ci, row, t).toString(), col.x * size.width, y, paint)
            }
        }
    }
}

private fun DrawScope.drawRain(drops: List<LiveScenes.Drop>, t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1622), Color(0xFF1B2B3A))))
    // blurry city lights behind the glass
    val lights = listOf(Color(0xFFFFB74D), Color(0xFF4FC3F7), Color(0xFFF06292), Color(0xFFFFF176))
    for (i in 0 until 14) {
        val x = size.width * ((i * 0.137f + 0.05f) % 1f); val y = size.height * (0.55f + 0.35f * ((i * 0.29f) % 1f))
        val rr = minOf(size.width, size.height) * (0.04f + 0.03f * ((i * 0.41f) % 1f))
        val a = 0.18f + 0.08f * sin(t * 0.5f + i)
        drawCircle(Brush.radialGradient(listOf(lights[i % 4].copy(alpha = a), Color.Transparent), Offset(x, y), rr), rr, Offset(x, y))
    }
    for (d in drops) {
        val y = LiveScenes.dropY(d, t) * size.height
        val x = d.x * size.width + (y / size.height) * size.width * 0.08f
        val len = d.length * size.height
        drawLine(Color(0xFFB0C4DE).copy(alpha = 0.35f), Offset(x, y), Offset(x - len * 0.15f, y - len), strokeWidth = 1.6f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawFireflies(flies: List<LiveScenes.Fly>, t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF02060F), Color(0xFF071A12))))
    // dark grass along the bottom
    for (i in 0 until 60) {
        val x = size.width * i / 60f; val h = size.height * (0.06f + 0.05f * ((i * 0.37f) % 1f))
        drawLine(Color(0xFF0B2A1A), Offset(x, size.height), Offset(x + 6f * sin(t * 0.6f + i), size.height - h), strokeWidth = 5f, cap = StrokeCap.Round)
    }
    for (f in flies) {
        val x = (f.x + f.rx * sin(t * f.speed + f.phase)) * size.width
        val y = (f.y + f.ry * sin(t * f.speed * 1.3f + f.phase * 2)) * size.height
        val g = LiveScenes.glow(f, t)
        val r = minOf(size.width, size.height) * 0.025f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFE6FF7A).copy(alpha = 0.55f * g), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
        drawCircle(Color(0xFFF4FF9E).copy(alpha = 0.25f + 0.75f * g), 2.5f, Offset(x, y))
    }
}

/** Settings › Appearance: the scenes as chips, with a small preview of the chosen one moving. */
@Composable
fun LiveBackgroundPicker() {
    val ctx = LocalContext.current
    var scene by remember { androidx.compose.runtime.mutableStateOf(LiveBackgrounds.get(ctx)) }
    val labels = mapOf(Scene.OFF to app.aemu.R.string.x_lb_off, Scene.STARS to app.aemu.R.string.x_lb_stars, Scene.SYNTH to app.aemu.R.string.x_lb_synth,
        Scene.MATRIX to app.aemu.R.string.x_lb_matrix, Scene.RAIN to app.aemu.R.string.x_lb_rain, Scene.FIREFLIES to app.aemu.R.string.x_lb_fireflies)
    androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(app.aemu.R.string.x_lb_title), style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp))
    androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(app.aemu.R.string.x_lb_sub), style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
    androidx.compose.foundation.layout.Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        for (s in Scene.entries) androidx.compose.material3.FilterChip(selected = scene == s, onClick = { scene = s; LiveBackgrounds.set(ctx, s) },
            label = { androidx.compose.material3.Text(s.emoji + " " + androidx.compose.ui.res.stringResource(labels.getValue(s))) })
    }
    if (scene != Scene.OFF) Box(Modifier.padding(16.dp).fillMaxWidth().height(140.dp)
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))) { LiveScene(scene, Modifier.fillMaxSize()) }
}
