package app.aemu.core

import app.aemu.ui.StatsMath
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate

class X19FeaturesTest {
    private val today = LocalDate.of(2026, 10, 19)

    @Test fun streaksAndWeek() {
        val days = mapOf(today to 60_000L, today.minusDays(1) to 1000L, today.minusDays(2) to 5000L,
            today.minusDays(5) to 9000L, today.minusDays(6) to 9000L, today.minusDays(7) to 9000L, today.minusDays(8) to 9000L)
        assertEquals(3, PlayDays.currentStreak(days, today))
        assertEquals(4, PlayDays.bestStreak(days))
        assertEquals(2, PlayDays.currentStreak(days - today, today))          // today not played yet: count from yesterday
        val week = PlayDays.lastWeek(days, today)
        assertEquals(7, week.size); assertEquals(today, week.last().first); assertEquals(0L, week[3].second)
        assertEquals(mapOf(today to 5L), PlayDays.parse("2026-10-19 5\nbad line\n"))
    }

    @Test fun topLists() {
        val t = StatsMath.top(listOf("KitKat" to 10L, "Froyo" to 0L, "KitKat" to 5L, "Pie" to 20L), 5)
        assertEquals(listOf("Pie" to 20L, "KitKat" to 15L), t)
    }

    @Test fun macroRoundTripAndTiming() {
        val m = TouchMacro(listOf(TouchMacro.Step(0, listOf(InputService.P(0, 10, 20))), TouchMacro.Step(120, emptyList())))
        val back = TouchMacro.parse(m.format())
        assertEquals(m.steps, back.steps)
        val seen = ArrayList<List<InputService.P>>()
        val t0 = System.currentTimeMillis()
        back.play({ seen.add(it) }, { false })
        assertTrue(System.currentTimeMillis() - t0 >= 100)
        assertEquals(listOf(listOf(InputService.P(0, 10, 20)), emptyList(), emptyList()), seen)   // ends lifted
        val stopped = ArrayList<List<InputService.P>>()
        back.play({ stopped.add(it) }, { true })
        assertEquals(listOf(emptyList<InputService.P>()), stopped)                                // stop = lift and leave
    }

    @Test fun recorderStartsAtTheFirstTouchAndLifts() {
        val r = TouchMacro.Recorder()
        Thread.sleep(30)
        r.add(listOf(InputService.P(0, 5, 5))); r.add(listOf(InputService.P(0, 5, 5)))       // repeats are dropped
        val m = r.finish()
        assertEquals(0L, m.steps.first().atMs)
        assertEquals(2, m.steps.size); assertTrue(m.steps.last().points.isEmpty())
    }

    private fun parcelDump(vararg parts: Any): String {
        val b = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)
        for (p in parts) when (p) {
            is Int -> b.putInt(p)
            is String -> { b.putInt(p.length); p.forEach { b.putChar(it) }; b.putChar('\u0000'); while (b.position() % 4 != 0) b.put(0) }
        }
        val bytes = b.array().copyOf(b.position())
        val words = (bytes.indices step 4).map { ByteBuffer.wrap(bytes, it, 4).order(ByteOrder.LITTLE_ENDIAN).int }
        return "Result: Parcel(\n" + words.chunked(4).mapIndexed { i, w ->
            "  0x%08x: ".format(i * 16) + w.joinToString(" ") { "%08x".format(it) } + " '................'"
        }.joinToString("\n") + ")"
    }

    @Test fun guestClipboardReply() {
        // no exception, ClipData present, label, one MIME type, one item with its text
        val dump = parcelDump(0, 1, 1, "label", 1, "text/plain", 1, 1, "Hello from Android 1.6 ⚽")
        assertEquals("Hello from Android 1.6 ⚽", GuestClipboard.text(dump))
        assertEquals("only text", GuestClipboard.text(parcelDump(0, 1, "only text")))     // 2.x getClipboardText
        assertNull(GuestClipboard.text("Result: Parcel(00000000 00000000   '........')"))
        assertTrue(GuestClipboard.command(19).contains("com.android.shell"))
    }

    @Test fun gifSize() {
        assertEquals(240 to 400, GifRecorder.size(480, 800))
        assertEquals(240 to 135, GifRecorder.size(1920, 1080))
    }
}
