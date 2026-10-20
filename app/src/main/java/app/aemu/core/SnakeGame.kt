/* Added for AEmulator X, 2026-10-20: the hidden Snake game. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import kotlin.random.Random

/**
 * Snake on a [w] × [h] grid, eating Android desserts. Pure logic: the screen calls [turn] for input and [step] on
 * every tick ([tickMs] gets shorter as the snake grows). Hitting a wall or itself ends the game.
 */
class SnakeGame(val w: Int = 16, val h: Int = 16, private val rnd: Random = Random.Default) {
    data class Cell(val x: Int, val y: Int)
    enum class Dir(val dx: Int, val dy: Int) { UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0) }

    /** Head first. */
    val body = ArrayDeque<Cell>()
    var dir = Dir.RIGHT; private set
    private var next = Dir.RIGHT
    var food: Cell? = null; private set
    var score = 0; private set
    var over = false; private set

    init {
        val y = h / 2
        for (i in 0 until 3) body.addLast(Cell(w / 4 + 2 - i, y))
        food = place()
    }

    /** Asks for a new direction; turning straight back is ignored. Applied on the next [step]. */
    fun turn(d: Dir) {
        if (d.dx != -dir.dx || d.dy != -dir.dy) next = d
    }

    /** Moves one cell. Returns true when a dessert was eaten. */
    fun step(): Boolean {
        if (over) return false
        dir = next
        val head = body.first()
        val n = Cell(head.x + dir.dx, head.y + dir.dy)
        val eats = n == food
        if (n.x !in 0 until w || n.y !in 0 until h) { over = true; return false }
        // the tail moves away this tick unless the snake grows
        val hits = if (eats) body.contains(n) else body.subList(0, body.size - 1).contains(n)
        if (hits) { over = true; return false }
        body.addFirst(n)
        if (eats) {
            score++
            food = place()
            if (food == null) over = true          // the whole board is snake: that's a win
        } else body.removeLast()
        return eats
    }

    /** Milliseconds between steps: 170 at the start, faster with every dessert, never under 70. */
    val tickMs: Long get() = (170L - score * 5L).coerceAtLeast(70L)

    /** The dessert being shown now (one per score, round and round). */
    val dessert: Dessert get() = DESSERTS[score % DESSERTS.size]

    private fun place(): Cell? {
        val free = (0 until w * h).map { Cell(it % w, it / w) }.filterNot { body.contains(it) }
        return if (free.isEmpty()) null else free[rnd.nextInt(free.size)]
    }

    class Dessert(val emoji: String, val name: String, val version: String)

    companion object {
        val DESSERTS = listOf(
            Dessert("🧁", "Cupcake", "1.5"), Dessert("🍩", "Donut", "1.6"), Dessert("🥐", "Eclair", "2.0"),
            Dessert("🍦", "Froyo", "2.2"), Dessert("🍪", "Gingerbread", "2.3"), Dessert("🍯", "Honeycomb", "3.x"),
            Dessert("🍨", "Ice Cream Sandwich", "4.0"), Dessert("🫘", "Jelly Bean", "4.1"), Dessert("🍫", "KitKat", "4.4"),
            Dessert("🍭", "Lollipop", "5.x"), Dessert("🍡", "Marshmallow", "6.0"), Dessert("🥜", "Nougat", "7.x"),
            Dessert("🍪", "Oreo", "8.x"), Dessert("🥧", "Pie", "9"),
        )
    }
}
