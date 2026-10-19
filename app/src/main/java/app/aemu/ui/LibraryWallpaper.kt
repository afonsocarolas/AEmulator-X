/* Added for AEmulator X, 2026-10-19: the user's own library background. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A picture of the user's choice behind the library (files/library-wallpaper.jpg), scaled to at most 1600 px. */
object LibraryWallpaper {
    private const val MAX = 1600
    fun file(ctx: Context) = File(ctx.filesDir, "library-wallpaper.jpg")

    /** Blocking; false when [uri] is not a picture. */
    fun save(ctx: Context, uri: Uri): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX) sample *= 2
        val bmp = ctx.contentResolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("not a picture")
        val dst = file(ctx)
        val tmp = File(dst.path + ".tmp")
        tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bmp.recycle()
        tmp.renameTo(dst)
    }.getOrDefault(false)

    fun remove(ctx: Context) = file(ctx).delete()
}

/** The wallpaper with a dimming layer over it, or nothing when none is chosen; re-read whenever the screen returns. */
@Composable
fun WallpaperLayer(dark: Boolean): Boolean {
    val ctx = LocalContext.current
    val resumes = rememberResumeCount()
    val pic by produceState<ImageBitmap?>(null, resumes) {
        value = withContext(Dispatchers.IO) {
            runCatching { LibraryWallpaper.file(ctx).takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() } }.getOrNull()
        }
    }
    val p = pic ?: return false
    Image(p, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (dark) 0.45f else 0.25f)))
    return true
}
