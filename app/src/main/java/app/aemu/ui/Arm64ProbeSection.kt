/* Added for AEmulator X, 2026-10-16: experimental 64-bit firmware probe. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.core.Arm64Probe
import app.aemu.core.GuestImage
import app.aemu.core.VmPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** "Experimental: 64-bit probe" in the VM settings, only for 64-bit ARM firmware. */
@Composable
internal fun Arm64ProbeSection(img: GuestImage) {
    val ctx = LocalContext.current
    val paths = remember(img.id) { VmPaths(ctx, img.id) }
    if (!remember(img.id) { Arm64Probe.applies(img.abi, paths.root) }) return
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<String?>(null) }

    Text(stringResource(R.string.x_probe_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.x_probe_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FilledTonalButton(enabled = !running, modifier = Modifier.padding(vertical = 8.dp), onClick = {
        running = true
        scope.launch {
            report = withContext(Dispatchers.IO) { runCatching { Arm64Probe.run(paths.root, ctx.cacheDir) }.getOrElse { "probe crashed: $it" } }
            running = false
        }
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (running) LoadingIndicator(Modifier.size(20.dp)) else Icon(Icons.Rounded.Science, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (running) R.string.x_probe_running else R.string.x_probe_run))
        }
    }

    report?.let { text ->
        AlertDialog(
            onDismissRequest = { report = null },
            icon = { Icon(Icons.Rounded.Science, null) },
            title = { Text(stringResource(R.string.x_probe_title)) },
            text = {
                SelectionContainer {
                    Text(text, style = Mono, modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("AEmulator X 64-bit probe", text))
                    Toast.makeText(ctx, R.string.x_probe_copied, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.x_probe_copy)) }
            },
            dismissButton = { TextButton(onClick = { report = null }) { Text(stringResource(R.string.close)) } },
        )
    }
}
