/* Added for AEmulator X, 2026-10-22: firmware info card. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.core.DeviceMuseum
import app.aemu.core.FirmwareCollections
import app.aemu.core.FirmwareNotes
import app.aemu.core.GuestImage
import app.aemu.core.PlayStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Everything about one firmware on one card: Android, size, screen, boots, play time, notes, collections. */
@Composable
fun FirmwareInfoDialog(img: GuestImage, baseName: String?, onStart: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val stats by produceState(PlayStats.Stats(), img.id) { value = withContext(Dispatchers.IO) { PlayStats.read(ctx, img.id) } }
    val note = remember(img.id, FirmwareNotes.rev) { FirmwareNotes.read(ctx, img.id) }
    val collections = remember(img.id, FirmwareCollections.rev) { FirmwareCollections.all(ctx).filter { img.id in it.ids }.map { it.name } }
    val twin = remember(img.api) { DeviceMuseum.ALL.firstOrNull { DeviceMuseum.matches(it, listOf(img.api)) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(img.name, fontWeight = FontWeight.Bold)
                Text("${AndroidEras.name(img.api)} · API ${img.api}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow("🤖", stringResource(R.string.x_info_android), img.displayVersion)
                InfoRow("🧠", stringResource(R.string.x_info_cpu), if (img.abi.startsWith("arm64")) "64-bit (${img.abi})" else "32-bit (${img.abi})")
                InfoRow("📱", stringResource(R.string.x_info_screen), "${img.settings.width}×${img.settings.height} · ${img.settings.density} dpi")
                InfoRow("💾", stringResource(R.string.x_info_size), Formatter.formatShortFileSize(ctx, img.sizeBytes))
                if (baseName != null) InfoRow("🧩", stringResource(R.string.x_info_base), baseName)
                HorizontalDivider()
                InfoRow("🚀", stringResource(R.string.x_info_boots), if (img.bootCount > 0) "${img.bootCount}" else stringResource(R.string.x_info_never))
                if (img.lastBootMs > 0) InfoRow("⚡", stringResource(R.string.x_info_boot_time), "%.1f s".format(img.lastBootMs / 1000f))
                InfoRow("⏱️", stringResource(R.string.x_info_played), if (stats.totalMs >= 1000) PlayStats.duration(stats.totalMs) else "—")
                if (stats.lastPlayedAt > 0) InfoRow("🕒", stringResource(R.string.x_info_last),
                    DateUtils.getRelativeTimeSpanString(stats.lastPlayedAt).toString())
                if (img.createdAt > 0) InfoRow("📥", stringResource(R.string.x_info_added),
                    DateUtils.formatDateTime(ctx, img.createdAt, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR))
                if (collections.isNotEmpty() || note.isNotEmpty() || twin != null) HorizontalDivider()
                if (collections.isNotEmpty()) InfoRow("📁", stringResource(R.string.x_info_collections), collections.joinToString(", "))
                if (twin != null) InfoRow("🏛️", stringResource(R.string.x_info_twin), "${twin.name} (${twin.year})")
                if (note.isNotEmpty()) Text("📝 $note", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer).padding(10.dp))
            }
        },
        confirmButton = { Button(onClick = { onDismiss(); onStart() }) { Text(stringResource(R.string.start)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun InfoRow(emoji: String, label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("$emoji  $label", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f).padding(start = 8.dp))
    }
}
