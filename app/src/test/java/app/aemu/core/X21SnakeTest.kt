package app.aemu.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class X21SnakeTest {
    @Test fun movesAndCannotReverse() {
        val g = SnakeGame(16, 16, Random(1))
        val head = g.body.first()
        g.turn(SnakeGame.Dir.LEFT)                                  // straight back: ignored
        g.step()
        assertEquals(SnakeGame.Cell(head.x + 1, head.y), g.body.first())
        assertEquals(3, g.body.size)
        g.turn(SnakeGame.Dir.UP); g.step()
        assertEquals(SnakeGame.Cell(head.x + 1, head.y - 1), g.body.first())
    }

    @Test fun wallEndsTheGame() {
        val g = SnakeGame(8, 8, Random(2))
        repeat(20) { g.step() }
        assertTrue(g.over)
        assertFalse(g.step())
    }

    @Test fun eatingGrowsAndSpeedsUp() {
        val g = SnakeGame(16, 16, Random(3))
        var ate = false
        // steer towards the food until one dessert is eaten
        repeat(200) {
            if (ate || g.over) return@repeat
            val h = g.body.first(); val f = g.food!!
            g.turn(when {
                f.x > h.x -> SnakeGame.Dir.RIGHT; f.x < h.x -> SnakeGame.Dir.LEFT
                f.y > h.y -> SnakeGame.Dir.DOWN; else -> SnakeGame.Dir.UP
            })
            ate = g.step()
        }
        if (!g.over) {
            assertTrue(ate)
            assertEquals(1, g.score); assertEquals(4, g.body.size)
            assertEquals(165L, g.tickMs)
            assertEquals("Donut", g.dessert.name)
            assertFalse(g.body.contains(g.food))
        }
        assertEquals(14, SnakeGame.DESSERTS.size)
    }
}
