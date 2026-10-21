/* Added for AEmulator X, 2026-10-21: Device museum. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.DailyPick
import app.aemu.core.DeviceMuseum
import app.aemu.core.DeviceMuseum.Nav
import app.aemu.core.GuestImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Famous Android phones and tablets, one per page, drawn in code; boot a firmware of the same Android if you have one. */
class MuseumActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(app.aemu.AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AemuTheme { Museum(onClose = ::finish) } }
    }
}

@Composable
private fun Museum(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val images by produceState(emptyList<GuestImage>()) {
        value = withContext(Dispatchers.IO) { runCatching { app.aemu.core.ImageStore.list(ctx) }.getOrDefault(emptyList()) }.filter { it.baseId.isEmpty() }
    }
    val facts = stringArrayResource(R.array.x_museum_facts)
    val pager = rememberPagerState { DeviceMuseum.ALL.size }
    val scope = rememberCoroutineScope()
    AuroraBackground(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🏛️ " + stringResource(R.string.x_museum_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) }
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { i ->
                val d = DeviceMuseum.ALL[i]
                val mine = images.firstOrNull { DailyPick.factGroup(it.api) == DailyPick.factGroup(d.api) }
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize().padding(8.dp)) { drawDevice(d) }
                    }
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(d.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(XBrand.gradient).padding(horizontal = 10.dp, vertical = 3.dp)) {
                                    Text("${d.year}", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(stringResource(R.string.x_museum_by, d.maker, d.android), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("💡 " + facts.getOrElse(i) { "" }, style = MaterialTheme.typography.bodyLarge)
                            if (mine != null) Button(onClick = { VmActivity.start(ctx, mine.id) }) { Text(stringResource(R.string.x_museum_boot, mine.name)) }
                            else Text(stringResource(R.string.x_museum_none, d.android), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            // a dot per device, the current one long; tap to jump
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
                DeviceMuseum.ALL.indices.forEach { i ->
                    val on = i == pager.currentPage
                    Box(Modifier.padding(3.dp).size(if (on) 22.dp else 8.dp, 8.dp).clip(CircleShape)
                        .background(if (on) XBrand.Violet else MaterialTheme.colorScheme.outlineVariant)
                        .clickable { scope.launch { pager.animateScrollToPage(i) } })
                }
            }
            Text(stringResource(R.string.x_museum_swipe), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        }
    }
}

/** The device, as big as fits: body, screen with a tiny home screen, and its buttons, keyboard or trackball. */
private fun DrawScope.drawDevice(d: DeviceMuseum.Device) {
    val keyboardRoom = if (d.keyboard) 0.42f else 0f
    // fit body (ratio = h / w), plus the slid-out keyboard under it
    val totalRatio = d.ratio * (1f + keyboardRoom)
    var bw = size.width * (if (d.tablet && d.ratio < 1f) 0.92f else 0.6f)
    if (bw * totalRatio > size.height) bw = size.height / totalRatio
    val bh = bw * d.ratio
    val left = (size.width - bw) / 2
    val top = (size.height - bh * (1f + keyboardRoom)) / 2
    val body = Color(d.color)
    val r = bw * 0.09f
    // keyboard part first (under the phone)
    if (d.keyboard) {
        val kTop = top + bh * 0.82f
        drawRoundRect(Color(0xFF3A3A3A), Offset(left, kTop), Size(bw, bh * 0.6f), CornerRadius(r))
        val rows = 4; val cols = 10
        val kw = bw * 0.86f / cols; val kh = bh * 0.38f / rows
        for (rr in 0 until rows) for (c in 0 until cols) {
            drawRoundRect(Color(0xFF8A8A8A), Offset(left + bw * 0.07f + c * kw + kw * 0.1f, kTop + bh * 0.18f + rr * kh + kh * 0.12f),
                Size(kw * 0.8f, kh * 0.76f), CornerRadius(kw * 0.15f))
        }
    }
    drawRoundRect(Color(0x55000000), Offset(left + 6f, top + 10f), Size(bw, bh), CornerRadius(r))
    drawRoundRect(Brush.linearGradient(listOf(body, Color(body.red * 0.6f, body.green * 0.6f, body.blue * 0.6f))), Offset(left, top), Size(bw, bh), CornerRadius(r))
    drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(left, top), Size(bw, bh), CornerRadius(r), style = Stroke(width = 3f))
    // screen
    val side = bw * (if (d.tablet) 0.08f else 0.06f)
    val chin = when (d.nav) { Nav.ON_SCREEN -> side * 1.4f; Nav.KEYS -> bh * 0.24f; else -> bh * 0.13f }
    val forehead = if (d.nav == Nav.ON_SCREEN) side * 1.6f else bh * 0.1f
    val sTop = top + forehead; val sLeft = left + side
    val sw = bw - 2 * side; val sh = bh - forehead - chin
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1E3A5F), Color(0xFF0A1424)), sTop, sTop + sh), Offset(sLeft, sTop), Size(sw, sh))
    // a little home screen: status bar, clock, 3×4 icons
    drawRect(Color(0x66000000), Offset(sLeft, sTop), Size(sw, sh * 0.04f))
    val icon = sw / 6f
    val colors = listOf(Color(0xFF66BB6A), Color(0xFF42A5F5), Color(0xFFFFCA28), Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF26C6DA))
    for (row in 0 until 3) for (c in 0 until 4) {
        val cx = sLeft + sw * (0.17f + c * 0.22f); val cy = sTop + sh * (0.35f + row * 0.17f)
        if (d.api >= 21) drawCircle(colors[(row * 4 + c) % colors.size], icon * 0.38f, Offset(cx, cy))
        else drawRoundRect(colors[(row * 4 + c) % colors.size], Offset(cx - icon * 0.38f, cy - icon * 0.38f), Size(icon * 0.76f, icon * 0.76f), CornerRadius(icon * 0.12f))
    }
    drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(sLeft + sw * 0.3f, sTop + sh * 0.12f), Size(sw * 0.4f, sh * 0.07f), CornerRadius(sh * 0.02f))
    val ink = Color.White.copy(alpha = 0.7f)
    val navY = sTop + sh - sh * 0.05f
    if (d.nav == Nav.ON_SCREEN) {
        drawRect(Color(0xFF000000), Offset(sLeft, sTop + sh * 0.9f), Size(sw, sh * 0.1f))
        val g = sw * 0.035f
        drawCircle(ink, g, Offset(sLeft + sw * 0.5f, navY), style = Stroke(width = 3f))                 // home
        drawLine(ink, Offset(sLeft + sw * 0.22f + g, navY - g), Offset(sLeft + sw * 0.22f - g, navY), 3f) // back
        drawLine(ink, Offset(sLeft + sw * 0.22f - g, navY), Offset(sLeft + sw * 0.22f + g, navY + g), 3f)
        drawRect(ink, Offset(sLeft + sw * 0.78f - g, navY - g), Size(2 * g, 2 * g), style = Stroke(width = 3f)) // recents
    }
    // speaker
    drawRoundRect(Color(0xFF555555), Offset(left + bw * 0.38f, top + forehead * 0.42f), Size(bw * 0.24f, forehead * 0.14f), CornerRadius(4f))
    // chin
    val chinMid = top + bh - chin / 2
    when (d.nav) {
        Nav.KEYS -> {
            val kw = bw * 0.16f
            for (k in 0 until 4) drawRoundRect(Color(0xFF666666), Offset(left + bw * 0.06f + k * (bw * 0.88f / 4), chinMid - chin * 0.2f),
                Size(kw, chin * 0.25f), CornerRadius(6f))
        }
        Nav.CAPACITIVE -> for (k in 0 until 4) drawCircle(ink.copy(alpha = 0.5f), bw * 0.02f, Offset(left + bw * (0.2f + k * 0.2f), chinMid - (if (d.trackball) chin * 0.15f else 0f)))
        Nav.HOME_BUTTON -> drawRoundRect(Color(0xFF3A3A3A), Offset(left + bw * 0.38f, chinMid - chin * 0.2f), Size(bw * 0.24f, chin * 0.4f), CornerRadius(chin * 0.2f))
        Nav.ON_SCREEN -> {}
    }
    if (d.trackball) {
        val tb = Offset(left + bw / 2, chinMid + chin * 0.22f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFEEEEEE), Color(0xFF777777)), tb, bw * 0.05f), bw * 0.045f, tb)
    }
}
