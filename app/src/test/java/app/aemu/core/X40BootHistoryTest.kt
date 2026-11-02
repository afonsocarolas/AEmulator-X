package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class X40BootHistoryTest {
    @Test fun desc() {
        val d = BootHistory.parseDesc("480 800 30\np 1 0 part0\np 0 10 part1\n")!!
        assertEquals(480, d.width); assertEquals(800, d.height); assertEquals(30, d.fps)
        assertEquals(listOf("part0", "part1"), d.parts.map { it.dir })
        assertTrue(d.parts[0].playOnce); assertFalse(d.parts[1].playOnce)
        assertEquals(10, d.parts[1].pause)
        // newer files: comments, "c" parts and colours after the folder
        val n = BootHistory.parseDesc("# made by a maker\n1080 1920 60\nc 1 0 part0 #000000\nc 0 0 part1\n")!!
        assertEquals(2, n.parts.size); assertEquals("part0", n.parts[0].dir)
        assertNull(BootHistory.parseDesc("hello"))
        assertNull(BootHistory.parseDesc("480 800 30\n"))                                  // no parts
        assertEquals(60, BootHistory.parseDesc("1 1 500\np 1 0 a")!!.fps)                   // fps kept sane
    }

    @Test fun framesInOrder() {
        val names = listOf("desc.txt", "part1/0002.png", "part0/0001.png", "part0/0000.png", "part0/", "part0/notes.txt", "part10/0000.png")
        assertEquals(listOf("part0/0000.png", "part0/0001.png"), BootHistory.frames(names, "part0"))
        assertEquals(listOf("part1/0002.png"), BootHistory.frames(names, "part1"))
    }

    @Test fun eras() {
        assertEquals(0, BootHistory.era(4)); assertEquals(0, BootHistory.era(10)); assertEquals(1, BootHistory.era(12))
        assertEquals(2, BootHistory.era(16)); assertEquals(3, BootHistory.era(19)); assertEquals(4, BootHistory.era(23))
        assertEquals(5, BootHistory.era(28)); assertEquals(6, BootHistory.era(34))
    }
}
