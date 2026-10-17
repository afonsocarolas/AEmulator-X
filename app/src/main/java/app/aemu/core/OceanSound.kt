/* Added for AEmulator X, 2026-10-17: Relax mode, generated ocean sound. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Soft ocean waves made on the fly, no recording shipped: brown noise (deep, smooth) whose loudness swells and ebbs
 * like waves arriving every ~9 s, with a little variation, fading in and out. Stereo, quiet by design.
 */
class OceanSound {
    @Volatile private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({ play() }, "relax-ocean").apply { isDaemon = true; start() }
    }

    fun stop() { running = false; thread?.interrupt(); thread = null }

    private fun play() {
        val rate = 44100
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(maxOf(min, rate)).setTransferMode(AudioTrack.MODE_STREAM).build()
        }.getOrNull() ?: return
        track.play()
        val frames = rate / 20
        val buf = ShortArray(frames * 2)
        val rnd = Random(System.nanoTime())
        var l = 0.0; var r = 0.0; var t = 0L
        var waveLen = 9.0 * rate; var wavePos = 0.0
        try {
            while (running) {
                for (i in 0 until frames) {
                    // brown noise: integrated white noise, leaky so it stays centred
                    l = (l + 0.02 * (rnd.nextDouble() * 2 - 1)) / 1.02
                    r = (r + 0.02 * (rnd.nextDouble() * 2 - 1)) / 1.02
                    // a wave: slow rise, longer fall
                    val p = wavePos / waveLen
                    val swell = if (p < 0.4) sin(p / 0.4 * PI / 2) else sin((1 - (p - 0.4) / 0.6) * PI / 2)
                    val env = 0.25 + 0.75 * swell * swell
                    val fade = minOf(1.0, t / (3.0 * rate))                    // fade in over 3 s
                    val g = 3.2 * env * fade
                    buf[2 * i] = (l * g * 32767 * 0.5).toInt().coerceIn(-32767, 32767).toShort()
                    buf[2 * i + 1] = (r * g * 32767 * 0.5).toInt().coerceIn(-32767, 32767).toShort()
                    t++; wavePos++
                    if (wavePos >= waveLen) { wavePos = 0.0; waveLen = (7.5 + rnd.nextDouble() * 3.5) * rate }
                }
                track.write(buf, 0, buf.size)
            }
        } catch (_: Exception) {
        } finally {
            runCatching { track.pause(); track.flush(); track.release() }
        }
    }
}
