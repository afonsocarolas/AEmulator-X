/* Added for AEmulator X, 2026-10-21: daily challenge and Android birthday cards. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.AndroidBirthdays
import app.aemu.core.DailyChallenge
import app.aemu.core.DailyPick
import app.aemu.core.GuestImage
import app.aemu.core.UiSounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.sin
import kotlin.random.Random

/** Today's Android birthdays (if any), then today's challenge. [refresh] changes when the library comes back into view. */
@Composable
fun DailyCards(images: List<GuestImage>, refresh: Int, onStart: (GuestImage) -> Unit) {
    val date = remember(refresh) { LocalDate.now() }
    for (b in AndroidBirthdays.on(date)) BirthdayCard(b, date, images, onStart)
    ChallengeCard(images, refresh, date)
}

@Composable
private fun BirthdayCard(b: AndroidBirthdays.Birthday, date: LocalDate, images: List<GuestImage>, onStart: (GuestImage) -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { app.aemu.AppPrefs.prefs(ctx) }
    val key = "x_bday_" + b.name
    // the party sound once per birthday per year
    LaunchedEffect(b, date) {
        if (prefs.getInt(key, 0) != date.year) { prefs.edit().putInt(key, date.year).apply(); UiSounds.play(ctx, UiSounds.Sfx.PARTY) }
    }
    val mine = images.filter { it.baseId.isEmpty() }.firstOrNull { b.group >= 0 && DailyPick.factGroup(it.api) == b.group }
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Box {
            Confetti(Modifier.matchParentSize(), seconds = 6)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🎂🎉 " + stringResource(R.string.x_bd_title), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(stringResource(R.string.x_bd_happy, AndroidBirthdays.age(b, date), b.name) + " " + b.emoji,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(stringResource(R.string.x_bd_since, b.born.year), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                if (mine != null) Button(onClick = { onStart(mine) }) { Text(stringResource(R.string.x_bd_party, mine.name)) }
            }
        }
    }
}

private class ChallengeState(val challenge: DailyChallenge.Challenge, val progress: Int, val streak: Int, val best: Int, val justDone: Boolean)

@Composable
private fun ChallengeCard(images: List<GuestImage>, refresh: Int, date: LocalDate) {
    val ctx = LocalContext.current
    val systems = images.filter { it.baseId.isEmpty() }
    if (systems.isEmpty()) return
    val day = date.toEpochDay()
    val apis = remember(systems) { systems.map { it.api } }
    val st by produceState<ChallengeState?>(null, refresh, apis) {
        value = withContext(Dispatchers.IO) {
            val c = DailyChallenge.pick(day, apis) ?: return@withContext null
            val p = DailyChallenge.progress(c, DailyChallenge.today(ctx, day), DailyChallenge.playMsToday(ctx, day))
            val justDone = p >= c.target && DailyChallenge.markDone(ctx, day)
            val done = DailyChallenge.doneDays(ctx)
            ChallengeState(c, p, DailyChallenge.streak(done, day), DailyChallenge.bestStreak(done), justDone)
        }
    }
    val s = st ?: return
    var celebrate by remember { mutableStateOf(false) }
    LaunchedEffect(s.justDone) { if (s.justDone) { celebrate = true; UiSounds.play(ctx, UiSounds.Sfx.CHALLENGE) } }
    val done = s.progress >= s.challenge.target
    val (next, inDays) = remember(date) { AndroidBirthdays.next(date) }
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Box {
            if (celebrate) Confetti(Modifier.matchParentSize(), seconds = 4)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆 " + stringResource(R.string.x_dc_title), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    if (s.streak > 0) Text("🔥 " + stringResource(R.string.x_dc_streak, s.streak), style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFF7043), fontWeight = FontWeight.Bold)
                }
                Text(s.challenge.emoji + "  " + DailyChallenge.title(ctx, s.challenge) { AndroidEras.name(it) }, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(progress = { s.progress.toFloat() / s.challenge.target }, modifier = Modifier.weight(1f))
                    Text(if (done) "✅" else "${s.progress} / ${s.challenge.target}", style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 12.dp))
                }
                Text(stringResource(if (done) R.string.x_dc_done else R.string.x_dc_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (s.best > 1) Text(stringResource(R.string.x_dc_best, s.best), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("🎂 " + stringResource(R.string.x_bd_next, next.name, AndroidBirthdays.age(next, date.plusDays(inDays)), inDays.toInt()),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Paper bits falling over the card for [seconds]. */
@Composable
fun Confetti(modifier: Modifier, seconds: Int) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(seconds * 1000, easing = LinearEasing)) }
    val bits = remember {
        val r = Random(System.nanoTime())
        val colors = listOf(Color(0xFFFF1744), Color(0xFFFFD600), Color(0xFF00E676), Color(0xFF2979FF), Color(0xFFE040FB), Color(0xFFFF9100))
        List(70) { floatArrayOf(r.nextFloat(), r.nextFloat() * 0.6f, 0.25f + r.nextFloat() * 0.5f, r.nextFloat() * 360f, r.nextInt(colors.size).toFloat()) } to colors
    }
    Canvas(modifier) {
        val p = t.value
        if (p >= 1f) return@Canvas
        val (list, colors) = bits
        val fade = if (p > 0.8f) (1f - p) / 0.2f else 1f
        for (b in list) {
            val fall = (b[1] + p * seconds * b[2]) % 1.2f - 0.1f
            val x = (b[0] + 0.03f * sin((p * 20f + b[3]).toDouble()).toFloat()) * size.width
            val y = fall * size.height
            rotate(b[3] + p * 720f, Offset(x, y)) {
                drawRect(colors[b[4].toInt()].copy(alpha = fade), Offset(x - 5f, y - 3f), Size(10f, 6f))
            }
        }
    }
}
