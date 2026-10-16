/* Added for AEmulator X, 2026-10-16: experimental 64-bit firmware probe. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.os.Build
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * First experiment towards 64-bit firmware. The engine's translator (qemu-arm) only runs 32-bit ARM, but the phone is
 * arm64 itself, so a 64-bit firmware's programs might run natively, without translation. This probe tries exactly
 * that with the firmware's shell and reports what happens; it boots nothing and never writes into the firmware tree
 * (the two programs are copied to the app cache to get the exec bit).
 *
 *   A  host linker64 + guest sh + host libraries      does native exec of a guest arm64 binary work at all?
 *   B  host linker64 + guest sh + guest libraries     can the firmware's own bionic run under today's linker?
 *   C  guest linker64 + guest sh + guest libraries    can the firmware's own linker run on this kernel?
 *
 * B and C are the interesting ones: C is what a native 64-bit engine would need (plus path redirection and binder).
 */
object Arm64Probe {
    private const val SCRIPT = "echo AEMU_PROBE_OK; uname -m; echo \"sh=\$0\""
    /** E: every external command here must come from the firmware's toybox */
    private const val SCRIPT_E = "echo AEMU_PROBE_OK; uname -m; uname -r; toybox --version; echo \"cmd uname: \$(command -v uname)\"; ls / | head -n 4"
    private const val EM_AARCH64 = 183

    data class Elf(val is64: Boolean, val machine: Int, val interp: String?, val static: Boolean)

    /** True when the image looks like 64-bit ARM firmware (worth probing). */
    fun applies(abi: String, root: File) = abi.startsWith("arm64") || File(root, "system/bin/linker64").exists()

