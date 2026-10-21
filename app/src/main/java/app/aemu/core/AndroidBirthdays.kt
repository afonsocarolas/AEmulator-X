/* Added for AEmulator X, 2026-10-21: Android birthdays. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit

/** Release days of Android versions: on the day, the library throws a party. */
object AndroidBirthdays {
    /** [group] is the [DailyPick.factGroup] of the version (-1 for versions after Android 10, which share group 15). */
    data class Birthday(val name: String, val emoji: String, val born: LocalDate, val group: Int)

    val ALL = listOf(
        Birthday("Android 1.0", "🤖", LocalDate.of(2008, 9, 23), 0),
        Birthday("Cupcake", "🧁", LocalDate.of(2009, 4, 27), 1),
        Birthday("Donut", "🍩", LocalDate.of(2009, 9, 15), 2),
        Birthday("Eclair", "🥐", LocalDate.of(2009, 10, 26), 3),
        Birthday("Froyo", "🍦", LocalDate.of(2010, 5, 20), 4),
        Birthday("Gingerbread", "🍪", LocalDate.of(2010, 12, 6), 5),
        Birthday("Honeycomb", "🍯", LocalDate.of(2011, 2, 22), 6),
        Birthday("Ice Cream Sandwich", "🍨", LocalDate.of(2011, 10, 18), 7),
        Birthday("Jelly Bean", "🫘", LocalDate.of(2012, 7, 9), 8),
        Birthday("KitKat", "🍫", LocalDate.of(2013, 10, 31), 9),
        Birthday("Lollipop", "🍭", LocalDate.of(2014, 11, 12), 10),
        Birthday("Marshmallow", "🍡", LocalDate.of(2015, 10, 5), 11),
        Birthday("Nougat", "🥜", LocalDate.of(2016, 8, 22), 12),
        Birthday("Oreo", "🍪", LocalDate.of(2017, 8, 21), 13),
        Birthday("Pie", "🥧", LocalDate.of(2018, 8, 6), 14),
        Birthday("Android 10", "🔟", LocalDate.of(2019, 9, 3), 15),
        Birthday("Android 11", "🎂", LocalDate.of(2020, 9, 8), -1),
        Birthday("Android 12", "🎂", LocalDate.of(2021, 10, 4), -1),
        Birthday("Android 13", "🎂", LocalDate.of(2022, 8, 15), -1),
        Birthday("Android 14", "🎂", LocalDate.of(2023, 10, 4), -1),
        Birthday("Android 15", "🎂", LocalDate.of(2024, 10, 15), -1),
        Birthday("Android 16", "🎂", LocalDate.of(2025, 6, 10), -1),
    )

    /** Versions born on this day of the year. */
    fun on(date: LocalDate): List<Birthday> = ALL.filter { MonthDay.from(it.born) == MonthDay.from(date) && it.born.year < date.year }

    fun age(b: Birthday, date: LocalDate): Int = date.year - b.born.year

    /** The next birthday after [date] (not today's): the version and the days until it. */
    fun next(date: LocalDate): Pair<Birthday, Long> = ALL.map { b ->
        var d = b.born.withYear(date.year)
        if (!d.isAfter(date)) d = b.born.withYear(date.year + 1)
        b to ChronoUnit.DAYS.between(date, d)
    }.minWith(compareBy({ it.second }, { it.first.born }))
}
