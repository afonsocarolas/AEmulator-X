/* Added for AEmulator X, 2026-10-16: square tile library layout. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.core.GuestImage
import app.aemu.core.TileCover
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

/** The user's tile picture (TileCover), decoded off the main thread; null when there is none. */
@Composable
private fun rememberCover(id: String, rev: Int): ImageBitmap? {
    val ctx = LocalContext.current
    val cover by produceState<ImageBitmap?>(null, id, rev) {
        value = withContext(Dispatchers.IO) {
            runCatching { TileCover.file(ctx, id).takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() } }.getOrNull()
        }
    }
    return cover
}

/**
 * One firmware as a square glass tile: its last screen (thumbnail) fills the tile under a dark scrim, the gradient
 * release badge sits top-left, name and version at the bottom. Tap starts (or reopens) it; long press or ⋮ opens
 * the menu with settings, rename, delete and the low-power (charging) start.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun SquareTile(
    img: GuestImage,
    active: Boolean,
    refresh: Int,
    onStart: () -> Unit,
    onLowPower: () -> Unit,
    onSettings: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    // AEmulator X: the user's own picture wins over the last screen; [coverRev] reloads it after a change
    var coverRev by remember { mutableIntStateOf(0) }
    val cover = rememberCover(img.id, refresh + coverRev)
    val hasCover = cover != null
    val thumb = cover ?: rememberThumbnail(img, refresh)
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) thread { val ok = TileCover.save(ctx, img.id, uri)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (ok) coverRev++ else Toast.makeText(ctx, R.string.x_cover_failed, Toast.LENGTH_SHORT).show()
            } }
    }
    val shape = RoundedCornerShape(24.dp)
    val startLabel = stringResource(if (active) R.string.open_vm else R.string.start)
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f).glass(shape)
            .combinedClickable(role = Role.Button, onClickLabel = startLabel, onClick = onStart,
                onLongClick = { menu = true }),
    ) {
        if (thumb != null) {
            Image(thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            // never booted: a big soft phone glyph on the brand sweep
            Box(Modifier.fillMaxSize().background(XBrand.sweep(0.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PhoneAndroid, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier.size(56.dp))
            }
        }
        // scrim so the text stays readable over any screenshot
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.35f), 0.35f to Color.Transparent,
            0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))))

        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Top) {
            XBadge(img.release.take(3), size = 40.dp)
            Spacer(Modifier.weight(1f))
            if (active) Box(Modifier.padding(top = 6.dp, end = 4.dp).size(10.dp).clip(CircleShape).background(XBrand.Cyan)
                .border(2.dp, Color.Black.copy(alpha = 0.4f), CircleShape))
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.MoreVert, stringResource(R.string.menu), tint = Color.White)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(startLabel) }, leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                        onClick = { menu = false; onStart() })
                    if (!active) DropdownMenuItem(text = { Text(stringResource(R.string.lpm_start)) },
                        leadingIcon = { Icon(Icons.Rounded.BatteryChargingFull, null) }, onClick = { menu = false; onLowPower() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.settings)) }, leadingIcon = { Icon(Icons.Rounded.Tune, null) },
                        onClick = { menu = false; onSettings() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.x_cover_choose)) }, leadingIcon = { Icon(Icons.Rounded.Image, null) },
                        onClick = { menu = false; pickCover.launch(arrayOf("image/*")) })
                    if (hasCover) DropdownMenuItem(text = { Text(stringResource(R.string.x_cover_remove)) },
                        leadingIcon = { Icon(Icons.Rounded.HideImage, null) },
                        onClick = { menu = false; TileCover.remove(ctx, img.id); coverRev++ })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        onClick = { menu = false; onDelete() })
                }
            }
        }

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp)) {
            Text(img.name, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(img.displayVersion, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f, fill = false))
                Box(Modifier.size(26.dp).clip(CircleShape).background(XBrand.gradient), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(0.dp))
            }
        }
    }
}
