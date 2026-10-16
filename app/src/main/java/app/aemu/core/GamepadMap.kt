/* Added for AEmulator X, 2026-10-16: game controller support. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.view.KeyEvent

/**
 * Host game controller → guest Linux input codes (sent through [InputService.key]).
 *   GAMEPAD: Android's own gamepad buttons (BTN_A…), which Generic.kl maps to KEYCODE_BUTTON_* on 4.0+ games.
 *   CLASSIC: the controller acts like a phone's D-pad and keys, for 2.x-era games (Xperia Play style):
 *            A = D-pad centre, B = Back, X = Space, Y = Search, Start = Enter, Select = Menu, L1/R1 = Volume.
 * In both modes the D-pad, left stick and hat move the arrow keys.
 */
object GamepadMap {
    const val AUTO = 0
    const val GAMEPAD = 1
    const val CLASSIC = 2

    // Linux input-event-codes.h
    const val KEY_UP = 103; const val KEY_DOWN = 108; const val KEY_LEFT = 105; const val KEY_RIGHT = 106
    const val KEY_ENTER = 28; const val KEY_SPACE = 57; const val KEY_REPLY = 232 // KEY_REPLY = DPAD_CENTER in Generic.kl
    const val BTN_A = 304; const val BTN_B = 305; const val BTN_X = 307; const val BTN_Y = 308
    const val BTN_TL = 310; const val BTN_TR = 311; const val BTN_TL2 = 312; const val BTN_TR2 = 313
    const val BTN_SELECT = 314; const val BTN_START = 315; const val BTN_MODE = 316; const val BTN_THUMBL = 317; const val BTN_THUMBR = 318

    /** The mode actually used: AUTO picks GAMEPAD from Android 4.0 (API 14, Generic.kl has gamepad buttons) on. */
    fun resolve(mode: Int, api: Int): Int = when (mode) {
        GAMEPAD, CLASSIC -> mode
        else -> if (api >= 14) GAMEPAD else CLASSIC
    }

    /** Guest code for a host gamepad key, or null when the key is not a controller key. */
    fun key(keyCode: Int, mode: Int): Int? {
        dpad(keyCode)?.let { return it }
        return if (mode == GAMEPAD) when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> BTN_A; KeyEvent.KEYCODE_BUTTON_B -> BTN_B
            KeyEvent.KEYCODE_BUTTON_X -> BTN_X; KeyEvent.KEYCODE_BUTTON_Y -> BTN_Y
            KeyEvent.KEYCODE_BUTTON_L1 -> BTN_TL; KeyEvent.KEYCODE_BUTTON_R1 -> BTN_TR
            KeyEvent.KEYCODE_BUTTON_L2 -> BTN_TL2; KeyEvent.KEYCODE_BUTTON_R2 -> BTN_TR2
            KeyEvent.KEYCODE_BUTTON_SELECT -> BTN_SELECT; KeyEvent.KEYCODE_BUTTON_START -> BTN_START
            KeyEvent.KEYCODE_BUTTON_MODE -> BTN_MODE
            KeyEvent.KEYCODE_BUTTON_THUMBL -> BTN_THUMBL; KeyEvent.KEYCODE_BUTTON_THUMBR -> BTN_THUMBR
            else -> null
        } else when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> KEY_REPLY; KeyEvent.KEYCODE_BUTTON_B -> InputService.KEY_BACK
            KeyEvent.KEYCODE_BUTTON_X -> KEY_SPACE; KeyEvent.KEYCODE_BUTTON_Y -> InputService.KEY_SEARCH
            KeyEvent.KEYCODE_BUTTON_L1 -> InputService.KEY_VOLUMEDOWN; KeyEvent.KEYCODE_BUTTON_R1 -> InputService.KEY_VOLUMEUP
            KeyEvent.KEYCODE_BUTTON_START -> KEY_ENTER; KeyEvent.KEYCODE_BUTTON_SELECT -> InputService.KEY_MENU
            KeyEvent.KEYCODE_BUTTON_MODE -> InputService.KEY_HOME
            else -> null
        }
    }

    private fun dpad(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> KEY_UP; KeyEvent.KEYCODE_DPAD_DOWN -> KEY_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> KEY_LEFT; KeyEvent.KEYCODE_DPAD_RIGHT -> KEY_RIGHT
        else -> null
    }

    /**
     * Turns stick/hat/trigger positions into held keys. Feed every joystick motion event to [update]; it calls
     * [send] only when a direction or trigger crosses the threshold.
     */
    class Axes(private val send: (code: Int, down: Boolean) -> Unit) {
        private val held = HashSet<Int>()
        private fun set(code: Int, on: Boolean) {
            if (on == (code in held)) return
            if (on) held.add(code) else held.remove(code)
            send(code, on)
        }

        /** x/y: left stick or hat (−1…1, the stronger of the two); l2/r2: triggers (0…1); [mode] for the triggers. */
        fun update(x: Float, y: Float, l2: Float, r2: Float, mode: Int) {
            set(KEY_LEFT, x < -0.5f); set(KEY_RIGHT, x > 0.5f)
            set(KEY_UP, y < -0.5f); set(KEY_DOWN, y > 0.5f)
            if (mode == GAMEPAD) { set(BTN_TL2, l2 > 0.5f); set(BTN_TR2, r2 > 0.5f) }
        }

        /** Lets go of everything still held (controller unplugged, VM screen left). */
        fun releaseAll() { held.toList().forEach { set(it, false) } }
    }
}
