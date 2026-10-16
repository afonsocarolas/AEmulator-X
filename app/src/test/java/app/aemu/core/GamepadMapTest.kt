/* Added for AEmulator X, 2026-10-16: game controller support. GPL-3.0; see LICENSE. */
package app.aemu.core

import android.view.KeyEvent
import org.junit.Assert.*
import org.junit.Test

class GamepadMapTest {
    @Test fun autoPicksGamepadFromIcsOn() {
        assertEquals(GamepadMap.CLASSIC, GamepadMap.resolve(GamepadMap.AUTO, 10))
        assertEquals(GamepadMap.GAMEPAD, GamepadMap.resolve(GamepadMap.AUTO, 14))
        assertEquals(GamepadMap.CLASSIC, GamepadMap.resolve(GamepadMap.CLASSIC, 19))
        assertEquals(GamepadMap.GAMEPAD, GamepadMap.resolve(GamepadMap.GAMEPAD, 8))
    }

    @Test fun buttonsFollowTheMode() {
        assertEquals(GamepadMap.BTN_A, GamepadMap.key(KeyEvent.KEYCODE_BUTTON_A, GamepadMap.GAMEPAD))
        assertEquals(GamepadMap.BTN_START, GamepadMap.key(KeyEvent.KEYCODE_BUTTON_START, GamepadMap.GAMEPAD))
        assertEquals(GamepadMap.KEY_REPLY, GamepadMap.key(KeyEvent.KEYCODE_BUTTON_A, GamepadMap.CLASSIC))
        assertEquals(InputService.KEY_BACK, GamepadMap.key(KeyEvent.KEYCODE_BUTTON_B, GamepadMap.CLASSIC))
        assertEquals(GamepadMap.KEY_ENTER, GamepadMap.key(KeyEvent.KEYCODE_BUTTON_START, GamepadMap.CLASSIC))
        // the D-pad is arrows in both modes; other keys are not controller keys
        for (m in listOf(GamepadMap.GAMEPAD, GamepadMap.CLASSIC)) {
            assertEquals(GamepadMap.KEY_UP, GamepadMap.key(KeyEvent.KEYCODE_DPAD_UP, m))
            assertEquals(GamepadMap.KEY_RIGHT, GamepadMap.key(KeyEvent.KEYCODE_DPAD_RIGHT, m))
            assertNull(GamepadMap.key(KeyEvent.KEYCODE_A, m))
        }
    }

    @Test fun sticksPressAndReleaseArrowsOnce() {
        val sent = mutableListOf<Pair<Int, Boolean>>()
        val axes = GamepadMap.Axes { c, d -> sent += c to d }
        axes.update(0.9f, 0f, 0f, 0f, GamepadMap.GAMEPAD)
        axes.update(0.95f, 0f, 0f, 0f, GamepadMap.GAMEPAD) // still right: nothing new
        axes.update(0f, -0.8f, 0f, 0.9f, GamepadMap.GAMEPAD)
        assertEquals(listOf(GamepadMap.KEY_RIGHT to true, GamepadMap.KEY_RIGHT to false, GamepadMap.KEY_UP to true,
            GamepadMap.BTN_TR2 to true), sent)
        sent.clear(); axes.releaseAll()
        assertEquals(setOf(GamepadMap.KEY_UP to false, GamepadMap.BTN_TR2 to false), sent.toSet())
        sent.clear(); axes.update(0f, 0f, 1f, 1f, GamepadMap.CLASSIC) // no trigger buttons in classic mode
        assertTrue(sent.isEmpty())
    }
}
