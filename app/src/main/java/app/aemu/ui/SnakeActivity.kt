/* Added for AEmulator X, 2026-10-20: the hidden Snake game. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.Achievements
import app.aemu.core.SnakeGame
import kotlinx.coroutines.delay
import kotlin.math.abs

/** The secret game behind the About logo: Snake eating Android desserts. Swipe, arrow keys, WASD or a controller. */
class SnakeActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.aemu.AppPrefs.wrap(base))

    private var game by mutableStateOf(SnakeGame())
    private var paused by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { Snake() }
    }

    override fun onPause() { super.onPause(); paused = true }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val d = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> SnakeGame.Dir.UP
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> SnakeGame.Dir.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> SnakeGame.Dir.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> SnakeGame.Dir.RIGHT
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_DPAD_CENTER -> { action(); return true }
            else -> return super.onKeyDown(keyCode, event)
        }
        paused = false
        game.turn(d)
        return true
    }

    /** Tap / A / Space: a new game when it is over, otherwise pause and resume. */
    private fun action() {
        if (game.over) { game = SnakeGame(); paused = false } else paused = !paused
    }

    private fun best(): Int = getSharedPreferences("x_snake", MODE_PRIVATE).getInt("best", 0)

    @Composable
    private fun Snake() {
        var tick by remember { mutableIntStateOf(0) }
        var best by remember { mutableIntStateOf(best()) }
        var eatenName by remember { mutableStateOf<String?>(null) }
        val g = game
        LaunchedEffect(g, paused) {
            while (!paused && !g.over) {
                delay(g.tickMs)
                val ate = g.step()
                if (ate) app.aemu.core.UiSounds.play(this@SnakeActivity, app.aemu.core.UiSounds.Sfx.POP)
                if (ate) eatenName = SnakeGame.DESSERTS[(g.score - 1) % SnakeGame.DESSERTS.size].let { "${it.emoji} ${it.name} (${it.version})" }
                if (g.over) {
                    app.aemu.core.UiSounds.play(this@SnakeActivity, app.aemu.core.UiSounds.Sfx.GAME_OVER)
                    if (g.score >= 5) runCatching { Achievements.bump(this@SnakeActivity, app.aemu.core.DailyChallenge.SNAKE_ROUND) }
                    if (g.score > best) {
                        best = g.score
                        getSharedPreferences("x_snake", MODE_PRIVATE).edit().putInt("best", best).apply()
                    }
                    // every dessert in one game, Cupcake to Pie
                    if (g.score >= SnakeGame.DESSERTS.size) runCatching { Achievements.bump(this@SnakeActivity, Achievements.SNAKE) }
                }
                tick++
            }
        }
        LaunchedEffect(eatenName, g.score) { if (eatenName != null) { delay(1500); eatenName = null } }

        Box(Modifier.fillMaxSize().background(Color(0xFF101B12))) {
            Column(Modifier.align(Alignment.Center).padding(16.dp).widthIn(max = 640.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🐍 " + stringResource(R.string.x_snake_title), color = Color(0xFFB9F6CA), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text(stringResource(R.string.x_snake_score, g.score), color = Color.White, fontSize = 18.sp)
                    Text(stringResource(R.string.x_snake_best, best), color = Color(0xFFFFE082), fontSize = 18.sp)
                }
                Box(Modifier.fillMaxWidth().aspectRatio(g.w.toFloat() / g.h)
                    .pointerInput(g) {
                        var acc = Offset.Zero
                        detectDragGestures(onDragStart = { acc = Offset.Zero }) { change, drag ->
                            change.consume()
                            acc += drag
                            if (maxOf(abs(acc.x), abs(acc.y)) > 40f) {
                                g.turn(if (abs(acc.x) > abs(acc.y)) { if (acc.x > 0) SnakeGame.Dir.RIGHT else SnakeGame.Dir.LEFT }
                                       else { if (acc.y > 0) SnakeGame.Dir.DOWN else SnakeGame.Dir.UP })
                                paused = false
                                acc = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(g) { detectTapGestures { action() } }) {
                    Board(g, tick)
                    val msg = when {
                        g.over -> stringResource(R.string.x_snake_over, g.score)
                        paused -> stringResource(R.string.x_snake_paused)
                        else -> null
                    }
                    if (msg != null) Text(msg, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).background(Color(0xCC000000), androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(16.dp))
                }
                Text(eatenName?.let { stringResource(R.string.x_snake_ate, it) } ?: stringResource(R.string.x_snake_hint),
                    color = Color(0xFFA5D6A7), fontSize = 15.sp, textAlign = TextAlign.Center)
            }
            IconButton(onClick = ::finish, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().navigationBarsPadding().padding(8.dp)) {
                Icon(Icons.Rounded.Close, null, tint = Color.White)
            }
        }
    }

    @Composable
    private fun Board(g: SnakeGame, tick: Int) {
        Canvas(Modifier.fillMaxSize()) {
            tick // redraw on every step
            val cw = size.width / g.w; val ch = size.height / g.h
            drawRoundRect(Color(0xFF1B2E1E), cornerRadius = CornerRadius(cw))
            for (y in 0 until g.h) for (x in 0 until g.w) if ((x + y) % 2 == 0)
                drawRect(Color(0xFF203722), Offset(x * cw, y * ch), Size(cw, ch))
            val n = g.body.size
            g.body.forEachIndexed { i, c ->
                val col = lerp(Color(0xFF69F0AE), Color(0xFF1B5E20), i / n.coerceAtLeast(2).toFloat())
                val pad = if (i == 0) cw * 0.04f else cw * 0.1f
                drawRoundRect(col, Offset(c.x * cw + pad, c.y * ch + pad), Size(cw - 2 * pad, ch - 2 * pad), CornerRadius(cw * 0.3f))
            }
            g.body.firstOrNull()?.let { h ->           // eyes look where it is going
                val cx = h.x * cw + cw / 2 + g.dir.dx * cw * 0.15f; val cy = h.y * ch + ch / 2 + g.dir.dy * ch * 0.15f
                val ox = g.dir.dy * cw * 0.18f; val oy = g.dir.dx * ch * 0.18f
                for (s in listOf(-1f, 1f)) {
                    drawCircle(Color.White, cw * 0.12f, Offset(cx + s * ox, cy + s * oy))
                    drawCircle(Color.Black, cw * 0.06f, Offset(cx + s * ox + g.dir.dx * cw * 0.03f, cy + s * oy + g.dir.dy * ch * 0.03f))
                }
            }
            g.food?.let { f ->
                drawIntoCanvas { c ->
                    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = ch * 0.85f; textAlign = android.graphics.Paint.Align.CENTER
                    }
                    c.nativeCanvas.drawText(g.dessert.emoji, f.x * cw + cw / 2, f.y * ch + ch * 0.78f, p)
                }
            }
        }
    }
}
