/* Added for AEmulator X, 2026-10-22: firmware collections. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.core.FirmwareCollections
import app.aemu.core.GuestImage

/**
 * A row of chips over the library: All, then each collection (tap to show only it, long-press to edit), then
 * "+ New collection". [selected] is the name of the collection shown, null for all.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionChips(images: List<GuestImage>, selected: String?, onSelect: (String?) -> Unit) {
    val ctx = LocalContext.current
    val list = remember(FirmwareCollections.rev) { FirmwareCollections.all(ctx) }
    var editing by remember { mutableStateOf<FirmwareCollections.Collection?>(null) }
    var creating by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(stringResource(R.string.x_col_all)) })
        for (c in list) {
            // drawn like a FilterChip, which has no long press of its own
            val on = selected == c.name
            val shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            Box(Modifier.heightIn(min = 32.dp).clip(shape)
                .background(if (on) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                .border(1.dp, if (on) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.outline, shape)
                .combinedClickable(onClick = { onSelect(c.name) }, onLongClick = { editing = c })
                .padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text("📁 ${c.name} · ${c.ids.count { id -> images.any { it.id == id } }}", style = MaterialTheme.typography.labelLarge,
                    color = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        FilterChip(selected = false, onClick = { creating = true }, label = { Text("+ " + stringResource(R.string.x_col_new)) })
    }
    if (creating) CollectionEditor(null, images, onDone = { creating = false }, onSaved = { onSelect(it) })
    editing?.let { c ->
        CollectionEditor(c, images, onDone = { editing = null }, onSaved = { if (selected == c.name) onSelect(it) },
            onDeleted = { if (selected == c.name) onSelect(null) })
    }
}

/** Name and members of a collection (new when [c] is null). */
@Composable
private fun CollectionEditor(
    c: FirmwareCollections.Collection?, images: List<GuestImage>, onDone: () -> Unit,
    onSaved: (String) -> Unit, onDeleted: () -> Unit = {},
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(c?.name.orEmpty()) }
    var ids by remember { mutableStateOf(c?.ids ?: emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(if (c == null) R.string.x_col_new else R.string.x_col_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(FirmwareCollections.MAX_NAME) }, singleLine = true,
                    label = { Text(stringResource(R.string.name)) }, placeholder = { Text(stringResource(R.string.x_col_hint)) })
                Text(stringResource(R.string.x_col_pick), style = MaterialTheme.typography.labelLarge)
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    for (img in images.sortedBy { it.name.lowercase() }) Row(Modifier.fillMaxWidth().clickable {
                        ids = if (img.id in ids) ids - img.id else ids + img.id
                    }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(img.id in ids, onCheckedChange = null, modifier = Modifier.padding(8.dp))
                        Column {
                            Text(img.name, style = MaterialTheme.typography.bodyLarge)
                            Text(img.displayVersion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (c != null) TextButton(onClick = { confirmDelete = true }) {
                    Text(stringResource(R.string.x_col_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(enabled = FirmwareCollections.cleanName(name).isNotEmpty(), onClick = {
                FirmwareCollections.save(ctx, c?.name, name, ids)
                onSaved(FirmwareCollections.cleanName(name)); onDone()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.cancel)) } },
    )
    if (confirmDelete && c != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.x_col_delete)) },
        text = { Text(stringResource(R.string.x_col_delete_q, c.name)) },
        confirmButton = { Button(onClick = { FirmwareCollections.delete(ctx, c.name); onDeleted(); confirmDelete = false; onDone() }) {
            Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}
