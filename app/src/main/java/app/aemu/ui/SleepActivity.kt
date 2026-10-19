/* Added for AEmulator X, 2026-10-18: Sleep mode. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.SleepSound
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sleep mode: a dark, dimmed night sky with a quiet clock, slow 4-7-8 breathing, generated sleep sounds and a sleep
 * timer. Warm, dim colours only (no bright blue light); the controls hide by themselves. When the timer ends the sound
 * has already faded out over the last two minutes, the screen goes black and the phone may turn its screen off.
 */
class SleepActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.aemu.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        dim(true)
        setContent { Sleep(onClose = ::finish, onEnded = { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }) }
    }

    /** The screen at its lowest brightness while this page is open (only this window; the phone's setting is untouched). */
    private fun dim(on: Boolean) {
        window.attributes = window.attributes.apply {
            screenBrightness = if (on) 0.02f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }
}

/** 4-7-8 breathing (in 4 s, hold 7 s, out 8 s): phase 0 = in, 1 = hold, 2 = out, and the circle size 0..1. */
internal object SleepBreathing {
    const val IN = 4; const val HOLD = 7; const val OUT = 8
    const val CYCLE = IN + HOLD + OUT

    fun at(t: Float): Pair<Int, Float> {
        val s = ((t % CYCLE) + CYCLE) % CYCLE
        return when {
            s < IN -> 0 to ease(s / IN)
            s < IN + HOLD -> 1 to 1f
            else -> 2 to 1f - ease((s - IN - HOLD) / OUT)
        }
    }
    private fun ease(x: Float) = (0.5f - 0.5f * cos(PI * x.coerceIn(0f, 1f))).toFloat()
}

/** Sleep timer: the sound volume 0..1 with [leftSec] seconds left; it fades out over the last [FADE] seconds. */
internal object SleepTimer {
    const val FADE = 120f
    fun volume(leftSec: Float): Float = if (leftSec < 0f) 1f else (leftSec / FADE).coerceIn(0f, 1f)
}

