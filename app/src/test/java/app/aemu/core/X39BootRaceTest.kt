package app.aemu.core

import app.aemu.core.BootRace.Entry
import app.aemu.core.BootRace.Outcome
import org.junit.Assert.*
import org.junit.Test

class X39BootRaceTest {
    @Test fun outcomes() {
        assertEquals(Outcome.FIRST, BootRace.outcome(null, 40_000))
        assertEquals(Outcome.RECORD, BootRace.outcome(40_000, 35_000))
        assertEquals(Outcome.SLOWER, BootRace.outcome(40_000, 40_000))                 // a tie is not a record
        assertEquals(Outcome.SLOWER, BootRace.outcome(40_000, 52_000))
    }

    @Test fun fileRoundTripAndLimits() {
        val list = listOf(Entry("a", 30_000, 1), Entry("b", 45_500, 2))
        assertEquals(list, BootRace.parse(BootRace.format(list)))
        assertTrue(BootRace.parse("a 10 5\nb 99999999999 6\nbad line\nc x 7\n").isEmpty())   // not believable, or not numbers
        var l = emptyList<Entry>()
        repeat(BootRace.KEEP_PER_FIRMWARE + 10) { l = BootRace.added(l, Entry("a", 20_000L + it, it.toLong())) }
        l = BootRace.added(l, Entry("b", 50_000, 999))
        assertEquals(BootRace.KEEP_PER_FIRMWARE, l.count { it.id == "a" })
        assertEquals(10L, l.filter { it.id == "a" }.minOf { it.at })                      // the oldest went first
    }

    @Test fun leaderboard() {
        val e = listOf(Entry("kk", 31_000, 1), Entry("kk", 28_000, 2), Entry("kk", 33_000, 3), Entry("gb", 19_000, 4))
        val rows = BootRace.leaderboard(e, mapOf("old" to 25_000L, "kk" to 99_000L))
        assertEquals(listOf("gb", "old", "kk"), rows.map { it.id })                       // fastest first; history beats the old single time
        val kk = rows.last()
        assertEquals(28_000L, kk.best); assertEquals(33_000L, kk.last); assertEquals(3, kk.count)
        assertEquals("23.4 s", BootRace.seconds(23_400))
        assertEquals("1 min 05 s", BootRace.seconds(65_000))
    }
}
