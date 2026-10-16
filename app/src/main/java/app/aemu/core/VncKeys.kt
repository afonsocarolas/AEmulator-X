/* Added for AEmulator X, 2026-10-16: VNC server keyboard. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

/**
 * X11 keysyms (what VNC viewers send) → Linux key codes for the guest. Letters and digits map to the US layout keys;
 * shifted symbols map to their base key, so the viewer's Shift (also forwarded) produces them. Escape is Back and
 * Home is Home, which is what people reach for on a PC keyboard.
 */
object VncKeys {
    const val KEY_LEFTSHIFT = 42; const val KEY_RIGHTSHIFT = 54; const val KEY_LEFTCTRL = 29; const val KEY_LEFTALT = 56
    const val KEY_BACKSPACE = 14; const val KEY_TAB = 15; const val KEY_SPACE = 57; const val KEY_DELETE = 111
    const val KEY_LEFT = 105; const val KEY_RIGHT = 106; const val KEY_UP = 103; const val KEY_DOWN = 108

    private val letters = intArrayOf(30, 48, 46, 32, 18, 33, 34, 35, 23, 36, 37, 38, 50, 49, 24, 25, 16, 19, 31, 20, 22, 47, 17, 45, 21, 44)
    private val digits = intArrayOf(11, 2, 3, 4, 5, 6, 7, 8, 9, 10)          // 0..9
    private val symbols = mapOf(
        '-' to 12, '_' to 12, '=' to 13, '+' to 13, '[' to 26, '{' to 26, ']' to 27, '}' to 27, ';' to 39, ':' to 39,
        '\'' to 40, '"' to 40, '`' to 41, '~' to 41, '\\' to 43, '|' to 43, ',' to 51, '<' to 51, '.' to 52, '>' to 52,
        '/' to 53, '?' to 53, '!' to 2, '@' to 3, '#' to 4, '$' to 5, '%' to 6, '^' to 7, '&' to 8, '*' to 9, '(' to 10, ')' to 11,
    )

    fun linux(keysym: Int): Int? = when (keysym) {
        in 'a'.code..'z'.code -> letters[keysym - 'a'.code]
        in 'A'.code..'Z'.code -> letters[keysym - 'A'.code]
        in '0'.code..'9'.code -> digits[keysym - '0'.code]
        ' '.code -> KEY_SPACE
        0xff0d, 0xff8d -> InputService.KEY_ENTER                      // Return, KP_Enter
        0xff08 -> KEY_BACKSPACE; 0xff09 -> KEY_TAB; 0xffff -> KEY_DELETE
        0xff1b -> InputService.KEY_BACK                               // Escape = Back
        0xff50 -> InputService.KEY_HOME                               // Home
        0xff51 -> KEY_LEFT; 0xff52 -> KEY_UP; 0xff53 -> KEY_RIGHT; 0xff54 -> KEY_DOWN
        0xffbe -> InputService.KEY_MENU                               // F1 = Menu
        0xffe1 -> KEY_LEFTSHIFT; 0xffe2 -> KEY_RIGHTSHIFT; 0xffe3, 0xffe4 -> KEY_LEFTCTRL; 0xffe9, 0xffea -> KEY_LEFTALT
        else -> if (keysym in 0x20..0x7e) symbols[keysym.toChar()] else null
    }
}
