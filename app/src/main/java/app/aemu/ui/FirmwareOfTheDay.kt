/* Added for AEmulator X, 2026-10-20: Firmware of the day. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.DailyPick
import app.aemu.core.GuestImage

/**
 * A card at the top of the library: one of your firmwares picked for today, with a fun fact about its Android
 * version and a button to boot it. Hidden for the rest of the day with ×.
 */
@Composable
fun FirmwareOfTheDay(images: List<GuestImage>, onStart: (GuestImage) -> Unit) {
    if (images.size < 2) return                   // with one firmware there is nothing to suggest
    val ctx = LocalContext.current
    val day = remember { DailyPick.today() }
    val prefs = remember { app.aemu.AppPrefs.prefs(ctx) }
    var hidden by remember { mutableStateOf(prefs.getLong("x_fotd_hidden", -1L) == day) }
    if (hidden) return
    val sorted = remember(images) { images.sortedBy { it.id } }
    val img = sorted.getOrNull(DailyPick.index(sorted.size, day)) ?: return
    val facts = stringArrayResource(R.array.x_fotd_facts)
    val fact = facts.getOrNull(DailyPick.factGroup(img.api)) ?: return
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(XBrand.gradient), contentAlignment = Alignment.Center) {
                Text("📅", fontSize = 22.sp)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.x_fotd_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text("${img.name} · ${AndroidEras.name(img.api)}", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text("💡 $fact", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f))
                Button(onClick = { onStart(img) }, modifier = Modifier.padding(top = 4.dp)) { Text(stringResource(R.string.x_fotd_boot)) }
            }
            IconButton(onClick = { hidden = true; prefs.edit().putLong("x_fotd_hidden", day).apply() }) {
                Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

