/* Added for AEmulator X, 2026-10-22: Penalty shootout. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.Achievements
import app.aemu.core.Penalty
import app.aemu.core.UiSounds
import kotlin.math.sin

/** Penalty shootout against the phone: swipe the ball to shoot, tap where to dive when you keep goal. */
class PenaltyActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.aemu.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { PenaltyScreen(onClose = ::finish) }
    }
}

private enum class Phase { AIM, RUNUP, FLY, RESULT, END }

/** The goal and the ball's spot on this screen, in pixels. */
private class Field(val w: Float, val h: Float) {
    val goalW = minOf(w * 0.82f, h * 0.9f)
    val goalH = goalW * 0.34f
    val goalLeft = (w - goalW) / 2
    val goalBottom = h * 0.2f + goalH
    val spot = Offset(w / 2, h * 0.8f)
    val ballR = goalW * 0.045f
    /** a point in the goal mouth (0..1 each way; outside is past the posts or over the bar) on the screen */
    fun at(x: Float, y: Float) = Offset(goalLeft + x * goalW, goalBottom - y * goalH)
}

@Composable
private fun PenaltyScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var game by remember { mutableStateOf(Penalty()) }
    var phase by remember { mutableStateOf(Phase.AIM) }
    var phaseT by remember { mutableFloatStateOf(0f) }             // seconds in this phase
    var shot by remember { mutableStateOf(0.5f to 0.3f) }
    var dive by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var result by remember { mutableStateOf<Penalty.Result?>(null) }
    var drag by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var field by remember { mutableStateOf<Field?>(null) }
    var tick by remember { mutableIntStateOf(0) }

    fun startKick() {
        dive = null; result = null; phaseT = 0f
        phase = if (game.over) Phase.END else if (game.playerShoots) Phase.AIM else Phase.RUNUP
        if (phase == Phase.RUNUP) shot = game.cpuShot()
    }
    fun finishKick() {
        val (dx, dy) = dive ?: (0.5f to 0.3f)                       // a keeper who doesn't move stays in the middle
        val r = Penalty.resolve(shot.first, shot.second, dx, dy)
        val shooter = game.playerShoots
        game.record(r)
        result = r
        phase = Phase.RESULT; phaseT = 0f
        val good = (r == Penalty.Result.GOAL) == shooter
        UiSounds.play(ctx, if (r == Penalty.Result.GOAL) UiSounds.Sfx.CHEER else if (good) UiSounds.Sfx.POP else UiSounds.Sfx.GAME_OVER)
        if (game.over) {
            if (game.playerWon) runCatching { Achievements.bump(ctx, Achievements.PENALTY_WIN) }
            UiSounds.play(ctx, UiSounds.Sfx.WHISTLE)
        }
    }

    LaunchedEffect(game) {
        var last = 0L
        while (true) withFrameNanos { now ->
            val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
            last = now
            phaseT += dt
            when (phase) {
                Phase.RUNUP -> if (phaseT > RUNUP) { phase = Phase.FLY; phaseT = 0f }
                Phase.FLY -> if (phaseT > FLIGHT) finishKick()
                Phase.RESULT -> if (phaseT > 1.6f) startKick()
                else -> {}
            }
            tick++
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF1B5E20))
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val f = field ?: return@awaitEachGesture
                when (phase) {
                    Phase.AIM -> {
                        // a swipe starting near the ball shoots
                        if ((down.position - f.spot).getDistance() > f.ballR * 5) return@awaitEachGesture
                        drag = down.position to down.position
                        while (true) {
                            val c = awaitPointerEvent().changes.firstOrNull() ?: break
                            drag = down.position to c.position
                            if (!c.pressed) break
                        }
                        val (a, b) = drag ?: return@awaitEachGesture
                        drag = null
                        val dx = (b.x - a.x) / f.w; val dy = (a.y - b.y) / f.h
                        if (dy < 0.04f) return@awaitEachGesture          // a tiny flick is no shot
                        shot = Penalty.aim(dx, dy)
                        dive = game.cpuDive(shot.first, shot.second)
                        UiSounds.play(ctx, UiSounds.Sfx.POP)
                        phase = Phase.FLY; phaseT = 0f
                    }
                    // you keep goal: tap where to dive, before the ball gets there
                    Phase.RUNUP, Phase.FLY -> if (dive == null) {
                        dive = ((down.position.x - f.goalLeft) / f.goalW).coerceIn(0f, 1f) to
                            ((f.goalBottom - down.position.y) / f.goalH).coerceIn(0f, 1f)
                    }
                    else -> {}
                }
            }
        }) {
        Canvas(Modifier.fillMaxSize()) {
            tick
            val f = field?.takeIf { it.w == size.width && it.h == size.height } ?: Field(size.width, size.height).also { field = it }
            drawPitch(f)
            // keeper: in the middle, then diving to his point
            val diving = phase == Phase.FLY || phase == Phase.RESULT
            val k = dive
            val p = when {
                k == null || !diving -> 0f
                phase == Phase.RESULT -> 1f
                else -> (phaseT / FLIGHT).coerceIn(0f, 1f)
            }
            val home = f.at(0.5f, 0.3f)
            val to = if (k != null) f.at(k.first, k.second) else home
            val kp = Offset(home.x + (to.x - home.x) * p, home.y + (to.y - home.y) * p)
            val lean = if (k == null) 0f else ((k.first - 0.5f) * 140f).coerceIn(-75f, 75f) * p
            drawKeeper(f, kp, lean, mine = !game.playerShoots && phase != Phase.RESULT || (phase == Phase.RESULT && game.kicks.lastOrNull()?.byPlayer == false))
            // ball: from the spot to the shot point, smaller as it goes away, a little arc
            val target = f.at(shot.first, shot.second)
            val bp = when (phase) { Phase.FLY -> (phaseT / FLIGHT).coerceIn(0f, 1f); Phase.RESULT -> 1f; else -> 0f }
            val arc = sin(bp * Math.PI).toFloat() * f.goalH * 0.25f
            val ball = Offset(f.spot.x + (target.x - f.spot.x) * bp, f.spot.y + (target.y - f.spot.y) * bp - arc)
            // a saved ball bounces back out
            val saved = phase == Phase.RESULT && result == Penalty.Result.SAVED
            val shown = if (saved) Offset(ball.x, ball.y + f.goalH * 0.6f * (phaseT / 1.6f).coerceIn(0f, 1f)) else ball
            drawBall(shown, f.ballR * (1f - 0.5f * bp), bp * 900f)
            drag?.let { (a, b) -> drawLine(Color.White.copy(alpha = 0.7f), a, b, 6f) }
        }

        // scoreboard: a dot per kick, green goal, red not
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp)
            .background(Color(0xCC000000), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${stringResource(R.string.x_pk_you)}  ${game.playerGoals} – ${game.cpuGoals}  ${stringResource(R.string.x_pk_cpu)}",
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Dots(game.kicks.filter { it.byPlayer }); Dots(game.kicks.filter { !it.byPlayer })
            }
        }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
            Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color.White)
        }
        val msg = when (phase) {
            Phase.AIM -> stringResource(R.string.x_pk_shoot)
            Phase.RUNUP, Phase.FLY -> if (game.playerShoots) null else stringResource(R.string.x_pk_keep)
            Phase.RESULT -> when (result) {
                Penalty.Result.GOAL -> "⚽ " + stringResource(R.string.x_pk_goal)
                Penalty.Result.SAVED -> "🧤 " + stringResource(R.string.x_pk_saved)
                else -> stringResource(R.string.x_pk_miss)
            }
            Phase.END -> null
        }
        if (msg != null) Text(msg, color = Color.White, fontSize = if (phase == Phase.RESULT) 34.sp else 18.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(24.dp)
                .background(Color(0x88000000), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 8.dp))
        if (phase == Phase.END) Column(Modifier.align(Alignment.Center).background(Color(0xDD000000), RoundedCornerShape(24.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (game.playerWon) "🏆 " + stringResource(R.string.x_pk_won) else stringResource(R.string.x_pk_lost),
                color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("${game.playerGoals} – ${game.cpuGoals}", color = Color.White, fontSize = 22.sp)
            Button(onClick = { game = Penalty(); phase = Phase.AIM; phaseT = 0f; result = null; dive = null }) {
                Text(stringResource(R.string.x_pk_again))
            }
        }
        Text(stringResource(R.string.x_pk_title), color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp))
    }
}

