/* Added for AEmulator X, 2026-10-22: AEmulator Wrapped. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.WrappedStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Your AEmulator X year so far, as a few colourful pages: tap the right side or swipe for the next one. */
class WrappedActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.aemu.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { Wrapped(onClose = ::finish) }
    }
}

private class Page(val colors: List<Color>, val emoji: String, val top: String, val big: String, val bottom: String, val count: Long? = null)

@Composable
private fun Wrapped(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val stats by produceState<WrappedStats?>(null) { value = withContext(Dispatchers.IO) { runCatching { WrappedStats.read(ctx) }.getOrNull() } }
    val s = stats
    Box(Modifier.fillMaxSize().background(Color(0xFF120A2A))) {
        if (s == null) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        else Story(pages(ctx, s), s, onClose)
    }
}

private fun pages(ctx: Context, s: WrappedStats): List<Page> {
    val (h, m) = WrappedStats.hours(s.totalPlayMs)
    val list = ArrayList<Page>()
    list += Page(listOf(Color(0xFF6A3DF0), Color(0xFFE040FB)), "🎁", ctx.getString(R.string.x_wr_intro_top), "AEmulator X",
        ctx.getString(R.string.x_wr_intro_bottom, s.firmwares))
    list += Page(listOf(Color(0xFF0277BD), Color(0xFF00E5FF)), "⏱️", ctx.getString(R.string.x_wr_time_top),
        if (h > 0) "$h h $m min" else "$m min", ctx.getString(if (h >= 10) R.string.x_wr_time_lots else R.string.x_wr_time_some))
    s.top?.let { (name, api, ms) ->
        val (th, tm) = WrappedStats.hours(ms)
        list += Page(listOf(Color(0xFFE64A19), Color(0xFFFFD600)), "🥇", ctx.getString(R.string.x_wr_top_top), name,
            "${AndroidEras.name(api)} · " + (if (th > 0) "$th h $tm min" else "$tm min"))
    }
    s.favouriteApi?.let { api ->
        list += Page(listOf(Color(0xFF2E7D32), Color(0xFF76FF03)), "🕰️", ctx.getString(R.string.x_wr_era_top), AndroidEras.name(api),
            ctx.getString(R.string.x_wr_era_bottom, (s.favouriteShare * 100).toInt()))
    }
    list += Page(listOf(Color(0xFFD81B60), Color(0xFFFF8A80)), "🚀", ctx.getString(R.string.x_wr_boots_top), "", ctx.getString(R.string.x_wr_boots_bottom), count = s.boots.toLong())
    list += Page(listOf(Color(0xFF00838F), Color(0xFF64FFDA)), "📸", ctx.getString(R.string.x_wr_shots_top), "",
        ctx.getString(R.string.x_wr_shots_bottom, s.recordings), count = s.screenshots.toLong())
    list += Page(listOf(Color(0xFFFF6D00), Color(0xFFFFAB40)), "🔥", ctx.getString(R.string.x_wr_streak_top), "",
        ctx.getString(R.string.x_wr_streak_bottom, s.bestChallengeStreak), count = s.bestPlayStreak.toLong())
    list += Page(listOf(Color(0xFF4527A0), Color(0xFFB388FF)), "🏆", ctx.getString(R.string.x_wr_badges_top), "${s.badges} / ${s.badgesTotal}",
        ctx.getString(if (s.badges * 2 >= s.badgesTotal) R.string.x_wr_badges_lots else R.string.x_wr_badges_some))
    list += Page(listOf(Color(0xFF6A3DF0), Color(0xFF18FFFF)), "💜", ctx.getString(R.string.x_wr_end_top), ctx.getString(R.string.x_wr_end_big),
        ctx.getString(R.string.x_wr_end_bottom))
    return list
}

@Composable
private fun Story(pages: List<Page>, s: WrappedStats, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
        val p = pages[i]
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(p.colors))
            .pointerInput(i) {
                detectTapGestures { o ->
                    scope.launch { pager.animateScrollToPage((if (o.x > size.width / 3) i + 1 else i - 1).coerceIn(0, pages.size - 1)) }
                }
            }) {
            Column(Modifier.align(Alignment.Center).padding(32.dp).widthIn(max = 560.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(p.emoji, fontSize = 72.sp)
                Text(p.top, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                if (p.count != null) CountUp(p.count, visible = pager.currentPage == i)
                else Text(p.big, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 48.sp)
                Text(p.bottom, color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp, textAlign = TextAlign.Center)
                if (i == pages.lastIndex) Button(onClick = { share(ctx, s) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF4A22C9))) {
                    Text(stringResource(R.string.x_wr_share), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    // story bars: one per page, filled up to the current one
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 56.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        pages.indices.forEach { i ->
            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = if (i <= pager.currentPage) 0.95f else 0.35f)))
        }
    }
    Box(Modifier.fillMaxSize()) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 0.dp, end = 4.dp)) {
            Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color.White)
        }
        Text("AEmulator X Wrapped", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp))
    }
}

/** A number counting up from 0 when its page comes into view. */
@Composable
private fun CountUp(target: Long, visible: Boolean) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(visible, target) {
        if (visible) { a.snapTo(0f); a.animateTo(target.toFloat(), tween(1400, easing = FastOutSlowInEasing)) }
    }
    Text("${a.value.toLong()}", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black)
}

private fun share(ctx: Context, s: WrappedStats) {
    val (h, m) = WrappedStats.hours(s.totalPlayMs)
    val text = ctx.getString(R.string.x_wr_share_text, s.firmwares, "$h h $m min", s.top?.first ?: "—", s.screenshots, s.badges, s.badgesTotal)
    runCatching {
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
