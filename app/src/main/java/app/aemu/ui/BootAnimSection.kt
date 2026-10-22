/* Added for AEmulator X, 2026-10-17: custom boot animation. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.aemu.R
import app.aemu.core.BootAnimation
import app.aemu.core.GuestImage
import app.aemu.core.VmPaths
import kotlin.concurrent.thread

/** "Boot animation": the firmware's own, AEmulator X's, or a picked bootanimation.zip (applied at the next boot). */
@Composable
internal fun BootAnimSection(img: GuestImage, value: String, set: (String) -> Unit) {
    val ctx = LocalContext.current
    val paths = remember(img.id) { VmPaths(ctx, img.id) }
    var open by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    val main = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        thread {
            val ok = BootAnimation.importCustom(ctx, paths, uri)
            main.post {
                if (ok) set(BootAnimation.CUSTOM) else Toast.makeText(ctx, R.string.x_bootanim_bad, Toast.LENGTH_LONG).show()
            }
        }
    }
    val label = when (value) {
        BootAnimation.X -> stringResource(R.string.x_bootanim_x)
        BootAnimation.CUSTOM -> stringResource(R.string.x_bootanim_custom)
        else -> stringResource(R.string.x_bootanim_firmware)
    }
    ListItem(
        leadingContent = { Icon(Icons.Rounded.Animation, null) },
        headlineContent = { Text(stringResource(R.string.x_bootanim_title)) },
        supportingContent = { Text(stringResource(R.string.x_bootanim_sub)) },
        trailingContent = {
            Box {
                TextButton(onClick = { open = true }) { Text(label) }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.x_bootanim_firmware)) }, onClick = { open = false; set(BootAnimation.FIRMWARE) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.x_bootanim_x)) }, onClick = { open = false; set(BootAnimation.X) })
                    DropdownMenuItem(text = { Text("🎬 " + stringResource(R.string.x_bl_menu)) }, onClick = { open = false; creating = true })
                    DropdownMenuItem(text = { Text(stringResource(R.string.x_bootanim_pick)) },
                        onClick = { open = false; pick.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) })
                    if (BootAnimation.customFile(paths).isFile) DropdownMenuItem(text = { Text(stringResource(R.string.x_bootanim_custom)) },
                        onClick = { open = false; set(BootAnimation.CUSTOM) })
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    )
    if (creating) BootLogoCreator(img, onDismiss = { creating = false }, onSaved = { creating = false; set(BootAnimation.CUSTOM) })
}
