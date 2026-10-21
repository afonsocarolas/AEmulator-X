/* Added for AEmulator X, 2026-10-16: on-screen game buttons over the VM screen; 2026-10-21: layout editor. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.aemu.core.PadLayout
import app.aemu.core.PadLayout.Group
import kotlin.math.roundToInt

/**
 * Translucent D-pad, A/B/X/Y, Select/Start and (optional) L/R, placed by a [PadLayout]. [onKey] gets Android key codes
 * (KEYCODE_DPAD_*, KEYCODE_BUTTON_*), the same ones a real controller sends, so GamepadMap handles both. Every button
 * follows its own finger: hold a direction and press A at the same time.
 *
 * While [editing], the buttons send nothing: each group can be dragged, and touching one selects it ([selected]).
 */
@Composable
fun GamePadOverlay(
    onKey: (keyCode: Int, down: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    layout: PadLayout = PadLayout(),
    editing: Boolean = false,
    selected: Group? = null,
    onSelect: (Group) -> Unit = {},
    onLayout: (PadLayout) -> Unit = {},
) {
    val keys: (Int, Boolean) -> Unit = if (editing) { _, _ -> } else onKey
    BoxWithConstraints(modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(8.dp)) {
        val density = LocalDensity.current
        val w = with(density) { maxWidth.toPx() }; val h = with(density) { maxHeight.toPx() }
        val latest by rememberUpdatedState(layout)
        for (g in Group.entries) {
            val spot = layout.spot(g)
            if (!spot.shown) continue
            var size by remember(g) { mutableStateOf(IntSize.Zero) }
            Box(Modifier
                .offset { IntOffset((spot.x * w - size.width / 2f).roundToInt(), (spot.y * h - size.height / 2f).roundToInt()) }
                .onSizeChanged { size = it }
                .alpha(if (editing) 1f else layout.opacity)
                .then(if (!editing) Modifier else Modifier
                    .border(2.dp, if (g == selected) Color(0xFF18FFFF) else Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .background(if (g == selected) Color(0x3318FFFF) else Color(0x22000000), RoundedCornerShape(16.dp))
                    .pointerInput(g, w, h) {
                        detectDragGestures(onDragStart = { onSelect(g) }) { change, d ->
                            change.consume()
                            val s = latest.spot(g)
                            onLayout(latest.with(g, s.copy(x = s.x + d.x / w, y = s.y + d.y / h)))
                        }
                    }
                    .pointerInput(g) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false); onSelect(g) } })
                .padding(if (editing) 6.dp else 0.dp)) {
                val u = 52.dp * spot.size
                when (g) {
                    Group.DPAD -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PadKey(KeyEvent.KEYCODE_DPAD_UP, keys, icon = Icons.Rounded.KeyboardArrowUp, width = u, height = u)
                        Row {
                            PadKey(KeyEvent.KEYCODE_DPAD_LEFT, keys, icon = Icons.Rounded.KeyboardArrowLeft, width = u, height = u)
                            Spacer(Modifier.size(u))
                            PadKey(KeyEvent.KEYCODE_DPAD_RIGHT, keys, icon = Icons.Rounded.KeyboardArrowRight, width = u, height = u)
                        }
                        PadKey(KeyEvent.KEYCODE_DPAD_DOWN, keys, icon = Icons.Rounded.KeyboardArrowDown, width = u, height = u)
                    }
                    // face buttons in the usual diamond: Y top, X left, B right, A bottom
                    Group.FACE -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PadKey(KeyEvent.KEYCODE_BUTTON_Y, keys, label = "Y", shape = CircleShape, width = u, height = u)
                        Row {
                            PadKey(KeyEvent.KEYCODE_BUTTON_X, keys, label = "X", shape = CircleShape, width = u, height = u)
                            Spacer(Modifier.size(u))
                            PadKey(KeyEvent.KEYCODE_BUTTON_B, keys, label = "B", shape = CircleShape, width = u, height = u)
                        }
                        PadKey(KeyEvent.KEYCODE_BUTTON_A, keys, label = "A", shape = CircleShape, width = u, height = u)
                    }
                    Group.MENU -> Row(horizontalArrangement = Arrangement.spacedBy(14.dp * spot.size)) {
                        PadKey(KeyEvent.KEYCODE_BUTTON_SELECT, keys, label = "SELECT", width = 76.dp * spot.size, height = 30.dp * spot.size)
                        PadKey(KeyEvent.KEYCODE_BUTTON_START, keys, label = "START", width = 76.dp * spot.size, height = 30.dp * spot.size)
                    }
                    Group.L -> PadKey(KeyEvent.KEYCODE_BUTTON_L1, keys, label = "L", width = 76.dp * spot.size, height = 40.dp * spot.size)
                    Group.R -> PadKey(KeyEvent.KEYCODE_BUTTON_R1, keys, label = "R", width = 76.dp * spot.size, height = 40.dp * spot.size)
                }
            }
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
    val send by rememberUpdatedState(onKey)
    Box(
        Modifier.padding(2.dp).size(width, height)
            .background(Color.White.copy(alpha = if (pressed) 0.42f else 0.16f), shape)
            .border(1.5.dp, Color.White.copy(alpha = 0.45f), shape)
            .pointerInput(keyCode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    pressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    send(keyCode, true)
                    waitForUpOrCancellation()
                    pressed = false
                    send(keyCode, false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) Icon(icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(width * 0.58f))
        if (label != null) Text(label, color = Color.White.copy(alpha = 0.9f),
            style = (if (label.length > 1) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleMedium)
                .copy(fontWeight = FontWeight.Bold))
    }
}
