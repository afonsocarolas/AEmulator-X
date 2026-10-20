/* Added for AEmulator X, 2026-10-20: theme maker. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.CustomTheme

/** Where the user's own colour style is kept (one, shared through its text code). */
object CustomThemes {
    private const val KEY = "x_custom_theme"
    fun load(ctx: Context): CustomTheme? = CustomTheme.parse(app.aemu.AppPrefs.prefs(ctx).getString(KEY, null))
    fun save(ctx: Context, t: CustomTheme) = app.aemu.AppPrefs.prefs(ctx).edit().putString(KEY, t.code()).apply()

    fun toXTheme(t: CustomTheme) = XTheme(CustomTheme.ID, R.string.x_tm_custom, t.seed.toLong() and 0xFFFFFFFFL,
        t.a.toLong() and 0xFFFFFFFFL, t.b.toLong() and 0xFFFFFFFFL, t.c.toLong() and 0xFFFFFFFFL)
}

/** [XThemes.byId] that also knows the user's own style. */
fun XThemes.resolve(ctx: Context, id: String?): XTheme =
    if (id == CustomTheme.ID) CustomThemes.load(ctx)?.let { CustomThemes.toXTheme(it) } ?: ALL[0] else byId(id)

/**
 * The theme maker: pick the main colour and the three gradient colours, see them at once on a small preview of the
 * library, then use it. Share sends the theme as a text code; Paste reads a code a friend sent.
 */
@Composable
fun ThemeMakerDialog(onDismiss: () -> Unit, onUse: () -> Unit) {
    val ctx = LocalContext.current
    var t by remember { mutableStateOf(CustomThemes.load(ctx) ?: CustomTheme.DEFAULT) }
    var picking by remember { mutableIntStateOf(-1) }       // 0 = main, 1..3 = gradient
    val dark = isXDark()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.x_tm_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(t.name, { t = t.copy(name = it.take(30)) }, label = { Text(stringResource(R.string.x_tm_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Slot(R.string.x_tm_main, t.seed) { picking = 0 }
                    Slot(R.string.x_tm_g1, t.a) { picking = 1 }
                    Slot(R.string.x_tm_g2, t.b) { picking = 2 }
                    Slot(R.string.x_tm_g3, t.c) { picking = 3 }
                }
                Preview(t, dark)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { share(ctx, t) }) { Text(stringResource(R.string.x_tm_share)) }
                    TextButton(onClick = {
                        val clip = ctx.getSystemService(ClipboardManager::class.java)?.primaryClip
                        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString()
                        val got = CustomTheme.parse(text)
                        if (got != null) t = got
                        Toast.makeText(ctx, if (got != null) R.string.x_tm_pasted else R.string.x_tm_no_code, Toast.LENGTH_SHORT).show()
                    }) { Text(stringResource(R.string.x_tm_paste)) }
                    TextButton(onClick = { t = CustomTheme.DEFAULT.copy(name = t.name) }) { Text(stringResource(R.string.x_tm_reset)) }
                }
            }
        },
        confirmButton = { Button(onClick = { CustomThemes.save(ctx, t); onUse() }) { Text(stringResource(R.string.x_tm_use)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
    if (picking >= 0) {
        val now = when (picking) { 0 -> t.seed; 1 -> t.a; 2 -> t.b; else -> t.c }
        ColorPickerDialog(Color(now), onDismiss = { picking = -1 }, onReset = {
            val d = CustomTheme.DEFAULT
            t = when (picking) { 0 -> t.copy(seed = d.seed); 1 -> t.copy(a = d.a); 2 -> t.copy(b = d.b); else -> t.copy(c = d.c) }
            picking = -1
        }) { c ->
            val v = c.toArgb() or 0xFF000000.toInt()
            t = when (picking) { 0 -> t.copy(seed = v); 1 -> t.copy(a = v); 2 -> t.copy(b = v); else -> t.copy(c = v) }
            picking = -1
        }
    }
}

@Composable
private fun Slot(label: Int, color: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(4.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(color)).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
    }
}

/** A tiny library: the gradient wordmark, a firmware card and its Start button, in the theme being made. */
@Composable
private fun Preview(t: CustomTheme, dark: Boolean) {
    val s = schemeFromSeed(Color(t.seed), dark)
    val grad = Brush.linearGradient(listOf(Color(t.a), Color(t.b), Color(t.c)))
    Surface(color = s.background, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().border(1.dp, s.outlineVariant, RoundedCornerShape(20.dp))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AEmulator ", color = s.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(grad).padding(horizontal = 8.dp, vertical = 1.dp)) {
                    Text("X", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
            Surface(color = s.surfaceContainerHigh, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(s.primaryContainer), contentAlignment = Alignment.Center) {
                        Text("🍫", fontSize = 20.sp)
                    }
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("KitKat 4.4", color = s.onSurface, fontWeight = FontWeight.Medium)
                        Box(Modifier.padding(top = 3.dp).clip(RoundedCornerShape(8.dp)).background(grad).padding(horizontal = 6.dp)) {
                            Text("API 19", color = Color.White, fontSize = 11.sp)
                        }
                    }
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(s.primary).padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Text(stringResource(R.string.x_tm_start), color = s.onPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(grad))
        }
    }
}

private fun share(ctx: Context, t: CustomTheme) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.x_tm_share_text, t.name, t.code()))
    runCatching { ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
