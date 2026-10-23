/* Added for AEmulator X, 2026-10-23: firmware slideshow. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.aemu.R
import app.aemu.core.FirmwareWallpapers
import app.aemu.core.ImageStore
import app.aemu.core.Screenshot
import app.aemu.core.SlideOrder
import app.aemu.core.TileCover
import app.aemu.core.VmPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A calm screensaver of your firmwares: their screenshots, covers and wallpapers, slowly zooming, with name and year. */
class SlideshowActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.aemu.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { Slideshow(onClose = ::finish) }
    }
}

/** One picture: where it comes from and whose it is. */
private class Slide(val firmware: String, val api: Int, val load: () -> ByteArray?)

private fun slides(ctx: Context): List<Slide> {
    val imgs = runCatching { ImageStore.list(ctx) }.getOrDefault(emptyList()).filter { it.baseId.isEmpty() }
    val shots = runCatching { GalleryStore.load(ctx) }.getOrDefault(emptyList()).filter { !it.video && !it.name.endsWith(".gif", true) }
    val all = ArrayList<Pair<String, Slide>>()
    for (img in imgs) {
        val mine = ArrayList<Slide>()
        // your screenshots of it: their names end with the firmware's id
        for (s in shots.filter { it.name.substringBeforeLast('.').endsWith("-" + img.id) })
            mine += Slide(img.name, img.api) { runCatching { ctx.contentResolver.openInputStream(s.uri)?.use { it.readBytes() } }.getOrNull() }
        TileCover.file(ctx, img.id).takeIf { it.isFile }?.let { f -> mine += Slide(img.name, img.api) { runCatching { f.readBytes() }.getOrNull() } }
        runCatching { FirmwareWallpapers.scan(img, VmPaths(ctx, img.id).root) }.getOrDefault(emptyList())
            .take(SlideOrder.WALLPAPERS_PER_FIRMWARE).forEach { w -> mine += Slide(img.name, img.api) { w.bytes() } }
        // nothing bigger: its small home screen picture
        if (mine.isEmpty()) listOf(Screenshot.HOME, Screenshot.THUMB).map { File(VmPaths(ctx, img.id).dir, it) }.firstOrNull { it.isFile }
            ?.let { f -> mine += Slide(img.name, img.api) { runCatching { f.readBytes() }.getOrNull() } }
        mine.forEach { all += img.id to it }
    }
    return SlideOrder.interleave(all, { it.first }).map { it.second }
}

private fun decode(bytes: ByteArray, max: Int): ImageBitmap? {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
    var sample = 1
    while (maxOf(o.outWidth, o.outHeight) / (sample * 2) >= max) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}

private class Shown(val index: Int, val image: ImageBitmap, val slide: Slide)

@Composable
private fun Slideshow(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val list by produceState<List<Slide>?>(null) { value = withContext(Dispatchers.IO) { slides(ctx) } }
    var index by remember { mutableIntStateOf(0) }
    var shown by remember { mutableStateOf<Shown?>(null) }
    var shownFor by remember { mutableIntStateOf(-1) }               // the index whose picture is on screen
    var t by remember { mutableFloatStateOf(0f) }                    // seconds into the current slide
    var paused by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    val l = list
    // load the next picture; a picture that cannot be read is skipped
    LaunchedEffect(l, index) {
        if (l.isNullOrEmpty()) return@LaunchedEffect
        var i = index
        repeat(l.size) {
            val img = withContext(Dispatchers.IO) { l[i % l.size].load()?.let { runCatching { decode(it, 1800) }.getOrNull() } }
            if (img != null) { shown = Shown(i, img, l[i % l.size]); shownFor = index; t = 0f; return@LaunchedEffect }
            i++
        }
    }
    LaunchedEffect(l, paused) {
        if (l.isNullOrEmpty()) return@LaunchedEffect
        var last = 0L
        while (!paused) withFrameNanos { now ->
            if (last != 0L) t += (now - last) / 1e9f
            last = now
            if (t >= SlideOrder.SECONDS && shownFor == index) index = ((shown?.index ?: index) + 1) % l.size
        }
    }
    LaunchedEffect(controls) { if (controls) { kotlinx.coroutines.delay(3500); controls = false } }

    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTapGestures { controls = !controls } }) {
        when {
            l == null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            l.isEmpty() -> Text(stringResource(R.string.x_ss_empty), color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp))
            else -> Crossfade(shown, animationSpec = tween(SlideOrder.FADE_MS), label = "slide") { s ->
                if (s != null) Box(Modifier.fillMaxSize()) {
                    // soft blurred copy behind, so pictures of any shape fill the screen
                    Image(s.image, null, contentScale = ContentScale.Crop, alpha = 0.35f, modifier = Modifier.fillMaxSize()
                        .graphicsLayer { scaleX = 1.3f; scaleY = 1.3f })
                    val z = if (s === shown) SlideOrder.zoom(t) else SlideOrder.zoom(SlideOrder.SECONDS.toFloat())
                    val drift = if (s.index % 2 == 0) 1f else -1f
                    Image(s.image, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = z; scaleY = z; translationX = drift * (z - 1f) * size.width * 0.35f
                    })
                    // name and year
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                        .navigationBarsPadding().padding(start = 28.dp, end = 28.dp, top = 48.dp, bottom = 28.dp)) {
                        Column {
                            Text(s.slide.firmware, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("${AndroidEras.name(s.slide.api)} · ${AndroidEras.year(s.slide.api)}", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        AnimatedVisibility(controls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopEnd)) {
            Row(Modifier.statusBarsPadding().padding(8.dp).background(Color(0x66000000), androidx.compose.foundation.shape.CircleShape)) {
                if (!l.isNullOrEmpty()) IconButton(onClick = { paused = !paused }) {
                    Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, tint = Color.White)
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color.White) }
            }
        }
    }
}
