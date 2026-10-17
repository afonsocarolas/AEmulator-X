/* Added for AEmulator X, 2026-10-17: quick-start home screen widget. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import app.aemu.AppPrefs
import app.aemu.R
import app.aemu.core.GuestImage
import app.aemu.core.ImageStore
import app.aemu.core.PlayStats
import app.aemu.core.Screenshot
import app.aemu.core.VmPaths
import java.io.File
import kotlin.concurrent.thread

/** Up to four firmwares (favourites, then most recently played); a tap boots one through MainActivity's BOOT intent. */
class RecentWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        thread { try { render(ctx, mgr, ids) } finally { pending.finish() } }
    }

    companion object {
        private val SLOTS = listOf(
            Triple(R.id.slot1, R.id.img1, R.id.name1), Triple(R.id.slot2, R.id.img2, R.id.name2),
            Triple(R.id.slot3, R.id.img3, R.id.name3), Triple(R.id.slot4, R.id.img4, R.id.name4),
        )

        /** Redraws every placed widget (after the library changed or a system was played). Non-blocking. */
        fun refresh(ctx: Context) {
            val app = ctx.applicationContext
            thread {
                runCatching {
                    val mgr = AppWidgetManager.getInstance(app)
                    val ids = mgr.getAppWidgetIds(ComponentName(app, RecentWidget::class.java))
                    if (ids.isNotEmpty()) render(app, mgr, ids)
                }
            }
        }

        /** Favourites first, then by last played, then by name. */
        internal fun pick(images: List<GuestImage>, favorites: Set<String>, lastPlayed: Map<String, Long>): List<GuestImage> =
            images.sortedWith(compareByDescending<GuestImage> { it.id in favorites }
                .thenByDescending { lastPlayed[it.id] ?: 0L }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }).take(4)

        private fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            val images = runCatching { ImageStore.list(ctx) }.getOrDefault(emptyList())
            val chosen = pick(images, AppPrefs.favorites(ctx), images.associate { it.id to PlayStats.read(ctx, it.id).lastPlayedAt })
            val rv = RemoteViews(ctx.packageName, R.layout.widget_recent)
            val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            rv.setOnClickPendingIntent(R.id.widget_title, open)
            rv.setViewVisibility(R.id.widget_empty, if (chosen.isEmpty()) View.VISIBLE else View.GONE)
            rv.setViewVisibility(R.id.widget_slots, if (chosen.isEmpty()) View.GONE else View.VISIBLE)
            if (chosen.isEmpty()) rv.setOnClickPendingIntent(R.id.widget_empty, open)
            SLOTS.forEachIndexed { i, (slot, img, name) ->
                val g = chosen.getOrNull(i)
                rv.setViewVisibility(slot, if (g == null) View.INVISIBLE else View.VISIBLE)
                if (g == null) return@forEachIndexed
                rv.setTextViewText(name, g.name)
                picture(ctx, g.id)?.let { rv.setImageViewBitmap(img, it) } ?: rv.setImageViewResource(img, R.mipmap.ic_launcher)
                val boot = Intent(ACTION_BOOT).setClass(ctx, MainActivity::class.java).putExtra("id", g.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                rv.setOnClickPendingIntent(slot, PendingIntent.getActivity(ctx, 100 + i, boot,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            }
            mgr.updateAppWidget(ids, rv)
        }

        /** The tile picture or the last screen, small (widget bitmaps travel through binder). */
        private fun picture(ctx: Context, id: String): Bitmap? = runCatching {
            val dir = VmPaths(ctx, id).dir
            val f = listOf(File(dir, "cover.jpg"), File(dir, Screenshot.THUMB)).firstOrNull { it.isFile } ?: return null
            val bmp = BitmapFactory.decodeFile(f.path) ?: return null
            val side = minOf(bmp.width, bmp.height)
            val sq = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
            Bitmap.createScaledBitmap(sq, 160, 160, true)
        }.getOrNull()
    }
}
