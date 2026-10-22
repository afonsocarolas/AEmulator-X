/* Added for AEmulator X, 2026-10-22: boot logo creator. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.core.BootAnimation
import app.aemu.core.BootLogoMaker
import app.aemu.core.BootLogoMaker.Background
import app.aemu.core.BootLogoMaker.Entrance
import app.aemu.core.GuestImage
import app.aemu.core.VmPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

/** Make a boot animation: word, line under it, colours, background, entrance, picture; a live preview plays it. */
@Composable
internal fun BootLogoCreator(img: GuestImage, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    var spec by remember { mutableStateOf(BootLogoMaker.Spec(text = "AEmulator", subtitle = img.name.take(BootLogoMaker.MAX_SUB))) }
    var saving by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    val pickPicture = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) thread {
            val bmp = runCatching {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
                var sample = 1
                while (maxOf(o.outWidth, o.outHeight) / sample > 512) sample *= 2
                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            }.getOrNull()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (bmp != null) spec = spec.copy(picture = bmp) else Toast.makeText(ctx, R.string.x_cover_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    // live preview: the entrance, a short pause, then a few breaths of the glow, again and again
    LaunchedEffect(spec) {
        val pw = 200; val ph = 240
        while (true) {
            for (i in 0..BootLogoMaker.INTRO) {
                val f = withContext(Dispatchers.Default) { BootLogoMaker.frame(spec, pw, ph, i.toFloat() / BootLogoMaker.INTRO, 0.5f) }
                preview = f.asImageBitmap(); delay(1000L / BootLogoMaker.FPS)
            }
            for (i in 0 until BootLogoMaker.LOOP * 2) {
                val glow = 0.5f + 0.5f * kotlin.math.sin(2 * Math.PI * i / BootLogoMaker.LOOP).toFloat()
                val f = withContext(Dispatchers.Default) { BootLogoMaker.frame(spec, pw, ph, 1f, glow) }
                preview = f.asImageBitmap(); delay(1000L / BootLogoMaker.FPS)
            }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("🎬 " + stringResource(R.string.x_bl_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(150.dp, 180.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
                        preview?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.size(150.dp, 180.dp)) }
                    }
                }
                OutlinedTextField(spec.text, { spec = spec.copy(text = it.take(BootLogoMaker.MAX_TEXT)) }, singleLine = true,
                    label = { Text(stringResource(R.string.x_bl_text)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(spec.subtitle, { spec = spec.copy(subtitle = it.take(BootLogoMaker.MAX_SUB)) }, singleLine = true,
                    label = { Text(stringResource(R.string.x_bl_sub)) }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.x_bl_colors), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (p in BootLogoMaker.PALETTES) {
                        val on = p.contentEquals(spec.colors)
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(p.map { Color(it) }))
                            .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { spec = spec.copy(colors = p) })
                    }
                }
                Text(stringResource(R.string.x_bl_bg), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((b, label) in listOf(Background.BLACK to R.string.x_bl_bg_black, Background.NIGHT to R.string.x_bl_bg_night,
                        Background.GRADIENT to R.string.x_bl_bg_gradient))
                        FilterChip(spec.background == b, { spec = spec.copy(background = b) }, label = { Text(stringResource(label)) })
                }
                Text(stringResource(R.string.x_bl_entrance), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((e, label) in listOf(Entrance.ZOOM to R.string.x_bl_zoom, Entrance.FADE to R.string.x_bl_fade,
                        Entrance.SLIDE to R.string.x_bl_slide, Entrance.TYPE to R.string.x_bl_type))
                        FilterChip(spec.entrance == e, { spec = spec.copy(entrance = e) }, label = { Text(stringResource(label)) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { pickPicture.launch("image/*") }) {
                        Text("🖼️ " + stringResource(if (spec.picture == null) R.string.x_bl_pic_add else R.string.x_bl_pic_change))
                    }
                    if (spec.picture != null) TextButton(onClick = { spec = spec.copy(picture = null) }) { Text(stringResource(R.string.x_bl_pic_remove)) }
                }
                Text(stringResource(R.string.x_bl_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(enabled = !saving && spec.text.isNotBlank(), onClick = {
                saving = true
                val s = spec
                thread {
                    val paths = VmPaths(ctx, img.id)
                    val (w, h) = BootLogoMaker.size(img.settings.width, img.settings.height)
                    val ok = runCatching {
                        val tmp = java.io.File(paths.dir, "bootanimation-custom.part")
                        tmp.writeBytes(BootLogoMaker.zip(s, w, h))
                        BootAnimation.customFile(paths).delete()
                        tmp.renameTo(BootAnimation.customFile(paths))
                    }.getOrDefault(false)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        saving = false
                        Toast.makeText(ctx, if (ok) R.string.x_bl_saved else R.string.x_bootanim_bad, Toast.LENGTH_LONG).show()
                        if (ok) onSaved()
                    }
                }
            }) { Text(stringResource(if (saving) R.string.x_bl_making else R.string.x_bl_use)) }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

