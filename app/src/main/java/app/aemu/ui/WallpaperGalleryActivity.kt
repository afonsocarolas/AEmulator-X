/* Added for AEmulator X, 2026-10-22: classic wallpaper gallery. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.core.FirmwareWallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

/** The wallpapers inside your firmwares, by firmware; tap one to use it in the library, on this device, or save it. */
class WallpaperGalleryActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.aemu.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AemuTheme { Gallery(onClose = ::finish) } }
    }
}

/** A small version of a wallpaper, decoded off the main thread; kept while the screen is open. */
private val thumbs = android.util.LruCache<String, ImageBitmap>(48)

private fun decode(bytes: ByteArray, max: Int): ImageBitmap? {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
    var sample = 1
    while (maxOf(o.outWidth, o.outHeight) / (sample * 2) >= max) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}

@Composable
private fun Gallery(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val all by produceState<List<FirmwareWallpapers.Wallpaper>?>(null) { value = withContext(Dispatchers.IO) { FirmwareWallpapers.scanAll(ctx) } }
    var open by remember { mutableStateOf<FirmwareWallpapers.Wallpaper?>(null) }
    AuroraBackground(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🖼️ " + stringResource(R.string.x_wg_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.x_wg_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) }
            }
            val list = all
            when {
                list == null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.x_wg_looking), modifier = Modifier.padding(top = 12.dp))
                }
                list.isEmpty() -> Text(stringResource(R.string.x_wg_none), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(32.dp))
                else -> LazyVerticalGrid(GridCells.Adaptive(140.dp), contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                    for ((fw, items) in list.groupBy { it.firmwareId }) {
                        val first = items.first()
                        item(key = "h$fw", span = { GridItemSpan(maxLineSpan) }) {
                            XSectionHeader("${first.firmware} · ${AndroidEras.name(first.api)}", Modifier.padding(top = 8.dp))
                        }
                        items(items.size, key = { items[it].key }) { i -> Thumb(items[i]) { open = items[i] } }
                    }
                }
            }
        }
    }
    open?.let { w -> WallpaperDialog(w) { open = null } }
}

@Composable
private fun Thumb(w: FirmwareWallpapers.Wallpaper, onClick: () -> Unit) {
    val img by produceState(thumbs.get(w.key), w.key) {
        if (value == null) value = withContext(Dispatchers.IO) { w.bytes()?.let { decode(it, 360) } }?.also { thumbs.put(w.key, it) }
    }
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center) {
            img?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                ?: CircularProgressIndicator(Modifier.padding(24.dp))
        }
        Text(w.name.substringBeforeLast('.'), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp))
    }
}

/** Big preview and what to do with it. */
@Composable
private fun WallpaperDialog(w: FirmwareWallpapers.Wallpaper, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val big by produceState<ImageBitmap?>(null, w.key) { value = withContext(Dispatchers.IO) { w.bytes()?.let { decode(it, 1200) } } }
    var busy by remember { mutableStateOf(false) }
    fun act(work: (ByteArray) -> Boolean, ok: Int) {
        busy = true
        thread {
            val done = w.bytes()?.let { runCatching { work(it) }.getOrDefault(false) } ?: false
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                busy = false
                Toast.makeText(ctx, if (done) ok else R.string.x_wg_failed, Toast.LENGTH_SHORT).show()
                if (done) onDismiss()
            }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Column {
                Text(w.name.substringBeforeLast('.'), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${w.firmware} · ${AndroidEras.name(w.api)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    big?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth()) } ?: CircularProgressIndicator()
                }
                Button(enabled = !busy, onClick = { act({ LibraryWallpaper.saveBytes(ctx, it) }, R.string.x_wg_library_ok) }, modifier = Modifier.fillMaxWidth()) {
                    Text("📚 " + stringResource(R.string.x_wg_library))
                }
                FilledTonalButton(enabled = !busy, onClick = {
                    act({ b -> WallpaperManager.getInstance(ctx).setStream(java.io.ByteArrayInputStream(b)); true }, R.string.x_wg_device_ok)
                }, modifier = Modifier.fillMaxWidth()) { Text("📱 " + stringResource(R.string.x_wg_device)) }
                FilledTonalButton(enabled = !busy, onClick = {
                    act({ b ->
                        val ext = w.name.substringAfterLast('.', "jpg").lowercase()
                        val mime = when (ext) { "png" -> "image/png"; "webp" -> "image/webp"; else -> "image/jpeg" }
                        app.aemu.core.Screenshot.savePicture(ctx, b, "wallpaper-${w.firmware.replace(Regex("[^A-Za-z0-9]+"), "_")}-${w.name}", mime) != null
                    }, R.string.x_wg_saved)
                }, modifier = Modifier.fillMaxWidth()) { Text("💾 " + stringResource(R.string.x_wg_save)) }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
