/* Added for AEmulator X, 2026-10-19: short animated GIF recordings. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.graphics.Bitmap
import com.squareup.gifencoder.FloydSteinbergDitherer
import com.squareup.gifencoder.GifEncoder
import com.squareup.gifencoder.ImageOptions
import com.squareup.gifencoder.MedianCutQuantizer
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Frames of the guest screen, shrunk to [WIDTH] px, collected at [FPS] for at most [MAX_SECONDS], then encoded as a
 * looping GIF (gifencoder, Apache-2.0). Small enough to share in a chat.
 */
class GifRecorder(private val fps: Int = FPS) {
    private val frames = ArrayList<IntArray>()
    var w = 0; private set
    var h = 0; private set

    val full: Boolean get() = frames.size >= maxOf(FPS * MAX_SECONDS, ReplayRing.FPS * ReplayRing.SECONDS)
    val count: Int get() = frames.size

    /** Adds one frame (any size; it is scaled to [WIDTH] keeping the aspect). */
    @Synchronized fun add(src: Bitmap) {
        if (full) return
        val (fw, fh) = size(src.width, src.height)
        if (w == 0) { w = fw; h = fh }
        val small = Bitmap.createScaledBitmap(src, w, h, true)
        val px = IntArray(w * h); small.getPixels(px, 0, w, 0, 0, w, h)
        if (small !== src) small.recycle()
        frames.add(px)
    }

    /** The GIF, or null when nothing was recorded. Slow (seconds): call it off the main thread. */
    @Synchronized fun encode(): ByteArray? {
        if (frames.isEmpty()) return null
        val out = ByteArrayOutputStream()
        val enc = GifEncoder(out, w, h, 0)
        val opts = ImageOptions().setDelay(1000L / fps, TimeUnit.MILLISECONDS)
            .setColorQuantizer(MedianCutQuantizer.INSTANCE).setDitherer(FloydSteinbergDitherer.INSTANCE)
        for (px in frames) {
            val rgb = Array(h) { y -> IntArray(w) { x -> px[y * w + x] and 0xffffff } }
            enc.addImage(rgb, opts)
        }
        enc.finishEncoding()
        return out.toByteArray()
    }

    companion object {
        const val WIDTH = 240
        const val FPS = 10
        const val MAX_SECONDS = 8

        /** [WIDTH] wide, height in proportion (at least 1). */
        fun size(srcW: Int, srcH: Int): Pair<Int, Int> =
            WIDTH to (WIDTH.toLong() * srcH / srcW.coerceAtLeast(1)).toInt().coerceAtLeast(1)
    }
}
