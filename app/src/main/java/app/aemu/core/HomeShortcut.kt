/* Added for AEmulator X, 2026-10-16: home screen shortcuts that start one firmware. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import java.io.File

/** Pins a launcher shortcut that boots [GuestImage] directly (MainActivity's app.aemu.BOOT intent). */
object HomeShortcut {
    private const val SIZE = 192

    /** False when the launcher does not support pinned shortcuts. */
    fun pin(ctx: Context, img: GuestImage): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) return false
        val intent = Intent(app.aemu.ui.ACTION_BOOT).setClass(ctx, app.aemu.ui.MainActivity::class.java)
            .putExtra("id", img.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val info = ShortcutInfoCompat.Builder(ctx, "vm-${img.id}")
            .setShortLabel(img.name.take(24).ifBlank { "AEmulator X" })
            .setLongLabel(img.name.take(48).ifBlank { "AEmulator X" })
            .setIcon(icon(ctx, img.id))
            .setIntent(intent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(ctx, info, null)
    }

    /** The tile picture, else the last screen, centre-cropped to a square; the app icon when neither exists. */
    private fun icon(ctx: Context, id: String): IconCompat {
        val dir = VmPaths(ctx, id).dir
        val src = listOf(File(dir, "cover.jpg"), File(dir, Screenshot.THUMB)).firstOrNull { it.isFile }
        val bmp = src?.let { runCatching { BitmapFactory.decodeFile(it.path) }.getOrNull() }
            ?: return IconCompat.createWithResource(ctx, app.aemu.R.mipmap.ic_launcher)
        val side = minOf(bmp.width, bmp.height)
        val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
        return IconCompat.createWithBitmap(scaled)
    }
}
