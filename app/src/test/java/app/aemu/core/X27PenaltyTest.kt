package app.aemu.core

import app.aemu.core.Penalty.Result.GOAL
import app.aemu.core.Penalty.Result.MISS
import app.aemu.core.Penalty.Result.SAVED
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class X27PenaltyTest {
    @Test fun shotsAndSaves() {
        assertEquals(GOAL, Penalty.resolve(0.1f, 0.8f, 0.9f, 0.3f))               // top corner, keeper went the other way
        assertEquals(SAVED, Penalty.resolve(0.15f, 0.3f, 0.2f, 0.4f))             // right where he dived
        assertEquals(MISS, Penalty.resolve(1.1f, 0.5f, 0.5f, 0.5f))               // past the post
        assertEquals(MISS, Penalty.resolve(0.5f, 1.2f, 0.5f, 0.5f))               // over the bar
        val (x, y) = Penalty.aim(0f, 0.25f)
        assertEquals(0.5f, x, 1e-4f); assertTrue(y in 0f..1f)                     // a straight swipe: on target
        assertTrue(Penalty.aim(0f, 0.6f).second > 1f)                              // far too hard: over
    }

    @Test fun takingTurnsAndAnEarlyWin() {
        val p = Penalty(Random(1))
        assertTrue(p.playerShoots)
        // you score 3, the phone misses 3: it can no longer catch up
        repeat(3) { p.record(GOAL); assertFalse(p.playerShoots); p.record(SAVED) }
        assertTrue(p.over); assertTrue(p.playerWon)
        p.record(GOAL)
        assertEquals(6, p.kicks.size)                                               // nothing counts after the end
    }

    @Test fun suddenDeath() {
        val p = Penalty()
        repeat(5) { p.record(GOAL); p.record(GOAL) }
        assertFalse(p.over)                                                         // 5–5: go on
        p.record(MISS); assertFalse(p.over)                                         // the phone still has to kick
        p.record(GOAL)
        assertTrue(p.over); assertFalse(p.playerWon)
        assertEquals(5, p.playerGoals); assertEquals(6, p.cpuGoals)
    }

    @Test fun theComputerIsFairButBeatable() {
        val p = Penalty(Random(5))
        var goals = 0; var saves = 0
        repeat(2000) {
            val (sx, sy) = p.cpuShot()
            if (Penalty.resolve(sx, sy, 0.5f, 0.3f) == GOAL) goals++                 // against a keeper who stays put
            val (dx, dy) = p.cpuDive(0.15f, 0.5f)
            if (Penalty.resolve(0.15f, 0.5f, dx, dy) == SAVED) saves++               // its keeper against a good shot
        }
        assertTrue("phone scores $goals/2000", goals in 1000..1900)
        assertTrue("phone saves $saves/2000", saves in 300..1100)
    }
}
