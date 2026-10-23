/* Added for AEmulator X, 2026-10-23: firmware badges. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.core.FirmwareTags
import app.aemu.core.FirmwareTags.Tag
import app.aemu.core.GuestImage

private fun label(t: Tag) = when (t) {
    Tag.NEW -> R.string.x_tag_new; Tag.NEVER_BOOTED -> R.string.x_tag_never; Tag.TOP -> R.string.x_tag_top; Tag.BIT64 -> R.string.x_tag_64
    Tag.FAST -> R.string.x_tag_fast; Tag.CLASSIC -> R.string.x_tag_classic; Tag.CONTAINER -> R.string.x_tag_container
}

private fun color(t: Tag) = when (t) {
    Tag.NEW -> Color(0xFF00C853); Tag.NEVER_BOOTED -> Color(0xFF546E7A); Tag.TOP -> Color(0xFFFFA000); Tag.BIT64 -> Color(0xFF6A3DF0)
    Tag.FAST -> Color(0xFF0091EA); Tag.CLASSIC -> Color(0xFF8D6E63); Tag.CONTAINER -> Color(0xFF00897B)
}

/** The firmware's tags as small coloured pills (nothing when it has none). [compact]: emoji only, for small tiles. */
@Composable
fun FirmwareTagChips(img: GuestImage, isTop: Boolean, modifier: Modifier = Modifier, compact: Boolean = false, max: Int = 3) {
    val tags = remember(img, isTop) { FirmwareTags.of(img, isTop, max = max) }
    if (tags.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (t in tags) Text(if (compact) t.emoji else "${t.emoji} ${stringResource(label(t))}", color = Color.White,
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.background(color(t).copy(alpha = 0.9f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
    }
}
