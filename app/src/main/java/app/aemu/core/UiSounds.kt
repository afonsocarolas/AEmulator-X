/* Added for AEmulator X, 2026-10-21: sound effects. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Small sounds for the app's own moments (not the running system's): starting a firmware, a screenshot, a badge, a
 * daily challenge, a birthday, Dessert Snake. Made in code (sine bells and a noise click), so there are no sound
 * files to license. Off with Settings › Appearance › Sound effects.
 */
object UiSounds {
    enum class Sfx { START, SHUTTER, BADGE, CHALLENGE, PARTY, POP, GAME_OVER }

    const val RATE = 44100
    private const val PREF = "x_sfx"

    fun enabled(ctx: Context): Boolean = app.aemu.AppPrefs.prefs(ctx).getBoolean(PREF, true)
    fun setEnabled(ctx: Context, on: Boolean) = app.aemu.AppPrefs.prefs(ctx).edit().putBoolean(PREF, on).apply()

    private val cache = HashMap<Sfx, ShortArray>()

    /** Plays [s] once, in the background, when sound effects are on. */
    fun play(ctx: Context, s: Sfx) {
        if (!enabled(ctx)) return
        Thread({
            val pcm = synchronized(cache) { cache.getOrPut(s) { render(s) } }
            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
            }.getOrNull() ?: return@Thread
            runCatching {
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(pcm.size * 1000L / RATE + 80)
            }
            runCatching { track.stop() }
            track.release()
        }, "aemu-sfx").apply { isDaemon = true }.start()
    }

    private fun midi(n: Double) = 440.0 * 2.0.pow((n - 69) / 12)

    /** One soft bell note: [freq] Hz from [at] s, [len] s long, with an octave overtone. */
    private fun bell(out: DoubleArray, freq: Double, at: Double, len: Double, gain: Double = 0.5) {
        val start = (at * RATE).toInt(); val n = (len * RATE).toInt()
        for (i in 0 until n) {
            val k = start + i; if (k >= out.size) break
            val t = i.toDouble() / RATE
            val attack = minOf(1.0, t / 0.006)
            val env = attack * exp(-t * 5.5 / len)
            out[k] += gain * env * (sin(2 * PI * freq * t) + 0.3 * sin(4 * PI * freq * t) + 0.08 * sin(6 * PI * freq * t))
        }
    }

    /** The samples of [s], mono 16-bit at [RATE]. */
    fun render(s: Sfx): ShortArray {
        val notes: List<Triple<Double, Double, Double>> = when (s) {        // midi note, start s, length s
            Sfx.START -> listOf(Triple(72.0, 0.0, 0.35), Triple(76.0, 0.07, 0.35), Triple(79.0, 0.14, 0.35), Triple(84.0, 0.21, 0.6))
            Sfx.BADGE -> listOf(Triple(67.0, 0.0, 0.3), Triple(72.0, 0.1, 0.3), Triple(76.0, 0.2, 0.3), Triple(79.0, 0.3, 0.8), Triple(84.0, 0.3, 0.8))
            Sfx.CHALLENGE -> listOf(Triple(76.0, 0.0, 0.25), Triple(79.0, 0.09, 0.25), Triple(84.0, 0.18, 0.25), Triple(88.0, 0.27, 0.25),
                Triple(91.0, 0.36, 0.9), Triple(84.0, 0.36, 0.9))
            // "happy birthday"-ish opening: sol sol la sol do si
            Sfx.PARTY -> listOf(Triple(67.0, 0.0, 0.2), Triple(67.0, 0.18, 0.2), Triple(69.0, 0.36, 0.35), Triple(67.0, 0.72, 0.35),
                Triple(72.0, 1.08, 0.35), Triple(71.0, 1.44, 0.7))
            Sfx.POP -> listOf(Triple(84.0, 0.0, 0.08), Triple(91.0, 0.03, 0.1))
            Sfx.GAME_OVER -> listOf(Triple(72.0, 0.0, 0.3), Triple(67.0, 0.18, 0.3), Triple(63.0, 0.36, 0.3), Triple(60.0, 0.54, 0.7))
            Sfx.SHUTTER -> emptyList()
        }
        val len = if (s == Sfx.SHUTTER) 0.16 else notes.maxOf { it.second + it.third } + 0.05
        val out = DoubleArray((len * RATE).toInt())
        if (s == Sfx.SHUTTER) {
            // two short noise clicks: the shutter opens, then closes
            val rnd = Random(7)
            for ((at, g) in listOf(0.0 to 0.55, 0.07 to 0.4)) {
                val start = (at * RATE).toInt()
                for (i in 0 until (0.045 * RATE).toInt()) {
                    val k = start + i; if (k >= out.size) break
                    out[k] += g * exp(-i / (0.008 * RATE)) * (rnd.nextDouble() * 2 - 1)
                }
            }
        } else for ((n, at, l) in notes) bell(out, midi(n), at, l, if (s == Sfx.POP) 0.35 else 0.3)
        val peak = out.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-9)
        val scale = 0.6 / maxOf(peak, 0.6)                                   // never louder than 60 % of full scale
        return ShortArray(out.size) { (out[it] * scale * 32767).toInt().coerceIn(-32767, 32767).toShort() }
    }
}
