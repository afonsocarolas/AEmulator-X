/* Added for AEmulator X, 2026-10-16: library search, sort, favourites and play time. GPL-3.0; see LICENSE. */
package app.aemu.ui

import app.aemu.core.Engine
import app.aemu.core.GuestImage
import app.aemu.core.PlayStats
import app.aemu.core.VmSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySortTest {
    private fun img(id: String, name: String, api: Int, created: Long, skin: String = "AOSP", model: String = "Phone") = GuestImage(
        id = id, name = name, release = "$api", api = api, brand = "Brand", model = model, skin = skin, engine = Engine.KK,
        bootclasspath = "", exports = emptyMap(), dirs = emptyList(), services = emptyList(), sdcardPath = "/sdcard",
        settings = VmSettings(), createdAt = created,
    )

    private val g1 = img("g1", "T-Mobile G1", 4, created = 300, skin = "HTC")
    private val s3 = img("s3", "galaxy S3", 16, created = 100, skin = "TouchWiz", model = "GT-I9300")
    private val n7 = img("n7", "Nexus 7", 23, created = 200)
    private val all = listOf(n7, g1, s3)

    @Test fun sortsByNameVersionAddedAndRecent() {
        assertEquals(listOf("s3", "n7", "g1"), LibrarySort.apply(all, "", LibrarySort.Sort.NAME, emptySet(), emptyMap()).map { it.id })
        assertEquals(listOf("g1", "s3", "n7"), LibrarySort.apply(all, "", LibrarySort.Sort.VERSION, emptySet(), emptyMap()).map { it.id })
        assertEquals(listOf("g1", "n7", "s3"), LibrarySort.apply(all, "", LibrarySort.Sort.ADDED, emptySet(), emptyMap()).map { it.id })
        val stats = mapOf("n7" to PlayStats.Stats(1000, 50), "s3" to PlayStats.Stats(1000, 90))
        assertEquals(listOf("s3", "n7", "g1"), LibrarySort.apply(all, "", LibrarySort.Sort.RECENT, emptySet(), stats).map { it.id })
    }

    @Test fun favouritesComeFirstAndSearchFilters() {
        assertEquals(listOf("n7", "s3", "g1"), LibrarySort.apply(all, "", LibrarySort.Sort.NAME, setOf("n7"), emptyMap()).map { it.id })
        assertEquals(listOf("s3"), LibrarySort.apply(all, "touchwiz", LibrarySort.Sort.NAME, emptySet(), emptyMap()).map { it.id })
        assertEquals(listOf("s3"), LibrarySort.apply(all, " i9300 ", LibrarySort.Sort.NAME, emptySet(), emptyMap()).map { it.id })
        assertEquals(listOf("g1"), LibrarySort.apply(all, "API 4)", LibrarySort.Sort.NAME, emptySet(), emptyMap()).map { it.id })
        assertEquals(LibrarySort.Sort.NAME, LibrarySort.Sort.of("nonsense"))
    }

    @Test fun playStatsTextRoundTripAndDurations() {
        val s = PlayStats.Stats(7_500_000, 1_760_000_000_000)
        assertEquals(s, PlayStats.parse(PlayStats.format(s)))
        assertEquals(PlayStats.Stats(), PlayStats.parse("garbage"))
        assertEquals("45 s", PlayStats.duration(45_000))
        assertEquals("12 min", PlayStats.duration(12 * 60_000L))
        assertEquals("2 h 05 min", PlayStats.duration(125 * 60_000L))
    }
}
