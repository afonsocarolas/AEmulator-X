/* Added for AEmulator X, 2026-10-16: experimental 64-bit firmware probe. GPL-3.0; see LICENSE. */
package app.aemu.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files

class Arm64ProbeTest {
    /** A minimal 64-bit little-endian ELF: header + one PT_INTERP program header + the interpreter string. */
    private fun elf64(machine: Int, interp: String?): ByteArray {
        val s = (interp ?: "").toByteArray() + 0
        val b = ByteBuffer.allocate(64 + 56 + s.size).order(ByteOrder.LITTLE_ENDIAN)
        b.put(byteArrayOf(0x7f, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte(), 2, 1, 1))
        b.putShort(0x12, machine.toShort()); b.putLong(0x20, 64); b.putShort(0x36, 56)
        b.putShort(0x38, if (interp != null) 1 else 0)
        b.putInt(64, 3); b.putLong(64 + 0x08, 120); b.putLong(64 + 0x20, s.size.toLong())
        b.position(120); b.put(s)
        return b.array()
    }

    @Test fun readsArm64InterpreterAndStaticPrograms() {
        val dir = Files.createTempDirectory("probe").toFile()
        try {
            val dyn = File(dir, "sh").apply { writeBytes(elf64(183, "/system/bin/linker64")) }
            val e = Arm64Probe.elf(dyn)!!
            assertTrue(e.is64); assertEquals(183, e.machine); assertEquals("/system/bin/linker64", e.interp); assertFalse(e.static)
            val st = File(dir, "init").apply { writeBytes(elf64(183, null)) }
            assertTrue(Arm64Probe.elf(st)!!.static)
            assertNull(Arm64Probe.elf(File(dir, "txt").apply { writeText("#!/system/bin/sh\n") }))
        } finally { dir.deleteRecursively() }
    }

    @Test fun interpreterIsRewrittenInPlace() {
        val dir = Files.createTempDirectory("patch").toFile()
        try {
            val sh = File(dir, "sh").apply { writeBytes(elf64(183, "/system/bin/linker64")) }
            val out = Arm64Probe.patchInterp(sh, File(dir, "sh-d"), "./linker64")!!
            assertEquals("./linker64", Arm64Probe.elf(out)!!.interp)
            assertEquals(sh.length(), out.length())
            assertNull(Arm64Probe.patchInterp(sh, File(dir, "sh-x"), "/a/path/that/is/far/too/long/linker64"))
        } finally { dir.deleteRecursively() }
    }

    @Test fun absoluteLinksResolveInsideTheGuestTree() {
        val root = Files.createTempDirectory("root").toFile()
        try {
            val mksh = File(root, "system/bin/mksh").apply { parentFile.mkdirs(); writeBytes(elf64(183, "/system/bin/linker64")) }
            runCatching { Files.createSymbolicLink(File(root, "system/bin/sh").toPath(), File("/system/bin/mksh").toPath()) }
                .onFailure { return } // no symlinks on this host (Windows without privileges)
            assertEquals(mksh.canonicalPath, Arm64Probe.resolve(root, "/system/bin/sh")!!.canonicalPath)
            assertNull(Arm64Probe.resolve(root, "/system/bin/missing"))
        } finally { root.deleteRecursively() }
    }
}
