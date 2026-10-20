/* Added for AEmulator X, 2026-10-20: Instant replay viewer. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.aemu.R
import app.aemu.core.ReplayRing
import kotlinx.coroutines.delay

/**
 * Plays back the last seconds of the guest screen ([frames]: JPEG, oldest first) in a loop: pause, scrub, and save
 * it as a GIF. Only the picture: the running system itself does not go back in time.
 */
@Composable
fun ReplayDialog(frames: List<ByteArray>, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var at by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(playing, frames) {
        while (playing && frames.isNotEmpty()) {
            delay(1000L / ReplayRing.FPS)
            at = (at + 1) % frames.size
        }
    }
    val bmp = remember(at, frames) { frames.getOrNull(at)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(16.dp).widthIn(max = 560.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⏪ " + stringResource(R.string.x_rp_title), style = MaterialTheme.typography.titleLarge)
                Box(Modifier.fillMaxWidth().heightIn(max = 520.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                    if (bmp != null) Image(bmp.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp))
                    else Text(stringResource(R.string.x_rp_empty), color = Color.White, modifier = Modifier.padding(32.dp))
                }
                if (frames.isNotEmpty()) {
                    val secsAgo = (frames.size - 1 - at).toFloat() / ReplayRing.FPS
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { playing = !playing }) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null) }
                        Slider(at.toFloat(), { at = it.toInt().coerceIn(0, frames.size - 1); playing = false },
                            valueRange = 0f..(frames.size - 1).coerceAtLeast(1).toFloat(), modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.x_rp_ago, "%.1f".format(secsAgo)), style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.x_km_done)) }
                    Button(enabled = frames.isNotEmpty() && !saving, onClick = {
                        saving = true
                        kotlin.concurrent.thread(name = "aemu-replay-gif") {
                            val g = app.aemu.core.GifRecorder(ReplayRing.FPS)
                            for (f in frames) BitmapFactory.decodeByteArray(f, 0, f.size)?.let { g.add(it); it.recycle() }
                            val bytes = runCatching { g.encode() }.getOrNull()
                            val name = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(java.util.Date()) + "-replay.gif"
                            val saved = bytes?.let { app.aemu.core.Screenshot.savePicture(ctx, it, name, "image/gif") }
                            (ctx as? android.app.Activity)?.runOnUiThread {
                                saving = false
                                android.widget.Toast.makeText(ctx, if (saved != null) ctx.getString(R.string.x_gif_saved, saved) else ctx.getString(R.string.x_gif_failed),
                                    android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Text(stringResource(if (saving) R.string.x_gif_saving else R.string.x_rp_save)) }
                }
            }
        }
    }
}
