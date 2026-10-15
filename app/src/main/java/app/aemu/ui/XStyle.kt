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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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

/** The X brand colours: electric violet → magenta → cyan. */
object XBrand {
    val Violet = Color(0xFF7C4DFF)
    val Magenta = Color(0xFFE040FB)
    val Cyan = Color(0xFF18FFFF)
    val Rec = Color(0xFFFF1744)

    val gradient: Brush get() = Brush.linearGradient(listOf(Violet, Magenta, Cyan))
    fun sweep(alpha: Float = 1f): Brush =
        Brush.sweepGradient(listOf(Violet, Magenta, Cyan, Violet).map { it.copy(alpha = alpha) })
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
    Box(modifier.background(scheme.background)) {
        Canvas(Modifier.fillMaxSize()) {
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
            fontWeight = FontWeight.Black,
            fontSize = if (style.fontSize.isSpecified) style.fontSize * 1.18f else 30.sp,
            shadow = Shadow(XBrand.Magenta.copy(alpha = 0.55f), Offset(0f, 2f), 18f),
        ))
    }
}

/** Frosted, slightly translucent surface with a thin gradient hairline. */
@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(28.dp), strong: Boolean = false): Modifier {
    val scheme = MaterialTheme.colorScheme
    val dark = isXDark()
    val fill = scheme.surfaceContainer.copy(alpha = if (strong) 0.92f else if (dark) 0.72f else 0.80f)
    return this
        .clip(shape)
        .background(fill)
        .border(1.dp, XBrand.sweep(if (dark) 0.45f else 0.35f), shape)
}

/** Round badge with a gradient ring (release number on firmware cards). */
@Composable
fun XBadge(text: String, size: Dp = 52.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(XBrand.sweep()).padding(2.5.dp)
            .clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
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
