/* Added for AEmulator X, 2026-10-16: VNC server for the running firmware. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A small RFB 3.3/3.7/3.8 server: any VNC viewer on the network sees the running firmware and drives it.
 *   security: VNC authentication only (random per-session password from [password]), never "None";
 *   encodings: Raw for the changed 32×32 tiles of each frame, DesktopSize when the guest resolution changes;
 *   pixel formats: whatever true-colour format the viewer asks for (8/16/32 bpp, either byte order).
 * Frames come from [submitFrame] (opaque ARGB ints); input goes out through [onPointer] (guest pixels, RFB button
 * mask) and [onKey] (X11 keysyms). Plain java.net, so it is tested on the JVM.
 */
class VncServer(
    private val desktopName: String,
    val password: String,
    private val onPointer: (x: Int, y: Int, buttons: Int) -> Unit,
    private val onKey: (keysym: Int, down: Boolean) -> Unit,
    private val log: (String) -> Unit = {},
) {
    companion object {
        private const val TILE = 32
        private const val ENC_RAW = 0
        private const val ENC_DESKTOP_SIZE = -223
        private val RNG = SecureRandom()

        /** 8 characters (all a VNC password can use) without look-alikes (0/O, 1/l/I). */
        fun newPassword(): String {
            val a = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            return String(CharArray(8) { a[RNG.nextInt(a.length)] })
        }
    }

    private class Frame(val w: Int, val h: Int, val px: IntArray, val seq: Long)

    private val frameLock = Object()
    @Volatile private var frame: Frame? = null
    private var seq = 0L
    private var server: ServerSocket? = null
    private val viewers = CopyOnWriteArrayList<Viewer>()
    @Volatile private var stopped = false
    /** called when the number of connected viewers changes */
    @Volatile var onViewers: ((Int) -> Unit)? = null
    val viewerCount get() = viewers.size
    val port get() = server?.localPort ?: 0

    /** Listens on [firstPort]…[firstPort]+9 (0 = any free port); returns the port. */
    fun start(firstPort: Int = 5900, bind: InetAddress? = null): Int {
        val s = if (firstPort == 0) ServerSocket(0, 4, bind) else
            (firstPort until firstPort + 10).firstNotNullOfOrNull { p -> runCatching { ServerSocket().apply {
                reuseAddress = true; bind(InetSocketAddress(bind, p), 4) } }.getOrNull() } ?: error("no free port from $firstPort")
        server = s
        Thread({
            while (!stopped) {
                val c = runCatching { s.accept() }.getOrNull() ?: break
                Thread({ Viewer(c).run() }, "vnc-viewer").apply { isDaemon = true; start() }
            }
        }, "vnc-accept").apply { isDaemon = true; start() }
        log("vnc: listening on port ${s.localPort}")
        return s.localPort
    }

    fun stop() {
        stopped = true
        runCatching { server?.close() }
        viewers.forEach { it.close() }
        synchronized(frameLock) { (frameLock as Object).notifyAll() }
    }

    /** A new screen ([px] = w×h opaque ARGB); copied, so the caller may reuse its array. */
    fun submitFrame(px: IntArray, w: Int, h: Int) {
        if (w <= 0 || h <= 0 || px.size < w * h) return
        synchronized(frameLock) {
            frame = Frame(w, h, px.copyOf(w * h), ++seq)
            (frameLock as Object).notifyAll()
        }
    }

    private class PixelFormat(val bpp: Int, val bigEndian: Boolean, val rMax: Int, val gMax: Int, val bMax: Int,
                              val rShift: Int, val gShift: Int, val bShift: Int) {
        val bytes get() = bpp / 8
        fun encode(argb: Int, out: ByteArray, off: Int) {
            val r = ((argb ushr 16) and 0xFF) * rMax / 255
            val g = ((argb ushr 8) and 0xFF) * gMax / 255
            val b = (argb and 0xFF) * bMax / 255
            val v = (r shl rShift) or (g shl gShift) or (b shl bShift)
            for (i in 0 until bytes) out[off + i] = (if (bigEndian) v ushr (8 * (bytes - 1 - i)) else v ushr (8 * i)).toByte()
        }
        companion object { val DEFAULT = PixelFormat(32, false, 255, 255, 255, 16, 8, 0) }
    }

    private inner class Viewer(private val sock: Socket) {
        private val input = DataInputStream(sock.getInputStream().buffered())
        private val out = DataOutputStream(BufferedOutputStream(sock.getOutputStream(), 1 shl 16))
        @Volatile private var pf = PixelFormat.DEFAULT
        @Volatile private var desktopSize = false
        @Volatile private var wanted = false
        @Volatile private var wantFull = true
        @Volatile private var closed = false
        private var sentW = 0
        private var sentH = 0
        private var last: IntArray? = null

        fun close() { closed = true; runCatching { sock.close() }; synchronized(frameLock) { (frameLock as Object).notifyAll() } }

        fun run() {
            val who = sock.inetAddress?.hostAddress ?: "?"
            try {
                sock.tcpNoDelay = true
                if (!handshake()) { log("vnc: $who failed to authenticate"); Thread.sleep(1500); return }
                viewers += this; onViewers?.invoke(viewers.size)
                log("vnc: $who connected")
                Thread({ runCatching { readLoop() }; close() }, "vnc-read").apply { isDaemon = true; start() }
                writeLoop()
            } catch (_: EOFException) {
            } catch (e: Exception) {
                if (!closed && !stopped) log("vnc: $who: ${e.message}")
            } finally {
                close()
                if (viewers.remove(this)) { onViewers?.invoke(viewers.size); log("vnc: $who disconnected") }
            }
        }

        private fun handshake(): Boolean {
            out.write("RFB 003.008\n".toByteArray(Charsets.US_ASCII)); out.flush()
            val v = ByteArray(12); input.readFully(v)
            val minor = String(v, Charsets.US_ASCII).substring(8, 11).toIntOrNull() ?: 3
            if (minor >= 7) {
                out.writeByte(1); out.writeByte(2); out.flush() // one security type: VNC authentication
                if (input.readUnsignedByte() != 2) return false
            } else { out.writeInt(2); out.flush() }               // 3.3: the server decides
            val challenge = ByteArray(16).also { RNG.nextBytes(it) }
            out.write(challenge); out.flush()
            val answer = ByteArray(16); input.readFully(answer)
            val ok = answer.contentEquals(VncDes.response(challenge, password))
            out.writeInt(if (ok) 0 else 1)
            if (!ok && minor >= 8) { val why = "Wrong password".toByteArray(); out.writeInt(why.size); out.write(why) }
            out.flush()
            if (!ok) return false
            input.readUnsignedByte() // ClientInit: shared flag (several viewers are always allowed)
            val f = waitFrame(0) ?: return false
            serverInit(f.w, f.h)
            return true
        }

        private fun serverInit(w: Int, h: Int) {
            out.writeShort(w); out.writeShort(h)
            out.writeByte(32); out.writeByte(24); out.writeByte(0); out.writeByte(1)
            out.writeShort(255); out.writeShort(255); out.writeShort(255)
            out.writeByte(16); out.writeByte(8); out.writeByte(0); out.write(ByteArray(3))
            val name = desktopName.toByteArray(Charsets.UTF_8)
            out.writeInt(name.size); out.write(name); out.flush()
            sentW = w; sentH = h
        }

        private fun readLoop() {
            while (!closed) when (input.readUnsignedByte()) {
                0 -> { // SetPixelFormat
                    input.skipBytes(3)
                    val bpp = input.readUnsignedByte(); input.readUnsignedByte()
                    val big = input.readUnsignedByte() != 0; val tc = input.readUnsignedByte() != 0
                    val rm = input.readUnsignedShort(); val gm = input.readUnsignedShort(); val bm = input.readUnsignedShort()
                    val rs = input.readUnsignedByte(); val gs = input.readUnsignedByte(); val bs = input.readUnsignedByte()
                    input.skipBytes(3)
                    if (tc && bpp in setOf(8, 16, 32)) { pf = PixelFormat(bpp, big, rm, gm, bm, rs, gs, bs); wantFull = true }
                }
                2 -> { // SetEncodings
                    input.skipBytes(1)
                    val n = input.readUnsignedShort()
                    val encs = IntArray(n) { input.readInt() }
                    desktopSize = ENC_DESKTOP_SIZE in encs
                }
                3 -> { // FramebufferUpdateRequest
                    val incremental = input.readUnsignedByte() != 0
                    input.skipBytes(8)
                    if (!incremental) wantFull = true
                    wanted = true
                    synchronized(frameLock) { (frameLock as Object).notifyAll() }
                }
                4 -> { val down = input.readUnsignedByte() != 0; input.skipBytes(2); onKey(input.readInt(), down) }
                5 -> { val b = input.readUnsignedByte(); val x = input.readUnsignedShort(); val y = input.readUnsignedShort(); onPointer(x, y, b) }
                6 -> { input.skipBytes(3); val n = input.readInt(); input.skipBytes(n) } // ClientCutText
                else -> error("unknown message")
            }
        }

        /** Waits for a frame newer than [after] (or any frame when 0). */
        private fun waitFrame(after: Long): Frame? = synchronized(frameLock) {
            while (!closed && !stopped) {
                val f = frame
                if (f != null && (after == 0L || f.seq > after)) return f
                (frameLock as Object).wait(1000)
            }
            null
        }

        private fun writeLoop() {
            var sentSeq = 0L
            while (!closed && !stopped) {
                // wait until the viewer asked for an update and there is something new to send (or it wants it all)
                val f = synchronized(frameLock) {
                    while (!closed && !stopped && !(wanted && frame != null && (wantFull || frame!!.seq > sentSeq))) (frameLock as Object).wait(1000)
                    frame
                } ?: continue
                if (closed || stopped) break
                wanted = false
                val full = wantFull; wantFull = false
                sentSeq = f.seq
                if (f.w != sentW || f.h != sentH) {
                    if (!desktopSize) { log("vnc: screen size changed and the viewer cannot follow it"); return }
                    out.writeByte(0); out.writeByte(0); out.writeShort(1)
                    out.writeShort(0); out.writeShort(0); out.writeShort(f.w); out.writeShort(f.h); out.writeInt(ENC_DESKTOP_SIZE)
                    out.flush(); sentW = f.w; sentH = f.h; last = null; wanted = true; wantFull = true
                    continue
                }
                val rects = dirty(f, if (full) null else last)
                out.writeByte(0); out.writeByte(0); out.writeShort(rects.size)
                val pfNow = pf
                var row = ByteArray(0)
                for (r in rects) {
                    out.writeShort(r[0]); out.writeShort(r[1]); out.writeShort(r[2]); out.writeShort(r[3]); out.writeInt(ENC_RAW)
                    val need = r[2] * pfNow.bytes
                    if (row.size < need) row = ByteArray(need)
                    for (y in r[1] until r[1] + r[3]) {
                        var o = 0; val base = y * f.w
                        for (x in r[0] until r[0] + r[2]) { pfNow.encode(f.px[base + x], row, o); o += pfNow.bytes }
                        out.write(row, 0, need)
                    }
                }
                out.flush()
                last = f.px
            }
        }
    }

    private fun dirty(f: Frame, prev: IntArray?) = dirtyRects(f.w, f.h, f.px, prev)
}

