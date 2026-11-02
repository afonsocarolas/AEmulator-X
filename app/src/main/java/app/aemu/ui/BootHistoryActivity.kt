/* Added for AEmulator X, 2026-11-02: Android boot history. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.BootHistory
import app.aemu.core.BootHistory.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.zip.ZipFile

/** Your firmwares' original boot screens, oldest Android first, one per page. */
class BootHistoryActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.aemu.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AemuTheme { BootHistoryScreen(onClose = ::finish) } }
    }
}

@Composable
private fun BootHistoryScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val items by produceState<List<BootHistory.Item>?>(null) { value = withContext(Dispatchers.IO) { BootHistory.scan(ctx) } }
    val eras = stringArrayResource(R.array.x_bh_era)
    AuroraBackground(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🎬 " + stringResource(R.string.x_bh_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) }
            }
            val list = items
            when {
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                list.isEmpty() -> Text(stringResource(R.string.x_bh_empty), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
                else -> {
                    val pager = rememberPagerState { list.size }
                    HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { i ->
                        val it = list[i]
                        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // the phone's screen
                            Box(Modifier.weight(1f).aspectRatio(9f / 16f).clip(RoundedCornerShape(24.dp)).background(Color.Black),
                                contentAlignment = Alignment.Center) {
                                val playing = pager.currentPage == i
                                when (val s = it.source) {
                                    is Source.Zip -> ZipPlayer(s.file, playing)
                                    is Source.Classic -> ClassicPlayer(s.apk, playing)
                                    else -> Text(if (s is Source.Vendor) "🔒" else "∅", fontSize = 40.sp, color = Color.White)
                                }
                            }
                            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(it.img.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${AndroidEras.name(it.img.api)} · ${AndroidEras.year(it.img.api)}", style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(when (it.source) {
                                        is Source.Zip -> R.string.x_bh_zip; is Source.Classic -> R.string.x_bh_classic
                                        is Source.Vendor -> R.string.x_bh_vendor; else -> R.string.x_bh_none
                                    }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("💡 " + eras.getOrElse(BootHistory.era(it.img.api)) { "" }, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    Text("${pager.currentPage + 1} / ${list.size} · " + stringResource(R.string.x_museum_swipe), style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(12.dp))
                }
            }
        }
    }
}

private fun decode(bytes: ByteArray, maxW: Int): Bitmap? {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
    var sample = 1
    while (o.outWidth / (sample * 2) >= maxW) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
}

/** Plays a bootanimation.zip the way Android does: the parts in order, each its number of times, the last loop forever. */
@Composable
private fun ZipPlayer(file: java.io.File, playing: Boolean) {
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(file, playing) {
        withContext(Dispatchers.IO) {
            runCatching {
                ZipFile(file).use { z ->
                    val desc = BootHistory.parseDesc(z.getInputStream(z.getEntry("desc.txt")).use { it.readBytes().decodeToString() })
                        ?: error("no desc")
                    val names = z.entries().toList().map { it.name }
                    val step = 1000L / desc.fps
                    suspend fun show(name: String) {
                        val t0 = System.currentTimeMillis()
                        decode(z.getInputStream(z.getEntry(name)).use { it.readBytes() }, 540)?.let { frame = it.asImageBitmap() }
                        delay((step - (System.currentTimeMillis() - t0)).coerceAtLeast(1))
                    }
                    // a still page shows the first picture only
                    val first = desc.parts.asSequence().map { BootHistory.frames(names, it.dir) }.firstOrNull { it.isNotEmpty() }?.firstOrNull()
                    if (!playing) { first?.let { show(it) }; return@use }
                    while (true) {
                        var played = false
                        for (p in desc.parts) {
                            val frames = BootHistory.frames(names, p.dir)
                            if (frames.isEmpty()) continue
                            val times = if (p.count == 0) Int.MAX_VALUE else p.count
                            repeat(times) { for (f in frames) { show(f); played = true }; delay(step * p.pause) }
                        }
                        if (!played) error("no pictures")
                    }
                }
            }.onFailure { if (it !is kotlinx.coroutines.CancellationException) failed = true else throw it }
        }
    }
    frame?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
    if (failed && frame == null) Text("∅", color = Color.White, fontSize = 40.sp)
}

/** Android 1.x–2.3's own logo: the ANDROID letters cut out of black, with the shine sliding behind them. */
@Composable
private fun ClassicPlayer(apk: java.io.File, playing: Boolean) {
    val pics by produceState<Pair<ImageBitmap, ImageBitmap>?>(null, apk) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                ZipFile(apk).use { z ->
                    fun read(n: String) = BitmapFactory.decodeStream(z.getInputStream(z.getEntry(n)))!!.asImageBitmap()
                    read(BootHistory.MASK) to read(BootHistory.SHINE)
                }
            }.getOrNull()
        }
    }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        var start = 0L
        while (playing) withFrameNanos { now -> if (start == 0L) start = now; t = (now - start) / 1e9f }
    }
    val p = pics ?: return
    val (mask, shine) = p
    Canvas(Modifier.fillMaxSize()) {
        val scale = minOf(size.width * 0.8f / mask.width, 2.5f)
        val mw = (mask.width * scale).toInt(); val mh = (mask.height * scale).toInt()
        val left = ((size.width - mw) / 2).toInt(); val top = ((size.height - mh) / 2).toInt()
        // the shine moves from left to right behind the logo, again and again
        val sw = (shine.width * scale).toInt().coerceAtLeast(1); val sh = mh
        val offset = ((t * 150f * scale) % sw).toInt()
        var x = left - sw + offset
        while (x < left + mw) {
            drawImage(shine, dstOffset = IntOffset(x, top), dstSize = IntSize(sw, sh))
            x += sw
        }
        drawImage(mask, dstOffset = IntOffset(left, top), dstSize = IntSize(mw, mh))
        drawRect(Color.Black, Offset.Zero, androidx.compose.ui.geometry.Size(size.width, top.toFloat()))
        drawRect(Color.Black, Offset(0f, (top + mh).toFloat()), androidx.compose.ui.geometry.Size(size.width, size.height - top - mh))
        // the shine only shows through the letters: nothing beside the logo
        drawRect(Color.Black, Offset(0f, top.toFloat()), androidx.compose.ui.geometry.Size(left.toFloat(), mh.toFloat()))
        drawRect(Color.Black, Offset((left + mw).toFloat(), top.toFloat()), androidx.compose.ui.geometry.Size(size.width - left - mw, mh.toFloat()))
    }
}