    /** Blocking (up to ~35 s). Returns a plain-text report for the user to read or copy. */
    fun run(root: File, cache: File): String = buildString {
        appendLine("AEmulator X 64-bit probe")
        appendLine("host: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.SUPPORTED_ABIS.joinToString()}")
        appendLine("host kernel: ${runCatching { System.getProperty("os.version") }.getOrNull() ?: "?"}")
        val sh = resolve(root, "/system/bin/sh")
        val gLinker = resolve(root, "/system/bin/linker64")
        val lib64 = File(root, "system/lib64")
        appendLine("guest sh: ${sh?.let { describe(it) } ?: "missing"}")
        appendLine("guest linker64: ${gLinker?.let { describe(it) } ?: "missing"}")
        appendLine("guest lib64: ${if (lib64.isDirectory) "${lib64.list()?.size ?: 0} files" else "missing"}")
        val shElf = sh?.let { elf(it) }
        if (sh == null || shElf == null) { appendLine("\nRESULT: no readable guest shell, nothing to run."); return@buildString }
        if (!shElf.is64 || shElf.machine != EM_AARCH64) { appendLine("\nRESULT: the guest shell is not an arm64 program (this firmware is 32-bit)."); return@buildString }

        val dir = File(cache, "arm64probe").apply { deleteRecursively(); mkdirs() }
        val shCopy = copyExec(sh, File(dir, "sh"))
        val linkerCopy = gLinker?.let { copyExec(it, File(dir, "linker64")) }
        val hostLinker = File("/system/bin/linker64")
        val guestLibs = listOf(lib64.path, File(root, "vendor/lib64").path, File(root, "system/vendor/lib64").path).joinToString(":")

        fun test(tag: String, what: String, cmd: List<String>, env: Map<String, String>, cwd: File? = null) {
            appendLine("\n[$tag] $what")
            appendLine("    $ ${cmd.joinToString(" ") { it.substringAfterLast('/') }}${if (env.isNotEmpty()) "   (${env.keys.joinToString()})" else ""}")
            val r = exec(cmd, env, cwd)
            appendLine("    exit: ${r.first}")
            r.second.lines().filter { it.isNotBlank() }.take(12).forEach { appendLine("    | ${it.take(200)}") }
            appendLine("    => ${if (r.second.contains("AEMU_PROBE_OK")) "WORKS" else "fails"}")
        }

        val canRunLinker = Build.VERSION.SDK_INT >= 29 // the linker runs programs given on its command line since Android 10
        if (shCopy == null) appendLine("\ncould not copy the guest shell to the app cache")
        else {
            if (canRunLinker) {
                test("A", "host linker + guest shell + host libraries", listOf(hostLinker.path, shCopy.path, "-c", SCRIPT), emptyMap())
                test("B", "host linker + guest shell + guest libraries", listOf(hostLinker.path, shCopy.path, "-c", SCRIPT),
                    mapOf("LD_LIBRARY_PATH" to guestLibs))
            } else appendLine("\n[A/B] skipped: this phone's linker cannot start programs directly (needs Android 10+)")
            if (linkerCopy != null)
                test("C", "guest linker + guest shell + guest libraries", listOf(linkerCopy.path, shCopy.path, "-c", SCRIPT),
                    mapOf("LD_LIBRARY_PATH" to guestLibs))
            else appendLine("\n[C] skipped: firmware has no linker64")
            // D: linkers before Android 10 cannot be handed a program on their command line (C then only prints its
            // usage). The real way: the kernel starts the program's PT_INTERP. A copy of the shell gets "./linker64",
            // relative to the working directory, i.e. the guest linker copied next to it.
            val patched = if (linkerCopy != null) patchInterp(shCopy, File(dir, "sh-d"), "./linker64") else null
            if (patched != null)
                test("D", "guest shell started by the kernel with the guest linker + guest libraries",
                    listOf(patched.path, "-c", SCRIPT), mapOf("LD_LIBRARY_PATH" to guestLibs), cwd = dir)
            else if (linkerCopy != null) appendLine("\n[D] skipped: could not point the shell at the guest linker")

            // E: D's shell found the host's uname in the host /system/bin. Put the firmware's own toybox (same
            // ./linker64 fix) and links for a few of its commands first on PATH, so the commands are the firmware's.
            val toybox = resolve(root, "/system/bin/toybox")
            val bin = File(dir, "bin").apply { mkdirs() }
            val toyCopy = if (patched != null && toybox != null) patchInterp(toybox, File(bin, "toybox"), "./linker64") else null
            if (toyCopy != null) {
                val applets = listOf("uname", "ls", "cat", "head")
                val linked = applets.count { a -> runCatching { android.system.Os.symlink("toybox", File(bin, a).path) }.isSuccess }
                appendLine("\nguest toybox: ${describe(toybox!!)}; linked $linked/${applets.size} commands")
                test("E", "guest shell + the firmware's own commands first on PATH",
                    listOf(patched!!.path, "-c", SCRIPT_E),
                    mapOf("LD_LIBRARY_PATH" to guestLibs, "PATH" to "${bin.path}:/system/bin"), cwd = dir)
            } else appendLine("\n[E] skipped: ${if (toybox == null) "firmware has no toybox" else "D could not be prepared"}")
        }
        dir.deleteRecursively()
        appendLine("\nPlease send this whole report to the AEmulator X developer.")
    }

    /** exit code (or a reason) and combined output, killed after 10 s. */
    private fun exec(cmd: List<String>, env: Map<String, String>, cwd: File? = null): Pair<String, String> = runCatching {
        val pb = ProcessBuilder(cmd).redirectErrorStream(true)
        if (cwd != null) pb.directory(cwd)
        pb.environment().putAll(env)
        val p = pb.start()
        val out = StringBuilder()
        val reader = Thread { runCatching { p.inputStream.bufferedReader().forEachLine { if (out.length < 8000) out.appendLine(it) } } }
            .apply { isDaemon = true; start() }
        val done = p.waitFor(10, TimeUnit.SECONDS)
        if (!done) p.destroyForcibly()
        reader.join(1000)
        val code = if (!done) "timeout (killed after 10 s)" else p.exitValue().let { c ->
            if (c > 128) "$c (killed by signal ${c - 128}${signalName(c - 128)})" else "$c"
        }
        code to out.toString()
    }.getOrElse { "could not start: ${it.message}" to "" }

    private fun signalName(s: Int) = when (s) { 4 -> " SIGILL"; 6 -> " SIGABRT"; 7 -> " SIGBUS"; 11 -> " SIGSEGV"; 31 -> " SIGSYS (blocked syscall)"; else -> "" }

