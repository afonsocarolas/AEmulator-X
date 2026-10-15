/* Added for AEmulator X, 2026-10-15: boot sound picker (X2). GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sign
import kotlin.math.sin

/**
 * The sound played on the phone when a VM starts booting, chosen per VM ([VmSettings.bootSound]):
 *   ""            the firmware's own boot sound, if it has one (unchanged behaviour)
 *   "none"        silence
 *   "x:<name>"    one of the built-in X sounds, synthesised here (original, no samples shipped)
 *   "fw:<path>"   a sound file from the firmware itself, as a guest path (/system/media/audio/ui/…)
 *   "file:<name>" an audio file the user picked, copied into the VM folder as [customFile]
 * Anything but "" also mutes the guest's audio while it boots, so the firmware's own boot sound does not play on top.
 */
object BootSound {
    const val FIRMWARE = ""
    const val NONE = "none"
    const val CHIME = "x:chime"
    const val RETRO = "x:retro"
    const val RISE = "x:rise"
    val BUILTIN = listOf(CHIME, RETRO, RISE)

    private const val RATE = 44100
    private val AUDIO_EXT = setOf("ogg", "wav", "mp3", "m4a", "aac", "flac", "amr", "mid", "midi", "imy", "oga")

    /** The user's own sound for this VM. */
    fun customFile(paths: VmPaths) = File(paths.dir, "bootsound.bin")

    /** True when this choice replaces the firmware's boot sound (the guest is muted while booting). */
    fun replacesFirmware(value: String) = value != FIRMWARE

    fun isFw(value: String) = value.startsWith("fw:")
    fun isFile(value: String) = value.startsWith("file:")
    fun fwPath(value: String) = value.removePrefix("fw:")
    fun fileName(value: String) = value.removePrefix("file:")

    /**
     * Sound files inside the firmware, as guest paths: the usual boot sound locations first (PowerOn.*, bootsound…),
     * then the system UI / notification / ringtone / alarm sounds. Blocking (walks the system tree).
     */
    fun firmwareSounds(root: File, limit: Int = 400): List<String> {
        val found = LinkedHashSet<String>()
        fun add(f: File) {
            if (found.size >= limit || !f.isFile || f.extension.lowercase() !in AUDIO_EXT || f.length() == 0L) return
            found += "/" + f.relativeTo(root).invariantSeparatorsPath
        }
        // classic boot sound locations (Samsung PowerOn.wav/ogg, HTC/LG/Moto bootanimation audio)
        listOf("system/etc", "system/media", "system/customize/resource", "system/media/audio", "system/media/bootsound")
            .map { File(root, it) }.forEach { d ->
                d.listFiles()?.filter { it.name.contains("power", true) || it.name.contains("boot", true) || it.name.contains("startup", true) }
                    ?.sortedBy { it.name.lowercase() }?.forEach(::add)
            }
        for (sub in listOf("ui", "notifications", "ringtones", "alarms")) {
            File(root, "system/media/audio/$sub").walkTopDown().maxDepth(3)
                .filter { it.isFile }.sortedBy { it.path.lowercase() }.forEach(::add)
        }
        return found.toList()
    }

    /** Copies the user's pick into the VM folder; returns the setting value, or null on failure. Blocking. */
    fun importCustom(ctx: Context, paths: VmPaths, uri: Uri): String? = runCatching {
        val name = ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "sound"
        val dst = customFile(paths)
        val tmp = File(dst.path + ".tmp")
        ctx.contentResolver.openInputStream(uri)!!.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
        if (tmp.length() == 0L || tmp.length() > 32L * 1024 * 1024) { tmp.delete(); error("empty or too big") }
        if (!tmp.renameTo(dst)) { dst.delete(); if (!tmp.renameTo(dst)) error("cannot store") }
        "file:" + name.take(80)
    }.getOrNull()

    /** A playing sound; [stop] is safe to call more than once and from any thread. */
    interface Playing { fun stop() }

    /**
     * Starts [value] (not [FIRMWARE] / [NONE], which play nothing and return null). [onDone] runs once when the
     * sound ends or fails. Non-blocking.
     */
    fun play(paths: VmPaths, value: String, log: (String) -> Unit = {}, onDone: () -> Unit = {}): Playing? = when {
        value in BUILTIN -> playPcm(synth(value), onDone)
        isFw(value) -> playFile(File(paths.root, fwPath(value).trimStart('/')), log, onDone)
        isFile(value) -> playFile(customFile(paths), log, onDone)
        else -> null
    }

    private val attrs: AudioAttributes by lazy { AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build() }

