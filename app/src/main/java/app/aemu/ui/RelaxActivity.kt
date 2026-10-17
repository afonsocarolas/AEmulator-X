/* Added for AEmulator X, 2026-10-17: Relax mode. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.OceanSound
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A calm screen: slow colours, a breathing guide (in 4 s, hold 4 s, out 6 s), optional ocean sound, a gentle timer. */
class RelaxActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.aemu.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { Relax(onClose = ::finish) }
    }
}

/** One breathing cycle: phase, seconds. */
internal object Breathing {
    const val IN = 4; const val HOLD = 4; const val OUT = 6
    const val CYCLE = IN + HOLD + OUT

    /** 0 = breathe in, 1 = hold, 2 = breathe out, and the circle size 0..1 at [t] seconds into the cycle. */
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

@Composable
private fun Relax(onClose: () -> Unit) {
    val ocean = remember { OceanSound() }
    var sound by remember { mutableStateOf(false) }
    var minutes by remember { mutableIntStateOf(0) }          // 0 = no limit
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(Unit) { onDispose { ocean.stop() } }
    LaunchedEffect(sound) { if (sound) ocean.start() else ocean.stop() }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(50) } }
    val elapsed = (now - startedAt) / 1000f
    val left = if (minutes > 0) (minutes * 60 - elapsed).coerceAtLeast(0f) else -1f
    val done = minutes > 0 && left <= 0f
    LaunchedEffect(done) { if (done) sound = false } // the timer ends quietly
    val (phase, breath) = if (done) 1 to 0.6f else Breathing.at(elapsed)

    // slow drifting colours
    val drift by rememberInfiniteTransition(label = "relax").animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "drift")
    Box(Modifier.fillMaxSize().background(Color(0xFF06121F))) {
        Canvas(Modifier.fillMaxSize()) {
            val r = maxOf(size.width, size.height) * 0.75f
            listOf(Color(0xFF0E4D64) to 0f, Color(0xFF1B3A6B) to 2.1f, Color(0xFF0B6E6E) to 4.2f).forEach { (c, ph) ->
                val center = Offset(size.width * (0.5f + 0.35f * cos(drift + ph)), size.height * (0.5f + 0.3f * sin(drift * 0.8f + ph)))
                drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.55f), Color.Transparent), center, r), r, center)
            }
        }
        // breathing circle
        Canvas(Modifier.align(Alignment.Center).size(320.dp)) {
            val max = this.size.minDimension / 2
            val rad = max * (0.35f + 0.6f * breath)
            drawCircle(Brush.radialGradient(listOf(Color(0x5580DEEA), Color(0x0080DEEA)), center, rad * 1.35f), rad * 1.35f)
            drawCircle(Brush.radialGradient(listOf(Color(0xCCB2EBF2), Color(0x6626C6DA)), center, rad), rad)
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(when { done -> R.string.x_relax_done; phase == 0 -> R.string.x_relax_in; phase == 1 -> R.string.x_relax_hold; else -> R.string.x_relax_out }),
                color = Color(0xFF06232D), fontSize = 22.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
        Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
            IconButton(onClick = { sound = !sound }) {
                Icon(if (sound) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff, stringResource(R.string.x_relax_sound), tint = Color.White)
            }
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color.White) }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (left >= 0) "%d:%02d".format(left.toInt() / 60, left.toInt() % 60) else stringResource(R.string.x_relax_title),
                color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 1, 3, 5).forEach { m ->
                    FilterChip(selected = minutes == m, onClick = { minutes = m; startedAt = System.currentTimeMillis() },
                        label = { Text(if (m == 0) "∞" else stringResource(R.string.x_relax_min, m), color = Color.White) })
                }
            }
        }
    }
}
