/* Added for AEmulator X, 2026-10-22: app icons. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aemu.R

/**
 * Other home screen icons, all from Chris's artwork. Android shows the icon of whichever launcher entry is enabled
 * (activity-aliases .ui.Icon<Name> in the manifest), so choosing one enables its entry and disables the others.
 */
object AppIcons {
    class Icon(val id: String, val label: Int, val bg: Long, val fg: Int)

    val ALL = listOf(
        Icon("Default", R.string.x_ic_default, 0xFFFD5751, R.mipmap.ic_launcher_fg),
        Icon("Neon", R.string.x_ic_neon, 0xFF0B0A12, R.mipmap.ic_launcher_neon_fg),
        Icon("Gold", R.string.x_ic_gold, 0xFF3E2723, R.mipmap.ic_launcher_gold_fg),
        Icon("Retro", R.string.x_ic_retro, 0xFF050A05, R.mipmap.ic_launcher_retro_fg),
        Icon("Christmas", R.string.x_ic_christmas, 0xFFC62828, R.mipmap.ic_launcher_christmas_fg),
        Icon("Mono", R.string.x_ic_mono, 0xFFFFFFFF, R.mipmap.ic_launcher_bw_fg),
    )

    private fun component(ctx: Context, id: String) = ComponentName(ctx.packageName, "app.aemu.ui.Icon$id")

    /** The icon in use: the enabled entry (the default one unless another was chosen). */
    fun current(ctx: Context): String {
        val pm = ctx.packageManager
        return ALL.firstOrNull { i ->
            val state = runCatching { pm.getComponentEnabledSetting(component(ctx, i.id)) }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            if (i.id == "Default") state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED && ALL.drop(1).none { isOn(ctx, it.id) }
            else state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }?.id ?: "Default"
    }

    private fun isOn(ctx: Context, id: String) = runCatching {
        ctx.packageManager.getComponentEnabledSetting(component(ctx, id)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }.getOrDefault(false)

    /** Switches the home screen icon (the new entry first, so the app never has none). */
    fun set(ctx: Context, id: String): Boolean = runCatching {
        val pm = ctx.packageManager
        pm.setComponentEnabledSetting(component(ctx, id), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        for (i in ALL) if (i.id != id)
            pm.setComponentEnabledSetting(component(ctx, i.id), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }.isSuccess
}

/** Settings › Appearance: the icons as round previews; tap one to use it. */
@Composable
fun AppIconPicker() {
    val ctx = LocalContext.current
    var current by remember { mutableStateOf(AppIcons.current(ctx)) }
    Text(stringResource(R.string.x_ic_title), style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in AppIcons.ALL) {
            val on = i.id == current
            Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                if (!on && AppIcons.set(ctx, i.id)) {
                    current = i.id
                    Toast.makeText(ctx, R.string.x_ic_changed, Toast.LENGTH_LONG).show()
                }
            }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color(i.bg))
                    .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center) {
                    // the foreground layer is 108 dp with the art in its middle 72 dp, like a launcher shows it
                    Image(painterResource(i.fg), null, contentScale = ContentScale.Fit, modifier = Modifier.requiredSize(84.dp))
                }
                Text(stringResource(i.label), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