    /** Follows links inside the guest tree; absolute targets are taken relative to [root]. */
    internal fun resolve(root: File, guestPath: String): File? {
        var f = File(root, guestPath.trimStart('/'))
        repeat(10) {
            val p = f.toPath()
            if (!Files.isSymbolicLink(p)) return f.takeIf { it.isFile }
            val t = Files.readSymbolicLink(p).toString()
            f = if (t.startsWith("/")) File(root, t.trimStart('/')) else File(f.parentFile, t)
        }
        return null
    }

    /**
     * Copy of [src] whose PT_INTERP says [interp] instead (written in place, NUL-padded, so it must not be longer than
     * the old path; the kernel reads the path through p_offset/p_filesz and stops at the first NUL).
     */
    internal fun patchInterp(src: File, dst: File, interp: String): File? = runCatching {
        val bytes = src.readBytes()
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (bytes.size < 64 || bytes[4].toInt() != 2) return null // 64-bit only
        val phoff = b.getLong(0x20).toInt()
        val phentsize = b.getShort(0x36).toInt() and 0xffff
        val phnum = b.getShort(0x38).toInt() and 0xffff
        for (i in 0 until phnum) {
            val ph = phoff + i * phentsize
            if (b.getInt(ph) != 3) continue
            val off = b.getLong(ph + 0x08).toInt()
            val size = b.getLong(ph + 0x20).toInt()
            val new = interp.toByteArray(Charsets.US_ASCII)
            if (new.size + 1 > size) return null
            for (j in 0 until size) bytes[off + j] = if (j < new.size) new[j] else 0
            dst.writeBytes(bytes)
            dst.setExecutable(true, false); dst.setReadable(true, false)
            return dst
        }
        null
    }.getOrNull()

    private fun copyExec(src: File, dst: File): File? = runCatching {
        src.inputStream().use { i -> dst.outputStream().use { o -> i.copyTo(o) } }
        dst.setExecutable(true, false); dst.setReadable(true, false)
        dst
    }.getOrNull()

    private fun describe(f: File): String = elf(f)?.let { e ->
        "${if (e.is64) "64" else "32"}-bit, ${if (e.machine == EM_AARCH64) "arm64" else if (e.machine == 40) "arm" else "machine ${e.machine}"}, " +
            (if (e.static) "static" else "interpreter ${e.interp}") + ", ${f.length()} bytes"
    } ?: "not an ELF program (${f.length()} bytes)"

    /** Just enough of the ELF header: class, machine and PT_INTERP. */
    internal fun elf(f: File): Elf? = runCatching {
        RandomAccessFile(f, "r").use { r ->
            val head = ByteArray(64); r.readFully(head)
            if (head[0] != 0x7f.toByte() || head[1] != 'E'.code.toByte() || head[2] != 'L'.code.toByte() || head[3] != 'F'.code.toByte()) return null
            val is64 = head[4].toInt() == 2
            val b = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN)
            val machine = b.getShort(0x12).toInt() and 0xffff
            val phoff = if (is64) b.getLong(0x20) else (b.getInt(0x1c).toLong() and 0xffffffffL)
            val phentsize = (if (is64) b.getShort(0x36) else b.getShort(0x2a)).toInt() and 0xffff
            val phnum = (if (is64) b.getShort(0x38) else b.getShort(0x2c)).toInt() and 0xffff
            var interp: String? = null
            for (i in 0 until phnum.coerceAtMost(64)) {
                val ph = ByteArray(phentsize.coerceIn(32, 64)); r.seek(phoff + i.toLong() * phentsize); r.readFully(ph)
                val pb = ByteBuffer.wrap(ph).order(ByteOrder.LITTLE_ENDIAN)
                if (pb.getInt(0) != 3) continue // PT_INTERP
                val off = if (is64) pb.getLong(0x08) else (pb.getInt(0x04).toLong() and 0xffffffffL)
                val size = (if (is64) pb.getLong(0x20) else (pb.getInt(0x10).toLong() and 0xffffffffL)).coerceIn(0, 256).toInt()
                val s = ByteArray(size); r.seek(off); r.readFully(s)
                interp = String(s, Charsets.US_ASCII).trimEnd('\u0000')
            }
            Elf(is64, machine, interp, interp == null)
        }
    }.getOrNull()
}
