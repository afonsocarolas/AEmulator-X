/* Added for AEmulator X, 2026-10-19: play statistics. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.core.ImageStore
import app.aemu.core.PlayDays
import app.aemu.core.PlayStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.TextStyle
import java.util.Locale

/** Everything the statistics screen shows, worked out off the main thread. */
internal data class StatsData(
    val totalMs: Long,
    val played: Int,
    val perFirmware: List<Pair<String, Long>>,
    val perAndroid: List<Pair<String, Long>>,
    val week: List<Pair<String, Long>>,
    val streak: Int,
    val bestStreak: Int,
)

internal object StatsMath {
    /** name → ms, biggest first, at most [max], zero entries dropped */
    fun top(entries: List<Pair<String, Long>>, max: Int): List<Pair<String, Long>> =
        entries.filter { it.second > 0 }.groupBy({ it.first }, { it.second }).map { (k, v) -> k to v.sum() }
            .sortedByDescending { it.second }.take(max)
}

@Composable
fun StatsDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val data by produceState<StatsData?>(null) {
        value = withContext(Dispatchers.IO) {
            val imgs = runCatching { ImageStore.list(ctx) }.getOrDefault(emptyList())
            val per = imgs.map { it to PlayStats.read(ctx, it.id).totalMs }
            val days = PlayDays.read(ctx)
            StatsData(
                totalMs = per.sumOf { it.second },
                played = per.count { it.second > 0 },
                perFirmware = StatsMath.top(per.map { it.first.name to it.second }, 8),
                perAndroid = StatsMath.top(per.map { AndroidEras.name(it.first.api) to it.second }, 8),
                week = PlayDays.lastWeek(days).map { (d, ms) -> d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to ms },
                streak = PlayDays.currentStreak(days),
                bestStreak = PlayDays.bestStreak(days),
            )
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.st_title)) },
        text = {
            val d = data
            if (d == null) Text("…") else Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Big(PlayStats.duration(d.totalMs), stringResource(R.string.st_total))
                    Big("🔥 ${d.streak}", stringResource(R.string.st_streak))
                    Big("🏅 ${d.bestStreak}", stringResource(R.string.st_best))
                }
                Text(stringResource(R.string.st_week), style = MaterialTheme.typography.titleSmall)
                WeekBars(d.week)
                if (d.perFirmware.isNotEmpty()) {
                    Text(stringResource(R.string.st_firmwares, d.played), style = MaterialTheme.typography.titleSmall)
                    Bars(d.perFirmware)
                    Text(stringResource(R.string.st_android), style = MaterialTheme.typography.titleSmall)
                    Bars(d.perAndroid)
                } else Text(stringResource(R.string.st_empty), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun Big(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Horizontal bars: label, bar scaled to the biggest, time. */
@Composable
private fun Bars(rows: List<Pair<String, Long>>) {
    val max = rows.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val color = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for ((name, ms) in rows) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.weight(1f).height(14.dp)) {
                Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                    drawRoundRect(color, size = Size(size.width * ms / max, size.height), cornerRadius = CornerRadius(7f, 7f))
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(PlayStats.duration(ms), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Seven columns, one per day, today on the right. */
@Composable
private fun WeekBars(days: List<Pair<String, Long>>) {
    val max = days.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val color = MaterialTheme.colorScheme.tertiary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    Column {
        Canvas(Modifier.fillMaxWidth().height(70.dp)) {
            val slot = size.width / days.size
            days.forEachIndexed { i, (_, ms) ->
                val h = if (ms == 0L) 4f else (size.height * ms / max).coerceAtLeast(6f)
                drawRoundRect(if (ms == 0L) empty else color, topLeft = Offset(i * slot + slot * 0.2f, size.height - h),
                    size = Size(slot * 0.6f, h), cornerRadius = CornerRadius(6f, 6f))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            days.forEach { (label, _) -> Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
    }
}