private val Amber = Color(0xFFFFB74D)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun Sleep(onClose: () -> Unit, onEnded: () -> Unit) {
    val player = remember { SleepSound() }
    var sound by remember { mutableStateOf<SleepSound.Kind?>(SleepSound.Kind.RAIN) }
    var breathe by remember { mutableStateOf(true) }
    var minutes by remember { mutableIntStateOf(30) }          // 0 = no limit
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var touchedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(Unit) { onDispose { player.stop() } }
    // the clock, the fade and the end run here (not only while drawing), so they also work with the screen turned off
    LaunchedEffect(minutes, startedAt) {
        while (true) {
            now = System.currentTimeMillis()
            val l = if (minutes > 0) minutes * 60 - (now - startedAt) / 1000f else -1f
            player.gain = SleepTimer.volume(l)
            if (minutes > 0 && l <= 0f) player.stop()
            delay(50)
        }
    }

    val elapsed = (now - startedAt) / 1000f
    val left = if (minutes > 0) (minutes * 60 - elapsed).coerceAtLeast(0f) else -1f
    val ended = minutes > 0 && left <= 0f
    LaunchedEffect(sound, ended) {
        if (sound != null && !ended) { player.kind = sound!!; player.start() } else player.stop()
        if (ended) onEnded()
    }
    val showControls = !ended && now - touchedAt < 10_000
    // after the end everything fades to black
    val sky by animateFloatAsState(if (ended) 0f else 1f, tween(8_000), label = "sky")

    Box(Modifier.fillMaxSize().background(Color.Black)
        .clickable(remember { MutableInteractionSource() }, indication = null) { touchedAt = System.currentTimeMillis() }) {
        // night sky with softly twinkling stars and a crescent moon
        val stars = remember { Random(7).let { r -> List(70) { Triple(r.nextFloat(), r.nextFloat() * 0.75f, r.nextFloat() * 6.28f) } } }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF070B1A).copy(alpha = sky), Color.Black)))
            val t = elapsed / 3f
            stars.forEach { (x, y, ph) ->
                val a = (0.25f + 0.25f * sin(t + ph)) * sky
                drawCircle(Color(0xFFFFE0B2).copy(alpha = a), 1.6.dp.toPx(), Offset(x * size.width, y * size.height))
            }
            val moon = Offset(size.width * 0.78f, size.height * 0.14f)
            val mr = 26.dp.toPx()
            drawCircle(Brush.radialGradient(listOf(Amber.copy(alpha = 0.10f * sky), Color.Transparent), moon, mr * 3), mr * 3, moon)
            drawCircle(Color(0xFFFFE0B2).copy(alpha = 0.55f * sky), mr, moon)
            drawCircle(Color(0xFF070B1A), mr * 0.95f, Offset(moon.x + mr * 0.45f, moon.y - mr * 0.2f))
        }
        // AEmulator X: soft warm snow in December (App settings › Snow in December); fades with the sky
        val ctx = androidx.compose.ui.platform.LocalContext.current
        if (sky > 0.05f && Snowfall.isDecember() && app.aemu.AppPrefs.decemberSnow(ctx))
            SnowLayer(Modifier.fillMaxSize(), count = 40, tint = Color(0xFFFFE0B2).copy(alpha = 0.6f * sky))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now)),
                color = Amber.copy(alpha = 0.45f * sky), fontSize = 52.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(28.dp))
            if (ended) {
                Text(stringResource(R.string.x_sleep_goodnight), color = Amber.copy(alpha = 0.5f * sky), fontSize = 20.sp)
            } else if (breathe) {
                val (phase, grow) = SleepBreathing.at(elapsed)
                Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val r = this.size.minDimension / 2 * (0.35f + 0.6f * grow)
                        drawCircle(Brush.radialGradient(listOf(Amber.copy(alpha = 0.22f), Color.Transparent), center, r * 1.4f), r * 1.4f)
                        drawCircle(Amber.copy(alpha = 0.10f), r)
                    }
                    Text(stringResource(when (phase) { 0 -> R.string.x_relax_in; 1 -> R.string.x_relax_hold; else -> R.string.x_relax_out }),
                        color = Amber.copy(alpha = 0.6f), fontSize = 18.sp)
                }
            }
        }
        AnimatedVisibility(showControls, Modifier.align(Alignment.TopEnd), enter = fadeIn(), exit = fadeOut()) {
            IconButton(onClick = onClose, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Amber.copy(alpha = 0.7f))
            }
        }
        AnimatedVisibility(showControls, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            Column(Modifier.navigationBarsPadding().padding(16.dp).padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (left >= 0) "%d:%02d".format(left.toInt() / 60, left.toInt() % 60) else stringResource(R.string.x_sleep_title),
                    color = Amber.copy(alpha = 0.7f), fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                val chip = FilterChipDefaults.filterChipColors(
                    labelColor = Amber.copy(alpha = 0.7f), selectedLabelColor = Color.Black,
                    selectedContainerColor = Amber.copy(alpha = 0.6f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    listOf(SleepSound.Kind.RAIN to R.string.x_sleep_rain, SleepSound.Kind.OCEAN to R.string.x_sleep_ocean,
                        SleepSound.Kind.NOISE to R.string.x_sleep_noise, null to R.string.x_sleep_silence).forEach { (k, label) ->
                        FilterChip(selected = sound == k, colors = chip,
                            onClick = { sound = k; touchedAt = System.currentTimeMillis() }, label = { Text(stringResource(label)) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    listOf(15, 30, 45, 60, 0).forEach { m ->
                        FilterChip(selected = minutes == m, colors = chip,
                            onClick = { minutes = m; startedAt = System.currentTimeMillis(); touchedAt = startedAt },
                            label = { Text(if (m == 0) "∞" else stringResource(R.string.x_relax_min, m)) })
                    }
                    FilterChip(selected = breathe, colors = chip,
                        onClick = { breathe = !breathe; touchedAt = System.currentTimeMillis() },
                        label = { Text(stringResource(R.string.x_sleep_breathe)) })
                }
            }
        }
    }
}
