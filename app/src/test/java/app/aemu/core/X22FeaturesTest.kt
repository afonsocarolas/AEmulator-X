package app.aemu.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class X22FeaturesTest {
    @Test fun themeCodeRoundTrip() {
        val t = CustomTheme("Night: Sky", 0xFF3949AB.toInt(), 0xFF1A237E.toInt(), 0xFF7C4DFF.toInt(), 0xFF00E5FF.toInt())
        assertEquals("AEX-THEME:Night  Sky:3949AB:1A237E:7C4DFF:00E5FF", t.code())
        val back = CustomTheme.parse("look at this! " + t.code() + "\nbye")!!
        assertEquals(t.copy(name = "Night  Sky"), back)
        assertNull(CustomTheme.parse("AEX-THEME:x:12:34:56:78"))
        assertNull(CustomTheme.parse("hello"))
        assertNull(CustomTheme.parse(null))
    }

    @Test fun replayKeepsTheLastFrames() {
        val r = ReplayRing<Int>(3)
        (1..5).forEach(r::add)
        assertEquals(listOf(3, 4, 5), r.snapshot())
        assertEquals(75, ReplayRing<Int>().capacity)
    }

    @Test fun voiceCommands() {
        val a = VoiceCommands.Action::class.java
        assertEquals(VoiceCommands.Action.SCREENSHOT, VoiceCommands.match(listOf("take a screenshot please")))
        assertEquals(VoiceCommands.Action.VOLUME_UP, VoiceCommands.match(listOf("Volume up!")))
        assertEquals(VoiceCommands.Action.BACK, VoiceCommands.match(listOf("go back")))
        assertEquals(VoiceCommands.Action.HOME, VoiceCommands.match(listOf("домой")))
        assertEquals(VoiceCommands.Action.BACK, VoiceCommands.match(listOf("Atrás")))
        assertEquals(VoiceCommands.Action.HOME, VoiceCommands.match(listOf("hmm", "tela inicial")))   // a later guess
        assertNull(VoiceCommands.match(listOf("what a nice day")))
        assertNull(VoiceCommands.match(listOf("homework")))                                         // whole words only
        assertNotNull(a)
    }

    private fun line(vararg xy: Float, n: Int = 20): List<GestureShapes.P> =
        xy.toList().chunked(2).zipWithNext { a, b -> (0 until n).map { i -> val t = i / n.toFloat()
            GestureShapes.P(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t) } }.flatten() + GestureShapes.P(xy[xy.size - 2], xy[xy.size - 1])

    @Test fun gestures() {
        val circle = (0..40).map { i -> val a = -PI / 2 + 2 * PI * i / 40
            GestureShapes.P((300 + 120 * cos(a)).toFloat(), (500 + 110 * sin(a)).toFloat()) }       // a slightly flat circle
        assertEquals(VoiceCommands.Action.HOME, GestureShapes.recognize(circle)?.action)
        assertEquals(VoiceCommands.Action.BACK, GestureShapes.recognize(line(400f, 100f, 150f, 260f, 410f, 420f))?.action)
        assertEquals(VoiceCommands.Action.RECENTS, GestureShapes.recognize(line(100f, 400f, 260f, 120f, 420f, 410f))?.action)
        assertEquals(VoiceCommands.Action.REPLAY, GestureShapes.recognize(line(100f, 300f, 180f, 400f, 400f, 100f))?.action)
        assertEquals(VoiceCommands.Action.GIF, GestureShapes.recognize(line(100f, 100f, 400f, 100f, 100f, 400f, 400f, 400f))?.action)
        assertNull(GestureShapes.recognize(listOf(GestureShapes.P(1f, 1f), GestureShapes.P(1f, 1f))))
        assertNull(GestureShapes.recognize(line(0f, 0f, 500f, 0f)))                                 // a plain line is nothing
    }

    @Test fun dailyPick() {
        assertEquals(-1, DailyPick.index(0, 100))
        val week = (0L until 7L).map { DailyPick.index(5, 20000 + it) }
        assertTrue(week.all { it in 0 until 5 })
        assertTrue(week.toSet().size > 1)                                                          // it does change
        assertEquals(DailyPick.index(5, 20001), DailyPick.index(5, 20001))
        assertEquals(9, DailyPick.factGroup(19))
        assertEquals(0, DailyPick.factGroup(1)); assertEquals(15, DailyPick.factGroup(34))
        val utc = java.util.TimeZone.getTimeZone("UTC")
        assertEquals(1L, DailyPick.today(86_400_000L + 5, utc))
    }
}
