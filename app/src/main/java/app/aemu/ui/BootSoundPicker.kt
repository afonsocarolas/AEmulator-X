/* Added for AEmulator X, 2026-10-15: boot sound picker (X2). GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.core.BootSound
import app.aemu.core.GuestImage
import app.aemu.core.VmPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Human name of a [BootSound] value. */
@Composable
private fun bootSoundLabel(value: String): String = when {
    value == BootSound.FIRMWARE -> stringResource(R.string.x_bs_firmware)
    value == BootSound.NONE -> stringResource(R.string.x_bs_none)
    value == BootSound.CHIME -> stringResource(R.string.x_bs_chime)
    value == BootSound.RETRO -> stringResource(R.string.x_bs_retro)
    value == BootSound.RISE -> stringResource(R.string.x_bs_rise)
    BootSound.isFw(value) -> BootSound.fwPath(value).substringAfterLast('/')
    BootSound.isFile(value) -> BootSound.fileName(value)
    else -> value
}

/** "Boot sound" row of the VM settings sheet; opens [BootSoundDialog]. */
@Composable
internal fun BootSoundSection(img: GuestImage, value: String, set: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Text(stringResource(R.string.x_bs_title), style = MaterialTheme.typography.titleMedium)
    ListItem(
        headlineContent = { Text(bootSoundLabel(value), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(stringResource(R.string.x_bs_sub)) },
        leadingContent = { XBadgeIcon() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { open = true },
    )
    if (open) BootSoundDialog(img, value, onDismiss = { open = false }) { set(it); open = false }
}

@Composable
private fun XBadgeIcon() {
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.MusicNote, null, tint = XBrand.Magenta)
    }
}

@Composable
private fun BootSoundDialog(img: GuestImage, current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    val paths = remember(img.id) { VmPaths(ctx, img.id) }
    val scope = rememberCoroutineScope()
    var choice by remember { mutableStateOf(current) }
    var playing by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<BootSound.Playing?>(null) }
    var fwSounds by remember { mutableStateOf<List<String>?>(null) }
    // the custom entry shows the stored file's name; a new pick replaces it
    var customValue by remember { mutableStateOf(current.takeIf { BootSound.isFile(it) && BootSound.customFile(paths).isFile }) }

    fun stopPreview() { player?.stop(); player = null; playing = null }
    fun preview(v: String) {
        val again = playing == v
        stopPreview()
        if (again) return
        playing = v
        player = BootSound.play(paths, v) { scope.launch { if (playing == v) { playing = null; player = null } } }
        if (player == null) playing = null
    }
    DisposableEffect(Unit) { onDispose { player?.stop() } }
    LaunchedEffect(img.id) { fwSounds = withContext(Dispatchers.IO) { runCatching { BootSound.firmwareSounds(paths.root) }.getOrDefault(emptyList()) } }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        stopPreview()
        scope.launch {
            val v = withContext(Dispatchers.IO) { BootSound.importCustom(ctx, paths, uri) }
            if (v == null) Toast.makeText(ctx, R.string.x_bs_import_failed, Toast.LENGTH_SHORT).show()
            else { customValue = v; choice = v }
        }
    }

    @Composable
    fun Option(v: String, label: String, sub: String? = null, canPreview: Boolean = true) {
        Row(Modifier.fillMaxWidth().clickable { choice = v }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = choice == v, onClick = { choice = v })
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (canPreview) IconButton(onClick = { preview(v) }) {
                Icon(if (playing == v) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, stringResource(R.string.x_bs_preview),
                    tint = if (playing == v) XBrand.Rec else MaterialTheme.colorScheme.primary)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { stopPreview(); onDismiss() },
        icon = { Icon(Icons.Rounded.MusicNote, null) },
        title = { Text(stringResource(R.string.x_bs_title)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                item { Text(stringResource(R.string.x_bs_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp)) }
                item { Option(BootSound.FIRMWARE, stringResource(R.string.x_bs_firmware), canPreview = false) }
                item { Option(BootSound.NONE, stringResource(R.string.x_bs_none), canPreview = false) }
                item { Header(stringResource(R.string.x_bs_builtin)) }
                items(BootSound.BUILTIN) { v -> Option(v, bootSoundLabel(v)) }
                item { Header(stringResource(R.string.x_bs_custom_header)) }
                item {
                    customValue?.let { Option(it, BootSound.fileName(it)) }
                    TextButton(onClick = { pickFile.launch(arrayOf("audio/*", "application/ogg")) }) {
                        Icon(Icons.Rounded.FolderOpen, null)
                        Text(stringResource(R.string.x_bs_custom), Modifier.padding(start = 8.dp))
                    }
                }
                item { Header(stringResource(R.string.x_bs_from_fw)) }
                val fw = fwSounds
                when {
                    fw == null -> item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(8.dp)) {
                            LoadingIndicator(Modifier.size(28.dp)); Text(stringResource(R.string.x_bs_scanning))
                        }
                    }
                    fw.isEmpty() -> item { Text(stringResource(R.string.x_bs_no_fw), style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)) }
                    else -> items(fw, key = { it }) { path ->
                        Option("fw:$path", path.substringAfterLast('/'), path.substringBeforeLast('/'))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { stopPreview(); onPick(choice) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = { stopPreview(); onDismiss() }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun Header(text: String) {
    Column(Modifier.padding(top = 10.dp, bottom = 2.dp)) {
        HorizontalDivider()
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
    }
}
