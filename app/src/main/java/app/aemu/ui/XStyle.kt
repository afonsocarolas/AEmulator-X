/* Added for AEmulator X, 2026-10-15: the X look — brand gradient, animated aurora background, wordmark,
 * glass surfaces and the recording indicator. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * An AEmulator X colour style: the three gradient colours ([a] → [b] → [c]) of the wordmark, badges, buttons and
 * aurora, plus the Material seed of the whole scheme ([seed] null = keep the system/dynamic/accent scheme).
 */
data class XTheme(val id: String, val label: Int, val seed: Long?, val a: Long, val b: Long, val c: Long,
                  /** Android 4.0 Holo look: always dark, flat panels with thin blue lines, square corners, no aurora */
                  val holo: Boolean = false)

object XThemes {
    val ALL = listOf(
        XTheme("system", app.aemu.R.string.x_theme_system, null, 0xFF7C4DFF, 0xFFE040FB, 0xFF18FFFF),
        XTheme("violet", app.aemu.R.string.x_theme_violet, 0xFF6A3DF0, 0xFF7C4DFF, 0xFFE040FB, 0xFF18FFFF),
        XTheme("ocean", app.aemu.R.string.x_theme_ocean, 0xFF0277BD, 0xFF1A49FF, 0xFF00B8D4, 0xFF64FFDA),
        XTheme("sunset", app.aemu.R.string.x_theme_sunset, 0xFFE64A19, 0xFFFF1744, 0xFFFF6D00, 0xFFFFD600),
        XTheme("emerald", app.aemu.R.string.x_theme_emerald, 0xFF2E7D32, 0xFF00C853, 0xFF76FF03, 0xFFB9F6CA),
        XTheme("rose", app.aemu.R.string.x_theme_rose, 0xFFD81B60, 0xFFF50057, 0xFFFF6E6E, 0xFFFFCCBC),
        XTheme("mono", app.aemu.R.string.x_theme_mono, 0xFF616161, 0xFF424242, 0xFF9E9E9E, 0xFFFAFAFA),
        // AEmulator X: Christmas red, green and gold; snow falls behind the library (Snow.kt)
        XTheme("christmas", app.aemu.R.string.x_theme_christmas, 0xFFC62828, 0xFFD32F2F, 0xFF2E7D32, 0xFFFFD54F),
        XTheme("holo", app.aemu.R.string.x_theme_holo, 0xFF33B5E5, 0xFF0099CC, 0xFF33B5E5, 0xFF8AD5F0, holo = true),
    )
    fun byId(id: String?) = ALL.firstOrNull { it.id == id } ?: ALL[0]
}

/** The X brand colours, following the chosen [XTheme] (AemuTheme sets [theme] before drawing its content). */
object XBrand {
    @Volatile var theme: XTheme = XThemes.ALL[0]
    /** first, middle and last gradient colour (named after the original violet theme) */
    val Violet: Color get() = Color(theme.a)
    val Magenta: Color get() = Color(theme.b)
    val Cyan: Color get() = Color(theme.c)
    val Rec = Color(0xFFFF1744)

    val gradient: Brush get() = Brush.linearGradient(listOf(Violet, Magenta, Cyan))
    fun sweep(alpha: Float = 1f): Brush =
        Brush.sweepGradient(listOf(Violet, Magenta, Cyan, Violet).map { it.copy(alpha = alpha) })
}

