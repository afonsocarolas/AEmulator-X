/* Added for AEmulator X, 2026-10-29: boot race. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.BootRace
import app.aemu.core.GuestImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The boot race leaderboard: your firmwares from the fastest boot to the slowest, with medals for the top three. */
@Composable
fun BootRaceDialog(images: List<GuestImage>, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val rows by produceState<List<BootRace.Row>?>(null, images) {
        value = withContext(Dispatchers.IO) {
            val byId = images.associateBy { it.id }
            BootRace.leaderboard(BootRace.read(ctx), images.filter { it.lastBootMs > 0 }.associate { it.id to it.lastBootMs })
                .filter { it.id in byId }
        }
    }
    val byId = images.associateBy { it.id }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🏁 " + stringResource(R.string.x_br_title)) },
        text = {
            val list = rows
            when {
                list == null -> Text("…")
                list.isEmpty() -> Text(stringResource(R.string.x_br_empty))
                else -> {
                    val slowest = list.maxOf { it.best }.coerceAtLeast(1)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.x_br_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(list, key = { _, r -> r.id }) { i, r ->
                                val img = byId.getValue(r.id)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${i + 1}." }, fontSize = if (i < 3) 24.sp else 16.sp,
                                        fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(img.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f))
                                            Text(BootRace.seconds(r.best), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                                        }
                                        // the bar: the faster, the shorter (relative to the slowest of the list)
                                        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                                            Box(Modifier.fillMaxWidth(r.best.toFloat() / slowest).fillMaxHeight()
                                                .background(XBrand.gradient, RoundedCornerShape(3.dp)))
                                        }
                                        Text("${AndroidEras.name(img.api)} · " + stringResource(R.string.x_br_detail, BootRace.seconds(r.last), r.count),
                                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
