/* Added for AEmulator X, 2026-10-23: firmware slideshow. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import kotlin.random.Random

/** Order of the slideshow: shuffled, but never two pictures of the same firmware in a row when it can be avoided. */
object SlideOrder {
    const val SECONDS = 8
    const val FADE_MS = 1200
    /** wallpapers taken from each firmware, so one with many does not fill the show */
    const val WALLPAPERS_PER_FIRMWARE = 3

    fun <T> interleave(items: List<T>, key: (T) -> String, rnd: Random = Random.Default): List<T> {
        val groups = items.groupBy(key).mapValues { it.value.shuffled(rnd).toMutableList() }.toMutableMap()
        val out = ArrayList<T>(items.size)
        var last: String? = null
        while (groups.isNotEmpty()) {
            // the fullest groups first (so the long ones do not end up alone at the end), any other than the last one shown
            val choices = groups.filterKeys { it != last }.ifEmpty { groups }
            val most = choices.maxOf { it.value.size }
            val k = choices.filterValues { it.size == most }.keys.random(rnd)
            out += groups.getValue(k).removeAt(0)
            if (groups.getValue(k).isEmpty()) groups.remove(k)
            last = k
        }
        return out
    }

    /** Ken Burns: how far into its slow zoom a slide is at [t] seconds: scale 1 → 1.12, a gentle drift to one side. */
    fun zoom(t: Float): Float = 1f + 0.12f * (t / SECONDS).coerceIn(0f, 1.2f)
}