/** App settings › Appearance: one gradient swatch per [XThemes] entry; [set] gets the chosen id. */
@Composable
fun XThemePicker(current: String, onMake: () -> Unit = {}, set: (String) -> Unit) {
    Text(androidx.compose.ui.res.stringResource(app.aemu.R.string.x_theme_title), style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp))
    Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val own = CustomThemes.load(ctx)?.let { CustomThemes.toXTheme(it) }
        (XThemes.ALL + listOf(own ?: XTheme(app.aemu.core.CustomTheme.ID, app.aemu.R.string.x_tm_custom, null, 0xFF9E9E9E, 0xFFBDBDBD, 0xFFE0E0E0))).forEach { t ->
            val selected = t.id == current
            val isOwn = t.id == app.aemu.core.CustomTheme.ID
            androidx.compose.foundation.layout.Column(
                Modifier.clip(RoundedCornerShape(12.dp)).clickable { if (isOwn) onMake() else set(t.id) }.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(t.a), Color(t.b), Color(t.c))))
                        .border(if (selected) 3.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Check, null,
                        tint = Color.White, modifier = Modifier.size(24.dp))
                    else if (isOwn) androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Palette, null,
                        tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Text(androidx.compose.ui.res.stringResource(t.label), style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Is the current scheme dark (follows the user's theme choice, not only the system one). */
@Composable
fun isXDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * Slowly drifting colour blobs behind the content. Cheap: three radial gradients on one Canvas,
 * one infinite transition; the blobs take their tint from the active colour scheme.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = isXDark()
    val t by rememberInfiniteTransition(label = "aurora").animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "phase",
    )
    val blobAlpha = if (dark) 0.34f else 0.22f
    val blobs = listOf(scheme.primary, scheme.tertiary, XBrand.Magenta)
    if (XBrand.theme.holo) {
        Box(modifier.background(Color(0xFF0A0A0A))) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.width * 0.9f
                drawCircle(Brush.radialGradient(listOf(Color(0x2633B5E5), Color.Transparent), Offset(size.width / 2, 0f), r), r, Offset(size.width / 2, 0f))
            }
            LibrarySnow(Modifier.fillMaxSize())
            content()
        }
        return
    }
    Box(modifier.background(scheme.background)) {
        // AEmulator X: the user's own picture replaces the drifting colours (App settings › Library wallpaper)
        val wallpaper = WallpaperLayer(dark)
        // AEmulator X: or a live background (Settings › Appearance › Live background)
        val live = !wallpaper && LiveBackgroundLayer()
        if (!wallpaper && !live) Canvas(Modifier.fillMaxSize()) {
            val r = maxOf(size.width, size.height) * 0.62f
            blobs.forEachIndexed { i, c ->
                val ph = t + i * 2.1f
                val center = Offset(
                    size.width * (0.5f + 0.38f * cos(ph * (1f + i * 0.17f))),
                    size.height * (0.32f + 0.30f * sin(ph * (0.8f + i * 0.21f)) + i * 0.16f),
                )
                drawCircle(Brush.radialGradient(listOf(c.copy(alpha = blobAlpha), Color.Transparent), center, r), r, center)
            }
        }
        LibrarySnow(Modifier.fillMaxSize())
        content()
    }
}

/** "AEmulator" with a big glowing gradient X after it; used as the app title. */
@Composable
fun XWordmark(style: TextStyle = LocalTextStyle.current, color: Color = Color.Unspecified) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("AEmulator", style = style.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp), color = color)
        Spacer(Modifier.width(6.dp))
        Text("X", style = style.copy(
            brush = XBrand.gradient,
            fontWeight = if (XBrand.theme.holo) FontWeight.Light else FontWeight.Black,
            fontSize = if (style.fontSize.isSpecified) style.fontSize * 1.18f else 30.sp,
            shadow = if (XBrand.theme.holo) null else Shadow(XBrand.Magenta.copy(alpha = 0.55f), Offset(0f, 2f), 18f),
        ))
    }
}

/**
 * A library section title. Holo: upper-case Holo blue with a thin blue line under it (like "WI-FI NETWORKS" in
 * Android 4.0 settings); otherwise the plain primary-coloured title.
 */
@Composable
fun XSectionHeader(text: String, modifier: Modifier = Modifier) {
    if (XBrand.theme.holo) {
        androidx.compose.foundation.layout.Column(modifier.fillMaxWidth()) {
            Text(text.uppercase(), color = Color(0xFF33B5E5),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF33B5E5)))
        }
    } else Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = modifier.padding(start = 4.dp))
}

/** Frosted, slightly translucent surface with a thin gradient hairline. */
@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(28.dp), strong: Boolean = false): Modifier {
    val scheme = MaterialTheme.colorScheme
    val dark = isXDark()
    if (XBrand.theme.holo) {
        val holoShape = RoundedCornerShape(2.dp)
        return this.clip(holoShape).background(Color(0xFF1C1C1C)).border(1.dp, Color(0x5933B5E5), holoShape)
    }
    val fill = scheme.surfaceContainer.copy(alpha = if (strong) 0.92f else if (dark) 0.72f else 0.80f)
    return this
        .clip(shape)
        .background(fill)
        .border(1.dp, XBrand.sweep(if (dark) 0.45f else 0.35f), shape)
}

/** Round badge with a gradient ring (release number on firmware cards). */
@Composable
fun XBadge(text: String, size: Dp = 52.dp) {
    val shape = if (XBrand.theme.holo) RoundedCornerShape(2.dp) else CircleShape
    Box(
        Modifier.size(size).clip(shape).background(XBrand.sweep()).padding(2.5.dp)
            .clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium.copy(brush = XBrand.gradient, fontWeight = FontWeight.Black))
    }
}

/** Pulsing red dot + elapsed time, shown while the screen recorder runs. */
@Composable
fun RecordingPill(elapsedMs: Long, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
        1f, 0.25f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dot",
    )
    val s = elapsedMs / 1000
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.62f))
            .border(1.dp, XBrand.Rec.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(10.dp).alpha(pulse).clip(CircleShape).background(XBrand.Rec))
        Text("REC  %02d:%02d".format(s / 60, s % 60), color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
    }
}
