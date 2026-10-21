/* Added for AEmulator X, 2026-10-21: firmware notes. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File

/** Your own words about a firmware ("best for games", "Wi-Fi is slow"…), kept in its folder and shown on its tile. */
object FirmwareNotes {
    const val MAX = 300
    /** bumped on every change, so tiles read their note again */
    var rev by mutableIntStateOf(0); private set

    private fun file(ctx: Context, id: String) = File(VmPaths(ctx, id).dir, "x-notes.txt")

    fun read(ctx: Context, id: String): String = runCatching { file(ctx, id).takeIf { it.isFile }?.readText() }.getOrNull().orEmpty()

    fun write(ctx: Context, id: String, text: String) {
        val t = clean(text)
        runCatching { if (t.isEmpty()) file(ctx, id).delete() else file(ctx, id).writeText(t) }
        rev++
    }

    /** Trimmed, at most [MAX] characters, no runs of blank lines. */
    fun clean(text: String): String = text.trim().replace(Regex("\\n{3,}"), "\n\n").take(MAX)

    /** The note squeezed onto one line for a tile. */
    fun oneLine(text: String): String = text.replace(Regex("\\s*\\n\\s*"), " · ").trim()
}
