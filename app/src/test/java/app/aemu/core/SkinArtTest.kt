/* Added for AEmulator X, 2026-10-17: built-in device skins. GPL-3.0; see LICENSE. */
package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class SkinArtTest {
    private fun aspect(s: SkinArt.Spec) = (s.screen.width * s.w) / (s.screen.height * s.h)

    @Test fun screensHaveTheirEraShapes() {
        assertEquals(9f / 16, aspect(SkinArt.PEBBLE), 0.02f)
        assertEquals(9f / 16, aspect(SkinArt.SLATE), 0.02f)
        assertEquals(3f / 2, aspect(SkinArt.SLIDER), 0.02f)
        assertEquals(4f / 3, aspect(SkinArt.TABLET), 0.02f)
    }

    @Test fun keysSitOnThePictureOffTheScreen() {
        for (s in SkinArt.ALL) {
            assertTrue(s.id, listOf(s.screen.l, s.screen.t, s.screen.r, s.screen.b).all { it in 0f..1f })
            for (k in s.keys) {
                val a = k.area
                assertTrue("${s.id} ${k.button}", listOf(a.l, a.t, a.r, a.b).all { it in 0f..1f })
                val overlaps = a.l < s.screen.r && a.r > s.screen.l && a.t < s.screen.b && a.b > s.screen.t
                assertFalse("${s.id} ${k.button} covers the screen", overlaps)
            }
            assertTrue(VmTheme.safeFileName(s.file))
        }
    }

    @Test fun builtInSkinThemesPointAtTheirPictures() {
        val skins = VmThemes.BUILT_IN.filter { it.isSkin }
        assertEquals(4, skins.size)
        assertTrue(skins.all { SkinArt.byFile(it.frameImage) != null })
        assertEquals("sunset", VmThemes.BUILT_IN[1].id) // the default fallback index is unchanged
    }
}
