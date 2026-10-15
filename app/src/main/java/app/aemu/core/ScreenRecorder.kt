/* Added for AEmulator X, 2026-10-15: screen recording of the running guest. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Surface
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Records the guest screen (and the guest's own sound) to an MP4:
 *   frames: the caller grabs the guest screen (PixelCopy of the GPU-bridge SurfaceView, or fb0 via [FbGrabber])
 *           and hands it to [submitFrame], which draws it onto the H.264 encoder's input surface;
 *   sound:  [submitPcm] is fed from AudioOut's tap, gaps are filled with silence so audio stays in step with video;
 *   mux:    one thread drains both encoders into a MediaMuxer in the app cache;
 *   [stop]  finishes the file and moves it to /sdcard/Movies/AEmulator/<timestamp>-<VM id>.mp4.
 * Only the guest screen is recorded — the host status bar, menus and the recording indicator are not.
 */
class ScreenRecorder(
    private val ctx: Context,
    private val vmId: String,
    guestW: Int,
    guestH: Int,
    /** guest PCM rate (AudioOut.sampleRate); 0 records video only */
    private val audioRate: Int,
    private val log: (String) -> Unit = {},
) {
    val width: Int
    val height: Int

    init {
        // long side between 640 and 1280 (old 320×480 screens are doubled so they stay sharp), both sides even
        val long = maxOf(guestW, guestH).coerceAtLeast(1)
        val scale = long.coerceIn(MIN_LONG, MAX_LONG).toFloat() / long
        width = (Math.round(guestW * scale) and 1.inv()).coerceAtLeast(2)
        height = (Math.round(guestH * scale) and 1.inv()).coerceAtLeast(2)
    }

    private val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    private val name = "$stamp-$vmId.mp4"
    private val tmp = File(ctx.cacheDir, "rec-$name")

    private lateinit var video: MediaCodec
    private var audio: MediaCodec? = null
    private lateinit var input: Surface
    private lateinit var muxer: MediaMuxer
    private val frameLock = Object()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val dst = Rect()

    @Volatile private var stopping = false
    @Volatile var startedAtMs = 0L
        private set
    private var baseUs = 0L

    private var videoTrack = -1
    private var audioTrack = -1
    @Volatile private var muxing = false
    private var videoEos = false
    private var audioEos = false
    private var gotKey = false
    private var lastVideoPts = -1L
    private var lastAudioPts = -1L
    private var wroteVideo = 0

    private val pcm = LinkedBlockingQueue<ByteArray>(256)
    private var muxThread: Thread? = null
    private var audioThread: Thread? = null

    /** Sets up the encoders and starts the threads; false when this phone cannot encode. */
    fun start(): Boolean = synchronized(frameLock) { if (stopping) false else open() }

    private fun open(): Boolean = runCatching {
        val vf = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, (width.toLong() * height * FPS / 6).toInt().coerceIn(2_000_000, 12_000_000))
            setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        video = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(vf, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
        input = video.createInputSurface()
        if (audioRate > 0) audio = runCatching {
            val af = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, audioRate, 2).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 160_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
            }
            MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply { configure(af, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE) }
        }.onFailure { log("rec: no audio encoder: ${it.message}") }.getOrNull()
        tmp.delete()
        muxer = MediaMuxer(tmp.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        video.start()
        audio?.start()
        baseUs = System.nanoTime() / 1000
        startedAtMs = System.currentTimeMillis()
        muxThread = Thread({ muxLoop() }, "aemu-rec-mux").apply { isDaemon = true; start() }
        if (audio != null) audioThread = Thread({ audioLoop() }, "aemu-rec-audio").apply { isDaemon = true; start() }
        log("rec: started ${width}x$height, audio ${if (audio != null) "$audioRate Hz" else "off"}")
        true
    }.getOrElse {
        log("rec: start failed: ${it.message}")
        release()
        tmp.delete()
        false
    }

    /** Draws one guest frame onto the encoder (any thread); the frame is scaled to fill the video. */
    fun submitFrame(bmp: Bitmap) {
        synchronized(frameLock) {
            if (stopping) return
            val c = runCatching { if (Build.VERSION.SDK_INT >= 23) input.lockHardwareCanvas() else input.lockCanvas(null) }.getOrNull() ?: return
            try {
                dst.set(0, 0, width, height)
                c.drawColor(android.graphics.Color.BLACK)
                c.drawBitmap(bmp, null, dst, paint)
            } finally {
                runCatching { input.unlockCanvasAndPost(c) }
            }
        }
    }

    /** Guest PCM from AudioOut's tap (16-bit stereo); copied, never blocks the audio thread. */
    fun submitPcm(buf: ByteArray, n: Int) {
        if (stopping || audio == null || n <= 0) return
        pcm.offer(buf.copyOf(n))
    }

    /** Blocking: finishes the file and saves it; returns the phone path, or null when nothing usable was recorded. */
    fun stop(): String? {
        synchronized(frameLock) {
            if (stopping) return null
            stopping = true
            runCatching { video.signalEndOfInputStream() }
        }
        audioThread?.join(3000)
        muxThread?.join(5000)
        val ok = muxing && wroteVideo > 0
        runCatching { if (muxing) muxer.stop() }.onFailure { log("rec: finish failed: ${it.message}") }
        release()
        val saved = if (ok && tmp.length() > 0) runCatching { save() }.onFailure { log("rec: save failed: ${it.message}") }.getOrNull() else null
        tmp.delete()
        log("rec: stopped, $wroteVideo frames → ${saved ?: "nothing saved"}")
        return saved
    }

    private fun release() {
        runCatching { video.stop() }; runCatching { video.release() }
        runCatching { audio?.stop() }; runCatching { audio?.release() }
        runCatching { input.release() }
        runCatching { muxer.release() }
    }

    // --- audio: guest PCM, with silence wherever the guest was quiet ---

    private fun audioLoop() {
        val codec = audio ?: return
        val bytesPerSec = audioRate * 4L
        var fed = 0L
        val silence = ByteArray(4096)
        fun feed(data: ByteArray, len: Int, eos: Boolean = false) {
            var off = 0
            while (off < len || eos) {
                val idx = codec.dequeueInputBuffer(10_000)
                if (idx < 0) { if (stopping && !eos) return; continue }
                val b = codec.getInputBuffer(idx) ?: return
                b.clear()
                val n = minOf(len - off, b.remaining())
                b.put(data, off, n)
                val pts = fed * 1_000_000 / bytesPerSec
                fed += n; off += n
                val last = eos && off >= len
                codec.queueInputBuffer(idx, 0, n, pts, if (last) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                if (last) return
            }
        }
        fun silenceUntil(targetBytes: Long) {
            var gap = (targetBytes - fed) and 3L.inv()
            while (gap > 0 && !stopping) {
                val n = minOf(gap, silence.size.toLong()).toInt()
                feed(silence, n); gap -= n
            }
        }
        try {
            while (!stopping) {
                val chunk = pcm.poll(20, TimeUnit.MILLISECONDS)
                val clock = (System.nanoTime() / 1000 - baseUs) * bytesPerSec / 1_000_000
                if (chunk != null) {
                    if (clock - fed > bytesPerSec / 10) silenceUntil(clock) // the guest was quiet for a while
                    feed(chunk, chunk.size)
                } else if (clock - fed > bytesPerSec / 20) silenceUntil(clock)
            }
            feed(silence, 0, eos = true)
        } catch (e: Exception) {
            log("rec: audio stopped: ${e.message}")
            audioEos = true
        }
    }

    // --- muxer: drains both encoders ---

    private fun muxLoop() {
        val info = MediaCodec.BufferInfo()
        try {
            while (true) {
                var busy = drain(video, info, true)
                audio?.let { if (!audioEos && drain(it, info, false)) busy = true }
                if (videoEos && (audio == null || audioEos)) break
                if (!busy) Thread.sleep(5)
            }
        } catch (e: Exception) {
            log("rec: encoder stopped: ${e.message}")
        }
    }

    /** One pass over [codec]'s output; true if something came out. */
    private fun drain(codec: MediaCodec, info: MediaCodec.BufferInfo, isVideo: Boolean): Boolean {
        var any = false
        while (true) {
            val idx = codec.dequeueOutputBuffer(info, 0)
            when {
                idx == MediaCodec.INFO_TRY_AGAIN_LATER -> return any
                idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (isVideo) videoTrack = muxer.addTrack(codec.outputFormat) else audioTrack = muxer.addTrack(codec.outputFormat)
                    if (!muxing && videoTrack >= 0 && (audio == null || audioTrack >= 0)) {
                        muxer.start(); muxing = true
                        // frames encoded before the muxer was ready are lost; start the file on a fresh key frame
                        runCatching { video.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0) }) }
                    }
                }
                idx >= 0 -> {
                    any = true
                    val buf = codec.getOutputBuffer(idx)
                    val config = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    val key = info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0
                    if (buf != null && muxing && !config && info.size > 0 && (!isVideo || gotKey || key)) {
                        if (isVideo) gotKey = true
                        // video timestamps come from the input surface (System.nanoTime), audio from the sample count
                        var pts = if (isVideo) info.presentationTimeUs - baseUs else info.presentationTimeUs
                        val last = if (isVideo) lastVideoPts else lastAudioPts
                        if (pts <= last) pts = last + 1
                        if (pts >= 0) {
                            info.presentationTimeUs = pts
                            buf.position(info.offset); buf.limit(info.offset + info.size)
                            muxer.writeSampleData(if (isVideo) videoTrack else audioTrack, buf, info)
                            if (isVideo) { lastVideoPts = pts; wroteVideo++ } else lastAudioPts = pts
                        }
                    }
                    codec.releaseOutputBuffer(idx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        if (isVideo) videoEos = true else audioEos = true
                        return true
                    }
                }
                else -> return any
            }
        }
    }

    // --- saving: same folder scheme as Screenshot (direct file, MediaStore on scoped storage) ---

    private fun save(): String? {
        val shown = "/sdcard/$PHONE_REL/$name"
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "AEmulator")
        val dst = File(dir, name)
        val direct = runCatching {
            if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) error("cannot create ${dir.absolutePath}")
            tmp.inputStream().use { i -> dst.outputStream().use { o -> i.copyTo(o) } }
            if (dst.length() != tmp.length()) error("short write")
        }.isSuccess
        if (direct) {
            MediaScannerConnection.scanFile(ctx, arrayOf(dst.absolutePath), arrayOf("video/mp4"), null)
            return shown
        }
        dst.delete()
        if (Build.VERSION.SDK_INT >= 29) {
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, PHONE_REL)
            }
            val uri = ctx.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv) ?: return null
            val wrote = runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { o -> tmp.inputStream().use { it.copyTo(o) } } }.isSuccess
            if (!wrote) { runCatching { ctx.contentResolver.delete(uri, null, null) }; return null }
            return shown
        }
        return null
    }

    /** Reads fb0 into one reusable bitmap per frame (software display mode, recovery, Samsung boot logos). */
    class FbGrabber(private val w: Int, private val h: Int) {
        private var data = ByteArray(0)
        private val px = IntArray(w * h)
        val bitmap: Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        /** Same formats and paging as Screenshot.take; false when the frame could not be read. */
        fun grab(fb: File?, format: Int, pages: Int, page: Int): Boolean {
            if (fb == null || !fb.isFile) return false
            val bpp = if (format == 0) 2 else 4
            val size = w * h * bpp
            if (data.size != size) data = ByteArray(size)
            val ok = runCatching {
                RandomAccessFile(fb, "r").use { f ->
                    val off = (if (pages == 2) page else 0).toLong() * size
                    f.seek(if (f.length() >= off + size) off else 0L)
                    f.readFully(data)
                }
            }.isSuccess
            if (!ok) return false
            Screenshot.decodePixels(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), px, format)
            bitmap.setPixels(px, 0, w, 0, 0, w, h)
            return true
        }
    }

    companion object {
        const val FPS = 30
        private const val MIN_LONG = 640
        private const val MAX_LONG = 1280
        private const val PHONE_REL = "Movies/AEmulator"
    }
}
