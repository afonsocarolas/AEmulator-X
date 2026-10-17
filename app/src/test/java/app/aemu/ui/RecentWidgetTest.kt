/* Added for AEmulator X, 2026-10-17: quick-start widget. GPL-3.0; see LICENSE. */
package app.aemu.ui

import app.aemu.core.Engine
import app.aemu.core.GuestImage
import app.aemu.core.VmSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentWidgetTest {
    private fun img(id: String, name: String) = GuestImage(id = id, name = name, release = "4.4", api = 19, brand = "", model = "",
        skin = "", engine = Engine.KK, bootclasspath = "", exports = emptyMap(), dirs = emptyList(), services = emptyList(),
        sdcardPath = "/sdcard", settings = VmSettings(), createdAt = 0)

    @Test fun favouritesThenRecentThenNameAtMostFour() {
        val all = listOf(img("a", "Alpha"), img("b", "beta"), img("c", "Gamma"), img("d", "Delta"), img("e", "Epsilon"))
        val picked = RecentWidget.pick(all, favorites = setOf("e"), lastPlayed = mapOf("c" to 300L, "a" to 100L))
        assertEquals(listOf("e", "c", "a", "b"), picked.map { it.id })
    }
}
