/* Added for AEmulator X, 2026-10-18: Sleep mode, generated sleep sounds. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sleep sounds made on the fly, no recordings shipped: soft rain, ocean waves, or a steady deep noise.
 * [gain] (0..1) can be lowered while playing, so the sleep timer fades the sound out instead of cutting it.
 */
class SleepSound {
    enum class Kind { RAIN, OCEAN, NOISE }

    @Volatile var kind = Kind.RAIN
    @Volatile var gain = 1f
    @Volatile private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({ play() }, "sleep-sound").apply { isDaemon = true; start() }
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
        var bl = 0.0; var br = 0.0              // brown noise
        var ll = 0.0; var lr = 0.0              // low-passed white noise (rain body)
        var drop = 0.0; var dropPan = 0.5       // one rain drop ringing out
        var t = 0L
        var waveLen = 9.0 * rate; var wavePos = 0.0
        var smoothGain = 0.0
        val dropDecay = exp(-1.0 / (0.012 * rate))
        try {
            while (running) {
                val k = kind
                for (i in 0 until frames) {
                    // the volume follows [gain] slowly: no clicks when the timer lowers it
                    smoothGain += (gain.coerceIn(0f, 1f) - smoothGain) * 0.0005
                    val fadeIn = minOf(1.0, t / (3.0 * rate))
                    var l: Double; var r: Double
                    when (k) {
                        Kind.RAIN -> {
                            // a soft hiss (white noise through a gentle low-pass) plus scattered drops
                            ll += 0.18 * ((rnd.nextDouble() * 2 - 1) - ll)
                            lr += 0.18 * ((rnd.nextDouble() * 2 - 1) - lr)
                            if (rnd.nextDouble() < 18.0 / rate) { drop = 0.5 + rnd.nextDouble() * 0.5; dropPan = rnd.nextDouble() }
                            val d = drop * (rnd.nextDouble() * 2 - 1)
                            drop *= dropDecay
                            l = 0.55 * ll + 0.25 * d * (1 - dropPan)
                            r = 0.55 * lr + 0.25 * d * dropPan
                        }
                        Kind.OCEAN -> {
                            bl = (bl + 0.02 * (rnd.nextDouble() * 2 - 1)) / 1.02
                            br = (br + 0.02 * (rnd.nextDouble() * 2 - 1)) / 1.02
                            val p = wavePos / waveLen
                            val swell = if (p < 0.4) sin(p / 0.4 * PI / 2) else sin((1 - (p - 0.4) / 0.6) * PI / 2)
                            val env = 0.25 + 0.75 * swell * swell
                            l = bl * 3.2 * env; r = br * 3.2 * env
                            wavePos++
                            if (wavePos >= waveLen) { wavePos = 0.0; waveLen = (7.5 + rnd.nextDouble() * 3.5) * rate }
                        }
                        Kind.NOISE -> {
                            // steady deep (brown) noise, like a fan in another room
                            bl = (bl + 0.02 * (rnd.nextDouble() * 2 - 1)) / 1.02
                            br = (br + 0.02 * (rnd.nextDouble() * 2 - 1)) / 1.02
                            l = bl * 2.6; r = br * 2.6
                        }
                    }
                    val g = 0.5 * 32767 * fadeIn * smoothGain
                    buf[2 * i] = (l * g).toInt().coerceIn(-32767, 32767).toShort()
                    buf[2 * i + 1] = (r * g).toInt().coerceIn(-32767, 32767).toShort()
                    t++
                }
                track.write(buf, 0, buf.size)
            }
        } catch (_: Exception) {
        } finally {
            runCatching { track.pause(); track.flush(); track.release() }
        }
    }
}
