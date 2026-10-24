package app.aemu.core

import app.aemu.core.LiveScenes.Scene
import org.junit.Assert.*
import org.junit.Test

class X35LiveScenesTest {
    @Test fun scenesByName() {
        assertEquals(Scene.MATRIX, Scene.of("matrix"))
        assertEquals(Scene.OFF, Scene.of(null)); assertEquals(Scene.OFF, Scene.of("nope"))
        assertEquals(Scene.entries.size, Scene.entries.map { it.id }.toSet().size)
    }

    @Test fun everythingLoops() {
        val s = LiveScenes.stars(50)
        for (t in listOf(0f, 1f, 37.5f, 10_000f)) {
            assertTrue(s.all { LiveScenes.starDepth(it, t) in 0.03f..1f })                     // never at the camera, never behind it
            assertTrue((0 until 16).all { LiveScenes.gridLine(it, 16, t) in 0f..1f })
            assertTrue(LiveScenes.columns(10).all { LiveScenes.columnHead(it, t) in -0.2f..1.4f })
            assertTrue(LiveScenes.drops(20).all { LiveScenes.dropY(it, t) in -0.1f..1.1f })
            assertTrue(LiveScenes.flies(10).all { LiveScenes.glow(it, t) in 0f..1f })
        }
        assertEquals(LiveScenes.stars(5).map { it.x }, LiveScenes.stars(5).map { it.x })   // same scene every time
    }

    @Test fun glyphsAreFromTheSet() {
        for (c in 0 until 30) for (r in 0 until 40) assertTrue(LiveScenes.GLYPHS.contains(LiveScenes.glyph(c, r, c * 0.7f + r)))
    }
}
