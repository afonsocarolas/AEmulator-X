/* Added for AEmulator X, 2026-10-16: library search, sort and favourites. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import app.aemu.core.GuestImage
import app.aemu.core.PlayStats
import java.util.Locale

/** Ordering and filtering of the firmware library: favourites always first, then the chosen [Sort]. */
object LibrarySort {
    enum class Sort(val id: String) { NAME("name"), VERSION("version"), RECENT("recent"), ADDED("added");
        companion object { fun of(id: String?) = entries.firstOrNull { it.id == id } ?: NAME } }

    /** Does [img] match the search [query] (name, version, skin, brand or model; case-insensitive)? */
    fun matches(img: GuestImage, query: String): Boolean {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return true
        return listOf(img.name, img.displayVersion, img.release, img.skin, img.brand, img.model)
            .any { it.lowercase(Locale.ROOT).contains(q) }
    }

    fun apply(images: List<GuestImage>, query: String, sort: Sort, favorites: Set<String>, stats: Map<String, PlayStats.Stats>): List<GuestImage> {
        val byName = compareBy<GuestImage, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
        val order: Comparator<GuestImage> = when (sort) {
            Sort.NAME -> byName
            Sort.VERSION -> compareBy<GuestImage> { it.api }.then(byName)
            Sort.RECENT -> compareByDescending<GuestImage> { stats[it.id]?.lastPlayedAt ?: 0L }.then(byName)
            Sort.ADDED -> compareByDescending<GuestImage> { it.createdAt }.then(byName)
        }
        return images.filter { matches(it, query) }.sortedWith(compareByDescending<GuestImage> { it.id in favorites }.then(order))
    }
}
