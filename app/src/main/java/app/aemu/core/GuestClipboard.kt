/* Added for AEmulator X, 2026-10-19: read the emulated phone's clipboard. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The guest's clipboard, asked from its own clipboard service with `service call` (no app needed in the guest). The
 * reply is a Parcel printed as hex words; the clipped text is the string after the "text/plain" MIME type (Android
 * 3.0+), or the only string there is (2.x getClipboardText).
 */
object GuestClipboard {
    /** The shell command for this Android version. */
    fun command(api: Int): String = when {
        api >= 29 -> "service call clipboard 2 s16 com.android.shell i32 0"
        api >= 18 -> "service call clipboard 2 s16 com.android.shell"
        else -> "service call clipboard 2"
    }

    /** The bytes of a "Result: Parcel(...)" dump. */
    fun bytes(dump: String): ByteArray {
        val words = Regex("""0x[0-9a-fA-F]+:\s+((?:[0-9a-fA-F]{8}\s*)+)""").findAll(dump)
            .flatMap { it.groupValues[1].trim().split(Regex("\\s+")).asSequence() }.map { it.toLong(16).toInt() }.toList()
        val b = ByteBuffer.allocate(words.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        words.forEach { b.putInt(it) }
        return b.array()
    }

    /** Every UTF-16 string the parcel holds (int32 length, then the characters), in order. */
    fun strings(data: ByteArray): List<String> {
        val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val out = ArrayList<String>()
        var i = 0
        while (i + 4 <= data.size) {
            val len = b.getInt(i)
            // a parcel string is followed by a 16-bit NUL: without one these bytes are a number, not text
            if (len in 1..20000 && i + 6 + len * 2 <= data.size && data[i + 4 + len * 2].toInt() == 0 && data[i + 5 + len * 2].toInt() == 0) {
                val s = String(data, i + 4, len * 2, Charsets.UTF_16LE)
                // (a lone count can pass for "\n": whitespace-only strings are not clipboard text)
                if (s.any { !it.isWhitespace() } && s.all { it == '\n' || it == '\t' || !it.isISOControl() } && s.none { it == '�' }) {
                    out.add(s)
                    i += 4 + ((len * 2 + 2 + 3) / 4) * 4   // characters, the terminating NUL, padding to 4
                    continue
                }
            }
            i += 4
        }
        return out
    }

    /** The clipped text, or null when the clipboard is empty or the answer is not understood. */
    fun text(dump: String): String? {
        val s = strings(bytes(dump))
        if (s.isEmpty()) return null
        val mime = s.indexOfFirst { it.startsWith("text/") }
        return if (mime >= 0) s.drop(mime + 1).firstOrNull { !it.contains('/') || it.length > 40 } ?: s.drop(mime + 1).firstOrNull()
        else s.singleOrNull()
    }
}
