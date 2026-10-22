/* Added for AEmulator X, 2026-10-22: firmware collections. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * Your own folders of firmwares ("Games", "Old ones"…); a firmware can be in several. Kept in files/x-collections.txt,
 * one collection per line: its name, a tab, the firmware ids separated by commas. Order is the order they were made.
 */
object FirmwareCollections {
    data class Collection(val name: String, val ids: Set<String>)

    const val MAX_NAME = 24
    /** bumped on every change, so the library reads them again */
    var rev by mutableIntStateOf(0); private set

    private fun file(ctx: Context) = File(ctx.filesDir, "x-collections.txt")

    fun cleanName(n: String) = n.replace('\t', ' ').replace('\n', ' ').trim().take(MAX_NAME)

    internal fun parse(text: String): List<Collection> = text.lines().mapNotNull { line ->
        val tab = line.indexOf('\t')
        val name = cleanName(if (tab < 0) line else line.substring(0, tab))
        if (name.isEmpty()) null
        else Collection(name, if (tab < 0) emptySet() else line.substring(tab + 1).split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet())
    }.distinctBy { it.name.lowercase() }

    internal fun format(list: List<Collection>): String = list.joinToString("") { "${it.name}\t${it.ids.joinToString(",")}\n" }

    fun all(ctx: Context): List<Collection> = runCatching { parse(file(ctx).readText()) }.getOrDefault(emptyList())

    private fun write(ctx: Context, list: List<Collection>) {
        runCatching { file(ctx).writeText(format(list)) }
        rev++
    }

    /** Adds or replaces (by [old] name, or by the new name) a collection. */
    internal fun upsert(list: List<Collection>, old: String?, c: Collection): List<Collection> {
        val key = (old ?: c.name).lowercase()
        val i = list.indexOfFirst { it.name.lowercase() == key }
        val rest = list.filterIndexed { j, it -> j == i || it.name.lowercase() != c.name.lowercase() }
        return if (i < 0) rest + c else rest.map { if (it.name.lowercase() == key) c else it }
    }

    fun save(ctx: Context, old: String?, name: String, ids: Set<String>) {
        val n = cleanName(name)
        if (n.isEmpty()) return
        write(ctx, upsert(all(ctx), old, Collection(n, ids)))
    }

    fun delete(ctx: Context, name: String) = write(ctx, all(ctx).filter { it.name != name })
}
