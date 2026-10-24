/* Added for AEmulator X, 2026-10-22: Penalty shootout. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import kotlin.math.abs
import kotlin.random.Random

/**
 * A penalty shootout against the phone: five kicks each, taking turns (you shoot first), then sudden death. Shots
 * and dives are points in the goal mouth: x 0..1 from left post to right post, y 0..1 from the ground to the bar.
 * A shot outside that is a miss; a keeper saves what is close enough to where he dives.
 */
class Penalty(private val rnd: Random = Random.Default) {
    enum class Result { GOAL, SAVED, MISS }
    data class Kick(val byPlayer: Boolean, val result: Result)

    val kicks = ArrayList<Kick>()

    val playerGoals get() = kicks.count { it.byPlayer && it.result == Result.GOAL }
    val cpuGoals get() = kicks.count { !it.byPlayer && it.result == Result.GOAL }
    private val playerKicks get() = kicks.count { it.byPlayer }
    private val cpuKicks get() = kicks.count { !it.byPlayer }

    /** The player shoots on even kicks, keeps on odd ones. */
    val playerShoots: Boolean get() = kicks.size % 2 == 0

    fun record(result: Result) { if (!over) kicks.add(Kick(playerShoots, result)) }

    /** Decided: after 5 each one side can no longer catch up, or sudden death has a winner after an equal number. */
    val over: Boolean get() {
        val pk = playerKicks; val ck = cpuKicks
        if (pk <= ROUNDS && ck <= ROUNDS) {
            val pLeft = ROUNDS - pk; val cLeft = ROUNDS - ck
            return playerGoals > cpuGoals + cLeft || cpuGoals > playerGoals + pLeft
        }
        return pk == ck && playerGoals != cpuGoals
    }

    val playerWon: Boolean get() = over && playerGoals > cpuGoals

    // ---- the computer

    /** Where the computer shoots: mostly well placed, sometimes a bit wide or high (it can miss too). */
    fun cpuShot(): Pair<Float, Float> {
        val x = if (rnd.nextFloat() < 0.15f) 0.5f + (rnd.nextFloat() - 0.5f) * 0.3f else
            (if (rnd.nextBoolean()) 0.12f else 0.88f) + (rnd.nextFloat() - 0.5f) * 0.3f
        val y = 0.15f + rnd.nextFloat() * 0.8f
        return x to y
    }

    /** Where the computer's keeper dives (he guesses: the side is right about 45% of the time). */
    fun cpuDive(shotX: Float, shotY: Float): Pair<Float, Float> {
        val guessRight = rnd.nextFloat() < 0.45f
        val x = when {
            guessRight -> shotX + (rnd.nextFloat() - 0.5f) * 0.25f
            rnd.nextFloat() < 0.2f -> 0.5f                             // stays in the middle
            else -> if (shotX < 0.5f) 0.8f else 0.2f
        }
        return x.coerceIn(0f, 1f) to (0.2f + rnd.nextFloat() * 0.6f)
    }

    companion object {
        const val ROUNDS = 5
        /** how far (in goal widths / heights) from his dive point a keeper still reaches the ball */
        const val REACH_X = 0.2f
        const val REACH_Y = 0.42f

        fun resolve(shotX: Float, shotY: Float, diveX: Float, diveY: Float): Result = when {
            shotX < 0f || shotX > 1f || shotY < 0f || shotY > 1f -> Result.MISS
            abs(shotX - diveX) <= REACH_X && abs(shotY - diveY) <= REACH_Y -> Result.SAVED
            else -> Result.GOAL
        }

        /**
         * A swipe from the ball ([dx] right, [dy] up, as fractions of the screen width / height) to a point in the
         * goal: sideways follows dx, height follows the swipe's length; too hard flies over, too wide goes past.
         */
        fun aim(dx: Float, dy: Float): Pair<Float, Float> {
            val x = 0.5f + dx * 2.2f
            val y = (dy - 0.1f) * 3.4f
            return x to y
        }
    }
}
