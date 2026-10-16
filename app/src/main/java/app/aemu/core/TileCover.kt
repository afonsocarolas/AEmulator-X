/* Added for AEmulator X, 2026-10-16: custom pictures for the square library tiles. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/** The user's own picture for a firmware's tile (VmPaths.dir/cover.jpg), shown instead of its last screen. */
object TileCover {
    private const val NAME = "cover.jpg"
    private const val MAX = 720

    fun file(ctx: Context, id: String) = File(VmPaths(ctx, id).dir, NAME)

    /** Decodes [uri], scales its longer side to at most 720 px and stores it as JPEG. Blocking; false on failure. */
    fun save(ctx: Context, id: String, uri: Uri): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX) sample *= 2
        val bmp = ctx.contentResolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("not a picture")
        val scale = MAX.toFloat() / maxOf(bmp.width, bmp.height)
        val out = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt().coerceAtLeast(1),
            (bmp.height * scale).toInt().coerceAtLeast(1), true) else bmp
        val dst = file(ctx, id)
        val tmp = File(dst.path + ".tmp")
        tmp.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (out !== bmp) out.recycle(); bmp.recycle()
        tmp.renameTo(dst)
    }.getOrDefault(false)

    fun remove(ctx: Context, id: String) = file(ctx, id).delete()
}
