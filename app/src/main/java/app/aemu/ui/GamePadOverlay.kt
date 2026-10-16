/* Added for AEmulator X, 2026-10-16: on-screen game buttons over the VM screen. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Translucent D-pad (left), A/B/X/Y (right) and Select/Start (bottom centre). [onKey] gets Android key codes
 * (KEYCODE_DPAD_*, KEYCODE_BUTTON_*), the same ones a real controller sends, so GamepadMap handles both.
 * Every button follows its own finger: hold a direction and press A at the same time.
 */
@Composable
fun GamePadOverlay(onKey: (keyCode: Int, down: Boolean) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().navigationBarsPadding().padding(16.dp)) {
        // D-pad
        Column(Modifier.align(Alignment.BottomStart), horizontalAlignment = Alignment.CenterHorizontally) {
            PadKey(KeyEvent.KEYCODE_DPAD_UP, onKey, icon = Icons.Rounded.KeyboardArrowUp)
            Row {
                PadKey(KeyEvent.KEYCODE_DPAD_LEFT, onKey, icon = Icons.Rounded.KeyboardArrowLeft)
                Spacer(Modifier.size(52.dp))
                PadKey(KeyEvent.KEYCODE_DPAD_RIGHT, onKey, icon = Icons.Rounded.KeyboardArrowRight)
            }
            PadKey(KeyEvent.KEYCODE_DPAD_DOWN, onKey, icon = Icons.Rounded.KeyboardArrowDown)
        }
        // face buttons in the usual diamond: Y top, X left, B right, A bottom
        Column(Modifier.align(Alignment.BottomEnd), horizontalAlignment = Alignment.CenterHorizontally) {
            PadKey(KeyEvent.KEYCODE_BUTTON_Y, onKey, label = "Y", shape = CircleShape)
            Row {
                PadKey(KeyEvent.KEYCODE_BUTTON_X, onKey, label = "X", shape = CircleShape)
                Spacer(Modifier.size(52.dp))
                PadKey(KeyEvent.KEYCODE_BUTTON_B, onKey, label = "B", shape = CircleShape)
            }
            PadKey(KeyEvent.KEYCODE_BUTTON_A, onKey, label = "A", shape = CircleShape)
        }
        Row(Modifier.align(Alignment.BottomCenter), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PadKey(KeyEvent.KEYCODE_BUTTON_SELECT, onKey, label = "SELECT", width = 76.dp, height = 30.dp)
            PadKey(KeyEvent.KEYCODE_BUTTON_START, onKey, label = "START", width = 76.dp, height = 30.dp)
        }
    }
}

@Composable
private fun PadKey(
    keyCode: Int,
    onKey: (Int, Boolean) -> Unit,
    label: String? = null,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    width: Dp = 52.dp,
    height: Dp = 52.dp,
) {
    var pressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    Box(
        Modifier.padding(2.dp).size(width, height)
            .background(Color.White.copy(alpha = if (pressed) 0.42f else 0.16f), shape)
            .border(1.5.dp, Color.White.copy(alpha = 0.45f), shape)
            .pointerInput(keyCode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    pressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onKey(keyCode, true)
                    waitForUpOrCancellation()
                    pressed = false
                    onKey(keyCode, false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) Icon(icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(30.dp))
        if (label != null) Text(label, color = Color.White.copy(alpha = 0.9f),
            style = (if (label.length > 1) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleMedium)
                .copy(fontWeight = FontWeight.Bold))
    }
}
