package app.aemu.core

import app.aemu.core.FirmwareCollections.Collection
import org.junit.Assert.*
import org.junit.Test

class X27FeaturesTest {
    @Test fun collectionsRoundTrip() {
        val list = listOf(Collection("Games", setOf("a", "b")), Collection("Old ones", emptySet()))
        assertEquals(list, FirmwareCollections.parse(FirmwareCollections.format(list)))
        assertEquals(listOf(Collection("Games", setOf("a"))), FirmwareCollections.parse("Games\ta\ngames\tb\n\t\n"))   // one per name
        assertEquals("Tab name", FirmwareCollections.cleanName("  Tab\tname \n"))
    }

    @Test fun collectionsEditing() {
        var list = FirmwareCollections.upsert(emptyList(), null, Collection("Games", setOf("a")))
        list = FirmwareCollections.upsert(list, null, Collection("Fav", setOf("b")))
        list = FirmwareCollections.upsert(list, "Games", Collection("Best games", setOf("a", "c")))       // rename keeps the place
        assertEquals(listOf("Best games", "Fav"), list.map { it.name })
        assertEquals(setOf("a", "c"), list[0].ids)
        list = FirmwareCollections.upsert(list, "Fav", Collection("best GAMES", setOf("z")))              // renamed onto another: one left
        assertEquals(1, list.size)
    }

    @Test fun wrapped() {
        val played = listOf(Triple("KitKat", 19, 3_600_000L * 5), Triple("Gingerbread", 10, 3_600_000L), Triple("Jelly", 17, 3_600_000L * 4), Triple("New", 28, 0L))
        val w = WrappedStats.build(played, mapOf("screenshot" to 7, "record" to 2, "boot" to 30), 4, 2, 3, 12)
        assertEquals(4, w.firmwares)
        assertEquals(3_600_000L * 10, w.totalPlayMs)
        assertEquals("KitKat", w.top?.first)
        assertEquals(19, w.favouriteApi); assertEquals(0.5f, w.favouriteShare, 1e-4f)
        assertEquals(7, w.screenshots); assertEquals(30, w.boots)
        assertEquals(10L to 0L, WrappedStats.hours(w.totalPlayMs))
        val empty = WrappedStats.build(emptyList(), emptyMap(), 0, 0, 0, 12)
        assertNull(empty.top); assertNull(empty.favouriteApi); assertEquals(0f, empty.favouriteShare)
    }
}
