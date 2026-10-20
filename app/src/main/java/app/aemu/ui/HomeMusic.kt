/* Added for AEmulator X, 2026-10-20: music player for the library. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aemu.R

/**
 * Your own songs while you browse the library: a playlist of audio files picked once (kept with persistable
 * permission), played one after another. It pauses when the library is left, so it never plays over a running system.
 */
object HomeMusic {
    private const val KEY = "x_music_list"
    private var player: MediaPlayer? = null
    var songs by mutableStateOf<List<Uri>>(emptyList()); private set
    var index by mutableStateOf(0); private set
    var playing by mutableStateOf(false); private set
    var title by mutableStateOf(""); private set
    var shuffle by mutableStateOf(false)
    /** the bar is shown (closing it stops the music but keeps the playlist) */
    var visible by mutableStateOf(false)

    fun load(ctx: Context) {
        if (songs.isNotEmpty()) return
        songs = app.aemu.AppPrefs.prefs(ctx).getString(KEY, null).orEmpty().lines().filter { it.isNotBlank() }.map(Uri::parse)
    }

    /** New songs picked: they replace the playlist and start playing. */
    fun setSongs(ctx: Context, picked: List<Uri>) {
        if (picked.isEmpty()) return
        for (u in picked) runCatching { ctx.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        songs = picked
        app.aemu.AppPrefs.prefs(ctx).edit().putString(KEY, picked.joinToString("\n")).apply()
        visible = true
        play(ctx, 0)
    }

    fun play(ctx: Context, i: Int) {
        if (songs.isEmpty()) return
        release()
        index = Math.floorMod(i, songs.size)
        val uri = songs[index]
        title = nameOf(ctx, uri)
        val app = ctx.applicationContext
        val mp = MediaPlayer()
        val ok = runCatching {
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            mp.setDataSource(app, uri)
            mp.setOnCompletionListener { next(app) }
            mp.setOnErrorListener { _, _, _ -> next(app); true }
            mp.prepare()
            mp.start()
        }.isSuccess
        if (!ok) { mp.release(); playing = false; title = app.getString(R.string.x_mu_cannot, title); return }
        player = mp
        playing = true
    }

    fun toggle(ctx: Context) {
        val mp = player
        when {
            mp == null -> play(ctx, index)
            mp.isPlaying -> { mp.pause(); playing = false }
            else -> { mp.start(); playing = true }
        }
    }

    fun next(ctx: Context) = play(ctx, if (shuffle && songs.size > 1) (index + 1 + kotlin.random.Random.nextInt(songs.size - 1)) % songs.size else index + 1)
    fun previous(ctx: Context) {
        val mp = player
        if (mp != null && runCatching { mp.currentPosition }.getOrDefault(0) > 3000) { mp.seekTo(0); return }
        play(ctx, index - 1)
    }

    fun pause() { player?.let { if (runCatching { it.isPlaying }.getOrDefault(false)) it.pause() }; playing = false }

    fun close() { release(); visible = false }

    private fun release() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null; playing = false
    }

    private fun nameOf(ctx: Context, uri: Uri): String = runCatching {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()?.substringBeforeLast('.') ?: uri.lastPathSegment.orEmpty()
}

/** The mini player over the bottom of the library. */
@Composable
fun MusicBar(modifier: Modifier, onPick: () -> Unit) {
    if (!HomeMusic.visible) return
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, tonalElevation = 6.dp, shadowElevation = 6.dp,
        modifier = modifier.widthIn(max = 520.dp)) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎵", modifier = Modifier.padding(end = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(HomeMusic.title.ifEmpty { "…" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                if (HomeMusic.songs.size > 1) Text("${HomeMusic.index + 1} / ${HomeMusic.songs.size}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = { HomeMusic.previous(ctx) }) { Icon(Icons.Rounded.SkipPrevious, null) }
                IconButton(onClick = { HomeMusic.toggle(ctx) }) { Icon(if (HomeMusic.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null) }
                IconButton(onClick = { HomeMusic.next(ctx) }) { Icon(Icons.Rounded.SkipNext, null) }
                IconButton(onClick = { HomeMusic.shuffle = !HomeMusic.shuffle }) {
                    Icon(Icons.Rounded.Shuffle, null, tint = if (HomeMusic.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onPick) { Icon(Icons.Rounded.LibraryMusic, null) }
                IconButton(onClick = { HomeMusic.close() }) { Icon(Icons.Rounded.Close, null) }
            }
        }
    }
}
