/* Added for AEmulator X, 2026-10-20: Firmware of the day. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

/** Which firmware the library suggests today, and which fun fact goes with it. Same answer all day long. */
object DailyPick {
    /** Days since 1970 in local time: the pick changes at midnight. */
    fun today(nowMs: Long = System.currentTimeMillis(), tz: java.util.TimeZone = java.util.TimeZone.getDefault()): Long =
        Math.floorDiv(nowMs + tz.getOffset(nowMs), 86_400_000L)

    /** Index of today's pick among [count] firmwares (sorted by id by the caller), or -1 when there are none. */
    fun index(count: Int, day: Long): Int {
        if (count <= 0) return -1
        // mix the day so neighbours in the list are not picked on neighbouring days
        var x = day * 0x9E3779B97F4A7C15uL.toLong()
        x = x xor (x ushr 31)
        return Math.floorMod(x, count.toLong()).toInt()
    }

    /** Fun fact group for an API level: one fact per Android version family (see x_fact_* strings). */
    fun factGroup(api: Int): Int = when {
        api <= 2 -> 0      // 1.0 / 1.1
        api == 3 -> 1      // Cupcake
        api == 4 -> 2      // Donut
        api <= 7 -> 3      // Eclair
        api == 8 -> 4      // Froyo
        api <= 10 -> 5     // Gingerbread
        api <= 13 -> 6     // Honeycomb
        api <= 15 -> 7     // Ice Cream Sandwich
        api <= 18 -> 8     // Jelly Bean
        api <= 20 -> 9     // KitKat
        api <= 22 -> 10    // Lollipop
        api == 23 -> 11    // Marshmallow
        api <= 25 -> 12    // Nougat
        api <= 27 -> 13    // Oreo
        api == 28 -> 14    // Pie
        else -> 15         // 10 and newer
    }
    const val GROUPS = 16
}