/**
 * Changed 32×32 tiles of [px] against [prev] as (x, y, w, h); everything when [prev] is null. Touching tiles of a row
 * are merged so a full-width change is one rect. An unchanged screen still answers with one 1×1 rect (viewers wait
 * for a reply to every request).
 */
internal fun dirtyRects(w: Int, h: Int, px: IntArray, prev: IntArray?): List<IntArray> {
    val tile = 32
    if (prev == null || prev.size != px.size) return listOf(intArrayOf(0, 0, w, h))
    val out = ArrayList<IntArray>()
    var ty = 0
    while (ty < h) {
        val th = minOf(tile, h - ty)
        var runStart = -1
        var tx = 0
        // one pass past the last tile (tx >= w) closes a run that reaches the right edge, also when w % 32 != 0
        while (true) {
            val changed = tx < w && run {
                val tw = minOf(tile, w - tx)
                var diff = false
                loop@ for (y in ty until ty + th) {
                    val base = y * w
                    for (x in tx until tx + tw) if (px[base + x] != prev[base + x]) { diff = true; break@loop }
                }
                diff
            }
            if (changed && runStart < 0) runStart = tx
            if (!changed && runStart >= 0) { out += intArrayOf(runStart, ty, minOf(tx, w) - runStart, th); runStart = -1 }
            if (tx >= w) break
            tx += tile
        }
        ty += tile
    }
    if (out.isEmpty()) out += intArrayOf(0, 0, 1, 1)
    return out
}
