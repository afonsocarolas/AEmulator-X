package app.aemu.core

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CpuInfoProbeTest {
    private val p = CpuInfo.ARM32_PROBE
    private val b = ByteBuffer.wrap(p).order(ByteOrder.LITTLE_ENDIAN)

    @Test fun isA32BitArmExecutable() {
        assertEquals(96, p.size)
        assertArrayEquals(byteArrayOf(0x7f, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte(), 1, 1), p.copyOf(6))
        assertEquals(2, b.getShort(16).toInt())            // ET_EXEC
        assertEquals(40, b.getShort(18).toInt())           // EM_ARM
        assertEquals(0x10054, b.getInt(24))                // entry = first instruction
        assertEquals(0x05000000, b.getInt(36))             // EABI5
        assertEquals(1, b.getShort(44).toInt())            // one program header
    }
    @Test fun loadsItselfAndExitsWith42() {
        assertEquals(1, b.getInt(52))                      // PT_LOAD
        assertEquals(0x10000, b.getInt(60))                // vaddr
        assertEquals(96, b.getInt(68)); assertEquals(96, b.getInt(72))
        assertEquals(5, b.getInt(76))                      // R + X
        assertEquals(0xe3a00000.toInt() or CpuInfo.PROBE_EXIT, b.getInt(84))   // mov r0, #42
        assertEquals(0xe3a07001.toInt(), b.getInt(88))     // mov r7, #1 (exit)
        assertEquals(0xef000000.toInt(), b.getInt(92))     // svc #0
    }
}