private const val RUNUP = 0.9f
private const val FLIGHT = 0.75f

@Composable
private fun Dots(kicks: List<Penalty.Kick>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
        for (i in 0 until maxOf(Penalty.ROUNDS, kicks.size)) {
            val k = kicks.getOrNull(i)
            Box(Modifier.size(12.dp).clip(CircleShape).background(when {
                k == null -> Color(0x55FFFFFF)
                k.result == Penalty.Result.GOAL -> Color(0xFF00E676)
                else -> Color(0xFFFF5252)
            }))
        }
    }
}

private fun DrawScope.drawPitch(f: Field) {
    // mowing stripes
    val stripes = 10
    for (i in 0 until stripes) drawRect(if (i % 2 == 0) Color(0xFF2E7D32) else Color(0xFF388E3C),
        Offset(0f, size.height * i / stripes), Size(size.width, size.height / stripes + 1))
    // crowd behind the goal
    drawRect(Color(0xFF263238), Offset.Zero, Size(size.width, f.goalBottom - f.goalH * 1.25f))
    val dot = f.goalW * 0.018f
    var y = dot
    var row = 0
    while (y < f.goalBottom - f.goalH * 1.3f) {
        var x = (row % 2) * dot * 1.5f
        while (x < size.width) {
            drawCircle(listOf(Color(0xFFFFCC80), Color(0xFFE57373), Color(0xFF90CAF9), Color(0xFFFFFFFF))[((x / dot).toInt() + row) % 4], dot * 0.8f, Offset(x, y))
            x += dot * 3f
        }
        y += dot * 2.6f; row++
    }
    val line = Color.White.copy(alpha = 0.85f)
    // goal line, six-yard and penalty boxes, penalty spot
    drawLine(line, Offset(0f, f.goalBottom), Offset(size.width, f.goalBottom), 4f)
    val six = f.goalW * 0.25f
    drawRect(line, Offset(f.goalLeft - six, f.goalBottom), Size(f.goalW + 2 * six, f.goalH * 0.55f), style = Stroke(4f))
    val big = f.goalW * 0.75f
    drawRect(line, Offset(f.goalLeft - big, f.goalBottom), Size(f.goalW + 2 * big, f.spot.y - f.goalBottom + f.goalH * 0.6f), style = Stroke(4f))
    drawCircle(line, 6f, f.spot)
    // net and goal
    val net = Color.White.copy(alpha = 0.25f)
    val top = f.goalBottom - f.goalH
    var nx = f.goalLeft
    while (nx <= f.goalLeft + f.goalW) { drawLine(net, Offset(nx, top), Offset(nx, f.goalBottom), 2f); nx += f.goalW / 24 }
    var ny = top
    while (ny <= f.goalBottom) { drawLine(net, Offset(f.goalLeft, ny), Offset(f.goalLeft + f.goalW, ny), 2f); ny += f.goalH / 8 }
    val post = f.goalW * 0.018f
    drawRect(Color.White, Offset(f.goalLeft - post, top - post), Size(f.goalW + 2 * post, post))
    drawRect(Color.White, Offset(f.goalLeft - post, top - post), Size(post, f.goalH + post))
    drawRect(Color.White, Offset(f.goalLeft + f.goalW, top - post), Size(post, f.goalH + post))
}

