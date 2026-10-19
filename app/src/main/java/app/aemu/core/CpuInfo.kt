package app.aemu.core

import android.os.Build
import java.io.File
import java.util.concurrent.TimeUnit

object CpuInfo {
    /**
     * Checks if the host device CPU and kernel support 32-bit ARM (AArch32 EL0) execution.
     * On 64-bit-only devices (e.g. Snapdragon 8 Gen 3/4, 8 Elite, Dimensity 9300),
     * this will return false. On devices with AArch32 support, it returns true.
     *
     * Modified for AEmulator X, 2026-10-19: a phone with a 64-bit-only Android lists no 32-bit ABI even when its CPU
     * and kernel still run 32-bit code, and a 64-bit process never sees the 32-bit feature names in /proc/cpuinfo, so
     * the old checks said "no" on such phones. Now a tiny 32-bit ARM program is run once: if the kernel starts it,
     * hardware 32-bit execution works.
     */
    val hasAArch32: Boolean by lazy {
        Build.SUPPORTED_32_BIT_ABIS.isNotEmpty() || runsArm32Probe() || runCatching {
            val f = File("/proc/cpuinfo")
            if (f.canRead()) {
                val t = f.readText()
                t.contains("Features") && (t.contains("swp") || t.contains("half") || t.contains("thumb"))
            } else false
        }.getOrDefault(false)
    }

    /**
     * True if host is running on a 64-bit CPU.
     */
    val is64Bit: Boolean by lazy {
        Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
    }

    /** Exit code of [ARM32_PROBE]. */
    const val PROBE_EXIT = 42

    /**
     * A complete static 32-bit ARM (EABI5) program, 96 bytes: the ELF header, one loadable segment at 0x10000 and
     * three instructions, `mov r0, #42; mov r7, #1 (exit); svc #0`.
     */
    val ARM32_PROBE: ByteArray by lazy {
        val b = java.nio.ByteBuffer.allocate(96).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val base = 0x10000
        // ELF header (52 bytes)
        b.put(byteArrayOf(0x7f, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte(), 1, 1, 1, 0)) // 32-bit, LE, v1
        b.put(ByteArray(8))
        b.putShort(2)                // ET_EXEC
        b.putShort(40)               // EM_ARM
        b.putInt(1)                  // EV_CURRENT
        b.putInt(base + 84)          // entry: the code after the headers
        b.putInt(52)                 // program headers right after this header
        b.putInt(0)                  // no section headers
        b.putInt(0x05000000)         // EABI version 5
        b.putShort(52); b.putShort(32); b.putShort(1)   // ehsize, phentsize, phnum
        b.putShort(0); b.putShort(0); b.putShort(0)      // shentsize, shnum, shstrndx
        // program header (32 bytes): PT_LOAD of the whole file, read + execute
        b.putInt(1); b.putInt(0); b.putInt(base); b.putInt(base)
        b.putInt(96); b.putInt(96); b.putInt(5); b.putInt(0x1000)
        // code (12 bytes)
        b.putInt(0xe3a0002a.toInt())  // mov r0, #42
        b.putInt(0xe3a07001.toInt())  // mov r7, #1   (__NR_exit)
        b.putInt(0xef000000.toInt())  // svc #0
        b.array()
    }

    /** What the 32-bit test program did ("exit 42", "Exec format error"...), shown under the setting when it failed. */
    @Volatile var probeResult: String = "not run"
        private set

    /** Runs [ARM32_PROBE] from the app's cache; true when it exits with [PROBE_EXIT]. Never throws. */
    private fun runsArm32Probe(): Boolean = runCatching {
        val dir = File(System.getProperty("java.io.tmpdir") ?: run { probeResult = "no cache directory"; return@runCatching false })
        val f = File(dir, "aemu-arm32-probe")
        f.writeBytes(ARM32_PROBE)
        f.setExecutable(true, true)
        val p = ProcessBuilder(f.absolutePath).redirectErrorStream(true).start()
        val done = p.waitFor(3, TimeUnit.SECONDS)
        if (!done) { p.destroyForcibly(); probeResult = "timed out"; return@runCatching false }
        f.delete()
        val out = runCatching { p.inputStream.bufferedReader().readText().trim().take(120) }.getOrDefault("")
        // the kernel refused the 32-bit program (ENOEXEC) and the C library's fallback then ran it through the shell,
        // which reports a "syntax error" on the binary: the chip has no 32-bit hardware (Snapdragon 8s Gen 3 seen)
        probeResult = if (p.exitValue() != PROBE_EXIT && out.contains("syntax error")) "exec format error"
            else "exit ${p.exitValue()}" + if (out.isNotEmpty()) ": $out" else ""
        p.exitValue() == PROBE_EXIT
    }.getOrElse {
        val m = it.message ?: it.javaClass.simpleName
        probeResult = if (m.contains("error=8") || m.contains("Exec format", true)) "exec format error" else m.substringAfterLast("error=").take(120)
        false
    }

    /** True once the test showed the kernel cannot run 32-bit code at all (no AArch32 in the chip). */
    val noArm32Hardware: Boolean get() = probeResult == "exec format error"
}