    private fun playFile(f: File, log: (String) -> Unit, onDone: () -> Unit): Playing? {
        if (!f.isFile) { log("boot sound: ${f.name} is missing"); onDone(); return null }
        val mp = MediaPlayer()
        var done = false
        fun finish() { synchronized(mp) { if (done) return; done = true }; runCatching { mp.release() }; onDone() }
        return runCatching {
            mp.setAudioAttributes(attrs)
            // open it here: the media server cannot read the app's private files by path
            FileInputStream(f).use { mp.setDataSource(it.fd) }
            mp.setOnCompletionListener { finish() }
            mp.setOnErrorListener { _, what, extra -> log("boot sound: playback error $what/$extra"); finish(); true }
            mp.setOnPreparedListener { it.start() }
            mp.prepareAsync()
            object : Playing { override fun stop() = finish() }
        }.getOrElse { log("boot sound: ${it.message}"); finish(); null }
    }

    private fun playPcm(pcm: ShortArray, onDone: () -> Unit): Playing? {
        val track = runCatching {
            AudioTrack.Builder().setAudioAttributes(attrs)
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.size * 2).build()
        }.getOrNull() ?: run { onDone(); return null }
        var done = false
        val lock = Object()
        fun finish() { synchronized(lock) { if (done) return; done = true }; runCatching { track.stop() }; runCatching { track.release() }; onDone() }
        return runCatching {
            track.write(pcm, 0, pcm.size)
            track.notificationMarkerPosition = pcm.size - 1
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) { finish() }
                override fun onPeriodicNotification(t: AudioTrack) {}
            }, android.os.Handler(android.os.Looper.getMainLooper()))
            track.play()
            object : Playing { override fun stop() = finish() }
        }.getOrElse { finish(); null }
    }

    // --- built-in sounds: small additive synth, mono 16-bit ---

    /** One note: [f] Hz from [at] s for [len] s; bell = decaying sine + soft overtones, square = 8-bit pulse. */
    private class Note(val f: Double, val at: Double, val len: Double, val gain: Double, val square: Boolean = false, val sweepTo: Double = 0.0)

    fun synth(name: String): ShortArray {
        val notes = when (name) {
            // bright three-note bell arpeggio resolving on a chord: E5 G#5 B5 → E6
            CHIME -> listOf(
                Note(659.25, 0.00, 1.6, 0.30), Note(830.61, 0.14, 1.5, 0.28), Note(987.77, 0.28, 1.5, 0.26),
                Note(1318.5, 0.46, 1.9, 0.30), Note(659.25, 0.46, 1.9, 0.16), Note(329.63, 0.46, 1.9, 0.14),
            )
            // 8-bit power-up jingle: C5 E5 G5 C6 and a held C6/G5
            RETRO -> listOf(
                Note(523.25, 0.00, 0.10, 0.40, true), Note(659.25, 0.09, 0.10, 0.40, true),
                Note(783.99, 0.18, 0.10, 0.40, true), Note(1046.5, 0.27, 0.10, 0.40, true),
                Note(1046.5, 0.40, 0.55, 0.36, true), Note(783.99, 0.40, 0.55, 0.22, true),
            )
            // airy rising sweep into a soft A major pad
            else -> listOf(
                Note(220.0, 0.00, 1.1, 0.22, sweepTo = 880.0),
                Note(440.0, 0.85, 1.6, 0.18), Note(554.37, 0.90, 1.55, 0.15), Note(659.25, 0.95, 1.5, 0.15), Note(880.0, 1.0, 1.45, 0.12),
            )
        }
        val total = notes.maxOf { it.at + it.len } + 0.05
        val out = DoubleArray((total * RATE).toInt())
        for (n in notes) {
            val start = (n.at * RATE).toInt()
            val count = (n.len * RATE).toInt()
            var phase = 0.0
            for (i in 0 until count) {
                val idx = start + i
                if (idx >= out.size) break
                val t = i.toDouble() / RATE
                val f = if (n.sweepTo > 0) n.f * Math.pow(n.sweepTo / n.f, t / n.len) else n.f
                phase += 2 * PI * f / RATE
                val attack = (t / 0.008).coerceAtMost(1.0)
                val env = when {
                    n.square -> attack * (1.0 - t / n.len).coerceAtLeast(0.0)
                    n.sweepTo > 0 -> sin(PI * t / n.len) // swell in and out
                    else -> attack * exp(-3.2 * t / n.len)
                }
                val wave = if (n.square) sign(sin(phase)) * 0.6
                    else sin(phase) + 0.35 * sin(2 * phase) * exp(-6 * t) + 0.12 * sin(3 * phase) * exp(-9 * t)
                out[idx] += wave * env * n.gain
            }
        }
        // gentle limiter, then 16-bit; short fade at the very end against clicks
        val pcm = ShortArray(out.size)
        val fade = (0.03 * RATE).toInt()
        for (i in out.indices) {
            var v = kotlin.math.tanh(out[i] * 1.4) * 0.85
            if (i > out.size - fade) v *= (out.size - i).toDouble() / fade
            pcm[i] = (v * Short.MAX_VALUE).toInt().toShort()
        }
        return pcm
    }
}
