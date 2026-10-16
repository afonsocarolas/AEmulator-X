/* Added for AEmulator X, 2026-10-16: VNC server. GPL-3.0; see LICENSE. */
package app.aemu.core

import org.junit.Assert.*
import org.junit.Test
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class VncServerTest {
    @Test fun desMatchesTheStandardTestVector() {
        // FIPS / classic worked example: key 133457799BBCDFF1, plaintext 0123456789ABCDEF -> 85E813540F0AB405
        assertEquals(0x85E813540F0AB405uL.toLong(), VncDes.encryptBlock(0x0123456789ABCDEFL, 0x133457799BBCDFF1L))
    }

    @Test fun keysymsMapToLinuxKeys() {
        assertEquals(30, VncKeys.linux('a'.code)); assertEquals(30, VncKeys.linux('A'.code)); assertEquals(44, VncKeys.linux('z'.code))
        assertEquals(11, VncKeys.linux('0'.code)); assertEquals(2, VncKeys.linux('1'.code))
        assertEquals(InputService.KEY_ENTER, VncKeys.linux(0xff0d)); assertEquals(InputService.KEY_BACK, VncKeys.linux(0xff1b))
        assertEquals(53, VncKeys.linux('?'.code)); assertEquals(57, VncKeys.linux(' '.code)); assertNull(VncKeys.linux(0x20ac))
    }

    @Test fun passwordGeneratorMakesEightUnambiguousCharacters() {
        val p = VncServer.newPassword()
        assertEquals(8, p.length)
        assertTrue(p.none { it in "0O1lI" })
    }

    @Test fun dirtyTilesAreMergedPerRow() {
        val w = 100; val h = 70
        val a = IntArray(w * h); val b = a.copyOf()
        assertEquals(listOf(listOf(0, 0, 1, 1)), dirtyRects(w, h, b, a).map { it.toList() })      // unchanged: 1x1
        b[5 * w + 40] = 1; b[5 * w + 70] = 1                                                       // tiles 1 and 2 of row 0
        assertEquals(listOf(listOf(32, 0, 64, 32)), dirtyRects(w, h, b, a).map { it.toList() })
        b[65 * w + 99] = 1                                                                         // last, partial tile
        assertEquals(listOf(96, 64, 4, 6), dirtyRects(w, h, b, a).last().toList())
        assertEquals(listOf(listOf(0, 0, w, h)), dirtyRects(w, h, b, null).map { it.toList() })
    }

    private class Client(port: Int) {
        val s = Socket(InetAddress.getLoopbackAddress(), port).apply { soTimeout = 5000 }
        val i = DataInputStream(s.getInputStream()); val o = DataOutputStream(s.getOutputStream())
        fun login(pw: String): Int {
            val v = ByteArray(12); i.readFully(v); assertEquals("RFB 003.008\n", String(v))
            o.write("RFB 003.008\n".toByteArray()); o.flush()
            assertEquals(1, i.readUnsignedByte()); assertEquals(2, i.readUnsignedByte())
            o.writeByte(2); o.flush()
            val ch = ByteArray(16); i.readFully(ch)
            o.write(VncDes.response(ch, pw)); o.flush()
            return i.readInt()
        }
    }

    @Test fun viewerLogsInGetsTheScreenThenOnlyChangesAndDrives() {
        val pointer = LinkedBlockingQueue<List<Int>>()
        val srv = VncServer("AEmulator X test", "s3cr3tPW", { x, y, b -> pointer += listOf(x, y, b) }, { _, _ -> })
        val w = 64; val h = 40
        val px = IntArray(w * h) { 0xFF112233.toInt() }
        srv.submitFrame(px, w, h)
        val port = srv.start(0, InetAddress.getLoopbackAddress())
        try {
            val c = Client(port)
            assertEquals(0, c.login("s3cr3tPW"))
            c.o.writeByte(1); c.o.flush()                                 // ClientInit (shared)
            assertEquals(w, c.i.readUnsignedShort()); assertEquals(h, c.i.readUnsignedShort())
            c.i.skipBytes(16); val n = c.i.readInt(); c.i.skipBytes(n)
            // full update
            c.o.writeByte(3); c.o.writeByte(0); repeat(2) { c.o.writeShort(0) }; c.o.writeShort(w); c.o.writeShort(h); c.o.flush()
            assertEquals(0, c.i.readUnsignedByte()); c.i.readUnsignedByte(); assertEquals(1, c.i.readUnsignedShort())
            assertEquals(listOf(0, 0, w, h), List(4) { c.i.readUnsignedShort() }); assertEquals(0, c.i.readInt())
            val data = ByteArray(w * h * 4); c.i.readFully(data)
            assertEquals(listOf(0x33, 0x22, 0x11, 0), data.take(4).map { it.toInt() and 0xFF })   // B G R x
            // one pixel changes: an incremental request gets just its tile
            px[10 * w + 40] = 0xFFFFFFFF.toInt(); srv.submitFrame(px, w, h)
            c.o.writeByte(3); c.o.writeByte(1); repeat(2) { c.o.writeShort(0) }; c.o.writeShort(w); c.o.writeShort(h); c.o.flush()
            assertEquals(0, c.i.readUnsignedByte()); c.i.readUnsignedByte(); assertEquals(1, c.i.readUnsignedShort())
            assertEquals(listOf(32, 0, 32, 32), List(4) { c.i.readUnsignedShort() }); assertEquals(0, c.i.readInt())
            c.i.skipBytes(32 * 32 * 4)
            // a click
            c.o.writeByte(5); c.o.writeByte(1); c.o.writeShort(12); c.o.writeShort(34); c.o.flush()
            assertEquals(listOf(12, 34, 1), pointer.poll(5, TimeUnit.SECONDS))
            assertEquals(1, srv.viewerCount)
            c.s.close()
        } finally { srv.stop() }
    }

    @Test fun wrongPasswordIsRejected() {
        val srv = VncServer("t", "rightpw1", { _, _, _ -> }, { _, _ -> })
        srv.submitFrame(IntArray(4), 2, 2)
        val port = srv.start(0, InetAddress.getLoopbackAddress())
        try { assertEquals(1, Client(port).login("wrongpw1")); assertEquals(0, srv.viewerCount) } finally { srv.stop() }
    }
}
