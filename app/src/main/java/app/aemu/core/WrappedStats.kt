/* Added for AEmulator X, 2026-10-22: AEmulator Wrapped. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context

/** Everything AEmulator Wrapped tells you, worked out once from what the app already keeps. */
data class WrappedStats(
    val firmwares: Int,
    val totalPlayMs: Long,
    /** most played firmware: name, API level, time */
    val top: Triple<String, Int, Long>?,
    /** the Android version family played most (as a representative API level), and its share of play time 0..1 */
    val favouriteApi: Int?,
    val favouriteShare: Float,
    val screenshots: Int,
    val recordings: Int,
    val boots: Int,
    val bestPlayStreak: Int,
    val bestChallengeStreak: Int,
    val badges: Int,
    val badgesTotal: Int,
) {
    companion object {
        /** [played]: name, API level and play time of every firmware. */
        fun build(
            played: List<Triple<String, Int, Long>>, counts: Map<String, Int>, bestPlayStreak: Int, bestChallengeStreak: Int,
            badges: Int, badgesTotal: Int,
        ): WrappedStats {
            val total = played.sumOf { it.third }
            val top = played.filter { it.third > 0 }.maxByOrNull { it.third }
            val byFamily = played.filter { it.third > 0 }.groupBy { DailyPick.factGroup(it.second) }.mapValues { e -> e.value.sumOf { it.third } }
            val fav = byFamily.maxByOrNull { it.value }
            return WrappedStats(
                firmwares = played.size, totalPlayMs = total, top = top,
                favouriteApi = fav?.key?.let { DailyChallenge.GROUP_API[it] },
                favouriteShare = if (fav == null || total <= 0) 0f else fav.value.toFloat() / total,
                screenshots = counts[Achievements.SCREENSHOT] ?: 0, recordings = counts[Achievements.RECORD] ?: 0,
                boots = counts[DailyChallenge.BOOT] ?: 0,
                bestPlayStreak = bestPlayStreak, bestChallengeStreak = bestChallengeStreak, badges = badges, badgesTotal = badgesTotal,
            )
        }

        /** Reads it all now. Blocking (library, play time and counter files). */
        fun read(ctx: Context): WrappedStats {
            val imgs = runCatching { ImageStore.list(ctx) }.getOrDefault(emptyList())
            val played = imgs.map { Triple(it.name, it.api, PlayStats.read(ctx, it.id).totalMs) }
            val facts = Achievements.facts(ctx)
            // every boot ever (the library keeps a count per firmware), not only since daily challenges came
            val counts = facts.counts + (DailyChallenge.BOOT to imgs.sumOf { it.bootCount })
            return build(played, counts, PlayDays.bestStreak(PlayDays.read(ctx)), DailyChallenge.bestStreak(DailyChallenge.doneDays(ctx)),
                Achievements.ALL.count { it.unlocked(facts) }, Achievements.ALL.size)
        }

        /** Hours and the minutes after them. */
        fun hours(ms: Long): Pair<Long, Long> = (ms / 3_600_000L) to (ms / 60_000L % 60)
    }
}
