/* Added for AEmulator X, 2026-10-16: gallery of screenshots and screen recordings. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.aemu.R
import app.aemu.core.Screenshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Screenshots (Pictures/AEmulator) and screen recordings (Movies/AEmulator), newest first. */
class GalleryActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.aemu.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AemuTheme { Gallery(onBack = ::finish) } }
    }
}

/** One saved picture or video: a file we can reach directly, or (scoped storage) a MediaStore entry. */
internal data class MediaItem(val name: String, val video: Boolean, val uri: Uri, val file: File?, val date: Long)

internal object GalleryStore {
    private fun picturesDir() = Screenshot.phoneDir()
    private fun moviesDir() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "AEmulator")

    fun load(ctx: Context): List<MediaItem> {
        val out = LinkedHashMap<String, MediaItem>()
        for ((dir, video) in listOf(picturesDir() to false, moviesDir() to true)) {
            dir.listFiles()?.filter { it.isFile && it.length() > 0 && wanted(it.name, video) }?.forEach {
                out[it.name] = MediaItem(it.name, video, Uri.fromFile(it), it, it.lastModified())
            }
        }
        // files saved through MediaStore (no all-files access) are only reachable there
        if (Build.VERSION.SDK_INT >= 29) {
            query(ctx, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Pictures/AEmulator%", false, out)
            query(ctx, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Movies/AEmulator%", true, out)
        }
        return out.values.sortedByDescending { it.date }
    }

    private fun wanted(name: String, video: Boolean) =
        if (video) name.endsWith(".mp4", true) else name.endsWith(".png", true) || name.endsWith(".jpg", true)

    private fun query(ctx: Context, base: Uri, rel: String, video: Boolean, out: MutableMap<String, MediaItem>) = runCatching {
        ctx.contentResolver.query(base, arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATE_MODIFIED),
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?", arrayOf(rel), null)?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                if (name in out || !wanted(name, video)) continue
                out[name] = MediaItem(name, video, ContentUris.withAppendedId(base, c.getLong(0)), null, c.getLong(2) * 1000)
            }
        }
    }

    /** A content:// uri other apps can open (FileProvider for plain files). */
    fun shareUri(ctx: Context, item: MediaItem): Uri =
        item.file?.let { FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", it) } ?: item.uri

    fun mime(item: MediaItem) = when {
        item.video -> "video/mp4"; item.name.endsWith(".jpg", true) -> "image/jpeg"; else -> "image/png"
    }

    fun delete(ctx: Context, item: MediaItem): Boolean = runCatching {
        val f = item.file
        if (f != null) f.delete().also { if (it) MediaScannerConnection.scanFile(ctx, arrayOf(f.path), null, null) }
        else ctx.contentResolver.delete(item.uri, null, null) > 0
    }.getOrDefault(false)

    fun thumbnail(ctx: Context, item: MediaItem, px: Int = 320): Bitmap? = runCatching {
        val f = item.file
        when {
            f != null && !item.video -> {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(f.path, o)
                var s = 1; while (maxOf(o.outWidth, o.outHeight) / (s * 2) >= px) s *= 2
                BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
            }
            f != null && Build.VERSION.SDK_INT >= 29 -> ThumbnailUtils.createVideoThumbnail(f, Size(px, px), null)
            f != null -> MediaMetadataRetriever().run { try { setDataSource(f.path); getFrameAtTime(0) } finally { release() } }
            Build.VERSION.SDK_INT >= 29 -> ctx.contentResolver.loadThumbnail(item.uri, Size(px, px), null)
            else -> null
        }
    }.getOrNull()
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Gallery(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var rev by remember { mutableIntStateOf(0) }
    val items by produceState<List<MediaItem>?>(null, rev) { value = withContext(Dispatchers.IO) { GalleryStore.load(ctx) } }
    var deleteItem by remember { mutableStateOf<MediaItem?>(null) }
    // AEmulator X: stickers on a screenshot
    var decorateItem by remember { mutableStateOf<MediaItem?>(null) }

    AuroraBackground(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.x_gallery)) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { pad ->
            val list = items
            when {
                list == null -> {}
                list.isEmpty() -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.x_gallery_empty), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(32.dp))
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = pad.calculateTopPadding(), bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.name }) { item ->
                        GalleryCell(item, onDelete = { deleteItem = item }, onDecorate = { decorateItem = item })
                    }
                }
            }
        }
    }

    decorateItem?.let { item -> StickerEditor(item, onDismiss = { decorateItem = null }, onSaved = { decorateItem = null; rev++ }) }
    deleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteItem = null },
            icon = { Icon(Icons.Rounded.Delete, null) },
            title = { Text(stringResource(R.string.x_gallery_delete_q)) },
            text = { Text(item.name) },
            confirmButton = {
                Button(onClick = {
                    if (!GalleryStore.delete(ctx, item)) Toast.makeText(ctx, R.string.m_failed, Toast.LENGTH_SHORT).show()
                    deleteItem = null; rev++
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteItem = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun GalleryCell(item: MediaItem, onDelete: () -> Unit, onDecorate: () -> Unit = {}) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val thumb by produceState<ImageBitmap?>(null, item.uri) {
        value = withContext(Dispatchers.IO) { GalleryStore.thumbnail(ctx, item)?.asImageBitmap() }
    }
    fun open() = runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(GalleryStore.shareUri(ctx, item), GalleryStore.mime(item))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }.onFailure { Toast.makeText(ctx, R.string.m_failed, Toast.LENGTH_SHORT).show() }
    fun share() = runCatching {
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(GalleryStore.mime(item))
            .putExtra(Intent.EXTRA_STREAM, GalleryStore.shareUri(ctx, item)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null))
    }

    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .combinedClickable(onClick = { open() }, onLongClick = { menu = true })) {
        thumb?.let { Image(it, item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            ?: Icon(Icons.Rounded.PhotoLibrary, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.Center))
        if (item.video) Box(Modifier.align(Alignment.Center).size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PlayArrow, null, tint = Color.White) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (!item.video && !item.name.endsWith(".gif", true)) DropdownMenuItem(text = { Text("✨ " + stringResource(R.string.x_st_title)) },
                onClick = { menu = false; onDecorate() })
            DropdownMenuItem(text = { Text(stringResource(R.string.x_gallery_share)) }, leadingIcon = { Icon(Icons.Rounded.Share, null) },
                onClick = { menu = false; share() })
            DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                onClick = { menu = false; onDelete() })
        }
    }
    LaunchedEffect(Unit) {}
}
