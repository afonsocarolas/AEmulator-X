package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class X24FeaturesTest {
    @Test fun frameLayouts() {
        assertArrayEquals(intArrayOf(480, 800, 0, 0), ScreenshotFrame.layout(ScreenshotFrame.Style.NONE, 480, 800))
        val phone = ScreenshotFrame.layout(ScreenshotFrame.Style.PHONE, 480, 800)
        assertTrue(phone[0] > 480 && phone[1] > 800)
        assertEquals(phone[0] - 480, 2 * phone[2])                              // screen centred left to right
        val pol = ScreenshotFrame.layout(ScreenshotFrame.Style.POLAROID, 480, 800)
        assertTrue(pol[1] - 800 - pol[3] > pol[3])                              // more room under the photo (caption)
        assertEquals(ScreenshotFrame.Style.POLAROID, ScreenshotFrame.Style.of("polaroid"))
        assertEquals(ScreenshotFrame.Style.NONE, ScreenshotFrame.Style.of("junk"))
        assertEquals(ScreenshotFrame.Style.NONE, ScreenshotFrame.Style.of(null))
    }

    @Test fun notes() {
        assertEquals("best for games", FirmwareNotes.clean("  best for games \n"))
        assertEquals("a\n\nb", FirmwareNotes.clean("a\n\n\n\n\nb"))
        assertEquals(FirmwareNotes.MAX, FirmwareNotes.clean("x".repeat(1000)).length)
        assertEquals("fast · Wi-Fi slow", FirmwareNotes.oneLine("fast\n  Wi-Fi slow"))
    }
}
