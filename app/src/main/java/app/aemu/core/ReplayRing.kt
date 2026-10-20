/* Added for AEmulator X, 2026-10-20: Instant replay. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

/**
 * The last [capacity] items, oldest dropped first: Instant replay keeps the last [SECONDS] seconds of the guest
 * screen as small JPEG frames ([FPS] a second), so it costs a few MB, not hundreds.
 */
class ReplayRing<T>(val capacity: Int = FPS * SECONDS) {
    private val items = ArrayDeque<T>()

    val size: Int @Synchronized get() = items.size

    @Synchronized fun add(item: T) {
        items.addLast(item)
        while (items.size > capacity) items.removeFirst()
    }

    /** Oldest first, a copy (recording goes on while it is watched). */
    @Synchronized fun snapshot(): List<T> = items.toList()

    @Synchronized fun clear() = items.clear()

    companion object {
        const val FPS = 5
        const val SECONDS = 15
        /** frames are shrunk to this width */
        const val WIDTH = 480
    }
}
