/* Added for AEmulator X, 2026-10-20: gesture shortcuts. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.GestureShapes
import kotlinx.coroutines.delay

/**
 * A see-through layer over the running system: draw one shape and its shortcut runs ([onShape]); a tap outside a
 * stroke closes it ([onClose]). Touches drawn here never reach the guest.
 */
@Composable
fun GesturePad(onShape: (GestureShapes.Shape) -> Unit, onClose: () -> Unit) {
    val points = remember { mutableStateListOf<Offset>() }
    var result by remember { mutableStateOf<String?>(null) }
    var matched by remember { mutableStateOf<GestureShapes.Shape?>(null) }
    LaunchedEffect(result) {
        if (result != null) {
            delay(600)
            matched?.let(onShape)
            if (matched != null) onClose() else { result = null; points.clear() }
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0x99000000))
        .pointerInput(Unit) { detectTapGestures { onClose() } }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { points.clear(); points.add(it); result = null },
                onDragEnd = {
                    val s = GestureShapes.recognize(points.map { GestureShapes.P(it.x, it.y) })
                    matched = s
                    result = s?.symbol ?: "?"
                },
            ) { change, _ -> change.consume(); points.add(change.position) }
        }) {
        Canvas(Modifier.fillMaxSize()) {
            if (points.size > 1) {
                val path = Path().apply { moveTo(points[0].x, points[0].y); for (p in points.drop(1)) lineTo(p.x, p.y) }
                drawPath(path, Color(0xFF18FFFF), style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp)
            .background(Color(0xCC000000), RoundedCornerShape(16.dp)).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.x_gs_title), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(stringResource(R.string.x_gs_legend), color = Color(0xFFB2EBF2), fontSize = 13.sp)
        }
        result?.let { r ->
            Text(if (r == "?") stringResource(R.string.x_gs_unknown) else r, color = Color.White, fontSize = if (r == "?") 20.sp else 72.sp,
                fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
        }
        Text(stringResource(R.string.x_gs_close), color = Color(0xFFBDBDBD), fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }
}