/** A simple keeper: yellow shirt (purple when it is you), arms up, leaning as he dives. */
private fun DrawScope.drawKeeper(f: Field, at: Offset, lean: Float, mine: Boolean) {
    val s = f.goalH
    val shirt = if (mine) Color(0xFF7C4DFF) else Color(0xFFFFD600)
    rotate(lean, at) {
        drawRoundRect(Color(0xFF212121), Offset(at.x - s * 0.09f, at.y + s * 0.12f), Size(s * 0.18f, s * 0.28f), CornerRadius(s * 0.04f))   // legs
        drawRoundRect(shirt, Offset(at.x - s * 0.12f, at.y - s * 0.18f), Size(s * 0.24f, s * 0.34f), CornerRadius(s * 0.06f))              // body
        drawLine(shirt, Offset(at.x - s * 0.1f, at.y - s * 0.12f), Offset(at.x - s * 0.3f, at.y - s * 0.38f), s * 0.07f)                   // arms
        drawLine(shirt, Offset(at.x + s * 0.1f, at.y - s * 0.12f), Offset(at.x + s * 0.3f, at.y - s * 0.38f), s * 0.07f)
        drawCircle(Color.White, s * 0.06f, Offset(at.x - s * 0.31f, at.y - s * 0.4f))                                                       // gloves
        drawCircle(Color.White, s * 0.06f, Offset(at.x + s * 0.31f, at.y - s * 0.4f))
        drawCircle(Color(0xFFFFCC80), s * 0.08f, Offset(at.x, at.y - s * 0.27f))                                                           // head
    }
}

private fun DrawScope.drawBall(c: Offset, r: Float, spin: Float) {
    drawCircle(Color(0x44000000), r, Offset(c.x + r * 0.2f, c.y + r * 0.35f))
    drawCircle(Color.White, r, c)
    rotate(spin, c) {
        drawCircle(Color(0xFF212121), r * 0.32f, c)
        for (i in 0 until 5) {
            val a = Math.toRadians(i * 72.0)
            drawCircle(Color(0xFF212121), r * 0.18f, Offset(c.x + (kotlin.math.cos(a) * r * 0.78f).toFloat(), c.y + (kotlin.math.sin(a) * r * 0.78f).toFloat()))
        }
    }
    drawCircle(Color(0xFF9E9E9E), r, c, style = Stroke(r * 0.06f))
}
