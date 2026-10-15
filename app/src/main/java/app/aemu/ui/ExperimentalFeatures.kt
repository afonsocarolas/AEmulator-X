/* Modified for AEmulator Plus, 2026-10-04: app title.
 * Modified for AEmulator X, 2026-10-15: gradient X wordmark (XStyle.kt). GPL-3.0; see NOTICE.md.
 * The experimental-features lock (42 taps on the title) was removed: those features are always available. */
package app.aemu.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@Composable
fun SunsetTitle(style: TextStyle = LocalTextStyle.current, color: Color = Color.Unspecified) {
    XWordmark(style, color)
}
