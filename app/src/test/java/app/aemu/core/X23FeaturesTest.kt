package app.aemu.core

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class X23FeaturesTest {
    @Test fun todayCountersResetEachDay() {
        var t = DailyChallenge.bumped("", 100, "screenshot")
        t = DailyChallenge.bumped(t, 100, "screenshot")
        assertEquals(mapOf("screenshot" to 2), DailyChallenge.parseToday(t, 100))
        assertTrue(DailyChallenge.parseToday(t, 101).isEmpty())                 // tomorrow starts at zero
        assertEquals(mapOf("boot" to 1), DailyChallenge.parseToday(DailyChallenge.bumped(t, 101, "boot"), 101))
    }

    @Test fun pickAndProgress() {
        assertNull(DailyChallenge.pick(5, emptyList()))
        val apis = listOf(19, 10)
        val seen = (0L until 60L).mapNotNull { DailyChallenge.pick(20000 + it, apis) }
        assertEquals(DailyChallenge.pick(20003, apis), DailyChallenge.pick(20003, apis))       // same all day
        assertTrue(seen.map { it.id }.toSet().size > 5)                                        // and varied
        assertTrue(seen.filter { it.group >= 0 }.all { it.group == 9 || it.group == 5 })        // only eras you have
        val shots = DailyChallenge.candidates(apis).first { it.id == "shots3" }
        assertEquals(2, DailyChallenge.progress(shots, mapOf("screenshot" to 2), 0))
        assertEquals(3, DailyChallenge.progress(shots, mapOf("screenshot" to 9), 0))
        val play = DailyChallenge.candidates(apis).first { it.id == "play15" }
        assertEquals(12, DailyChallenge.progress(play, emptyMap(), 12 * 60_000L + 5))
        assertEquals("boot_g9", DailyChallenge.candidates(listOf(19)).last().key)
    }

    @Test fun streaks() {
        val done = setOf(10L, 11L, 12L, 20L, 21L)
        assertEquals(2, DailyChallenge.streak(done, 21))
        assertEquals(2, DailyChallenge.streak(done, 22))                            // today still open
        assertEquals(0, DailyChallenge.streak(done, 23))
        assertEquals(3, DailyChallenge.bestStreak(done))
        assertEquals(0, DailyChallenge.bestStreak(emptySet()))
    }

    @Test fun birthdays() {
        val kk = AndroidBirthdays.on(LocalDate.of(2026, 10, 31)).single()
        assertEquals("KitKat", kk.name); assertEquals(13, AndroidBirthdays.age(kk, LocalDate.of(2026, 10, 31)))
        assertTrue(AndroidBirthdays.on(LocalDate.of(2026, 10, 21)).isEmpty())
        val (next, days) = AndroidBirthdays.next(LocalDate.of(2026, 10, 21))
        assertEquals("Eclair", next.name); assertEquals(5L, days)                      // Oct 26, before KitKat
        assertEquals("Honeycomb", AndroidBirthdays.next(LocalDate.of(2026, 12, 31)).first.name)   // across new year
        assertEquals("Cupcake", AndroidBirthdays.next(LocalDate.of(2027, 3, 1)).first.name)
        assertEquals(2, AndroidBirthdays.on(LocalDate.of(2026, 10, 4)).size)        // Android 12 and 14 share a day
    }

    @Test fun soundsAreShortAndNotTooLoud() {
        for (s in UiSounds.Sfx.values()) {
            val pcm = UiSounds.render(s)
            assertTrue(s.name, pcm.isNotEmpty() && pcm.size < UiSounds.RATE * 5 / 2)    // under 2.5 seconds
            val peak = pcm.maxOf { kotlin.math.abs(it.toInt()) }
            assertTrue(s.name, peak in 1000..(0.61 * 32767).toInt())
        }
    }
}
