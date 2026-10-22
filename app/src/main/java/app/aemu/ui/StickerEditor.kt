/* Added for AEmulator X, 2026-10-22: screenshot stickers. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import app.aemu.R
import app.aemu.core.Stickers
import app.aemu.core.Stickers.Sticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.concurrent.thread

/** Full-screen sticker editor for one screenshot; [onSaved] after a new decorated copy was saved. */
@Composable
internal fun StickerEditor(item: MediaItem, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    val base by produceState<Bitmap?>(null, item.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(item.uri)?.use { BitmapFactory.decodeStream(it, null, o) }
                var sample = 1
                while (maxOf(o.outWidth, o.outHeight) / sample > 2048) sample *= 2
                ctx.contentResolver.openInputStream(item.uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            }.getOrNull()
        }
    }
    var stickers by remember { mutableStateOf(listOf<Sticker>()) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var nextId by remember { mutableIntStateOf(1) }
    var askText by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val sel = stickers.firstOrNull { it.id == selected }

    fun update(id: Int, f: (Sticker) -> Sticker) { stickers = stickers.map { if (it.id == id) Stickers.clamp(f(it)) else it } }
    fun add(s: Sticker) { val n = s.copy(id = nextId); nextId++; stickers = stickers + n; selected = n.id }

    /** Renders the full-size picture and hands its PNG bytes to [then] (off the main thread). */
    fun export(then: (ByteArray) -> Boolean, ok: Int, after: () -> Unit = {}) {
        val b = base ?: return
        busy = true
        val list = stickers
        thread {
            val done = runCatching {
                val out = Stickers.render(b, list)
                val bytes = ByteArrayOutputStream().also { out.compress(Bitmap.CompressFormat.PNG, 100, it); out.recycle() }.toByteArray()
                then(bytes)
            }.getOrDefault(false)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                busy = false
                Toast.makeText(ctx, if (done) ok else R.string.m_failed, Toast.LENGTH_SHORT).show()
                if (done) after()
            }
        }
    }
    val stamp = { java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(java.util.Date()) }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF101014))) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                // top bar
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (!busy) onDismiss() }) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color.White) }
                    Text("✨ " + stringResource(R.string.x_st_title), color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(enabled = !busy && base != null, onClick = {
                        export({ bytes ->
                            val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
                            val f = File(dir, "decorated-${stamp()}.png").apply { writeBytes(bytes) }
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", f)
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            true
                        }, R.string.x_st_shared)
                    }) { Text(stringResource(R.string.x_gallery_share), color = Color.White) }
                    Button(enabled = !busy && base != null, onClick = {
                        export({ bytes -> app.aemu.core.Screenshot.savePicture(ctx, bytes, "decorated-${stamp()}.png", "image/png") != null },
                            R.string.x_st_saved, after = onSaved)
                    }) { Text(stringResource(R.string.save)) }
                }
                // the picture and its stickers
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    val b = base
                    if (b == null) CircularProgressIndicator()
                    else {
                        val boxW = constraints.maxWidth.toFloat(); val boxH = constraints.maxHeight.toFloat()
                        val scale = minOf(boxW / b.width, boxH / b.height)
                        val w = b.width * scale; val h = b.height * scale
                        val img = remember(b) { b.asImageBitmap() }
                        val current by rememberUpdatedState(stickers)
                        val currentSel by rememberUpdatedState(selected)
                        Canvas(Modifier.size(with(androidx.compose.ui.platform.LocalDensity.current) { w.toDp() },
                            with(androidx.compose.ui.platform.LocalDensity.current) { h.toDp() })
                            .pointerInput(w, h) {
                                detectTapGestures { o ->
                                    selected = Stickers.hit(current, o.x / w, o.y / h, h / w)?.id
                                }
                            }
                            .pointerInput(w, h) {
                                detectTransformGestures { centroid, pan, zoom, rotation ->
                                    val id = currentSel ?: Stickers.hit(current, centroid.x / w, centroid.y / h, h / w)?.id?.also { selected = it }
                                        ?: return@detectTransformGestures
                                    update(id) { it.copy(x = it.x + pan.x / w, y = it.y + pan.y / h, size = it.size * zoom, angle = it.angle + rotation) }
                                }
                            }) {
                            drawImage(img, dstSize = IntSize(size.width.toInt(), size.height.toInt()), dstOffset = IntOffset.Zero)
                            drawIntoCanvas { Stickers.draw(it.nativeCanvas, size.width.toInt(), size.height.toInt(), stickers) }
                            stickers.firstOrNull { it.id == selected }?.let { s ->
                                val r = maxOf(s.size * 0.6f, 0.05f) * size.width
                                drawCircle(Color.White, r, Offset(s.x * size.width, s.y * size.height),
                                    style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
                            }
                        }
                    }
                }
                // tools
                Surface(color = Color(0xFF1C1B22)) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (sel != null) Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.x_st_size), color = Color.White, style = MaterialTheme.typography.labelMedium)
                            Slider(sel.size, { v -> update(sel.id) { it.copy(size = v) } }, valueRange = Stickers.MIN_SIZE..Stickers.MAX_SIZE,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                            IconButton(onClick = { stickers = stickers.filter { it.id != sel.id }; selected = null }) {
                                Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = Color(0xFFFF8A80))
                            }
                        }
                        if (sel != null && !sel.emoji) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (c in Stickers.COLORS) Box(Modifier.size(30.dp).clip(CircleShape).background(Color(c))
                                .border(if (c == sel.color) 3.dp else 1.dp, if (c == sel.color) Color(0xFF18FFFF) else Color.Gray, CircleShape)
                                .clickable { update(sel.id) { it.copy(color = c) } })
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(onClick = { askText = true }, modifier = Modifier.padding(end = 8.dp)) { Text("Aa " + stringResource(R.string.x_st_text)) }
                            for (e in Stickers.EMOJIS) Text(e, fontSize = 30.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .clickable { add(Sticker(0, e, emoji = true)) }.padding(6.dp))
                        }
                        Text(stringResource(R.string.x_st_hint), color = Color(0xFFB0BEC5), style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
            if (busy) CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
    if (askText) {
        var t by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { askText = false },
            title = { Text(stringResource(R.string.x_st_text)) },
            text = { OutlinedTextField(t, { t = it.take(Stickers.MAX_TEXT) }, singleLine = true, placeholder = { Text(stringResource(R.string.x_st_text_hint)) }) },
            confirmButton = { Button(enabled = t.isNotBlank(), onClick = { add(Sticker(0, t.trim(), emoji = false, size = 0.3f)); askText = false }) {
                Text(stringResource(R.string.x_st_add)) } },
            dismissButton = { TextButton(onClick = { askText = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

