/* Added for AEmulator X, 2026-10-19: the trophy shelf. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aemu.R
import app.aemu.core.Achievements
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Every badge: unlocked ones bright, the others faded with their progress. */
@Composable
fun AchievementsDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val facts by produceState<Achievements.Facts?>(null) { value = withContext(Dispatchers.IO) { Achievements.facts(ctx) } }
    val f = facts
    val done = f?.let { x -> Achievements.ALL.count { it.unlocked(x) } } ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ach_title, done, Achievements.ALL.size)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(Achievements.ALL, key = { it.id }) { b ->
                    val on = f != null && b.unlocked(f)
                    Row(Modifier.fillMaxWidth().alpha(if (on) 1f else 0.5f), verticalAlignment = Alignment.CenterVertically) {
                        Text(b.emoji, fontSize = 30.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(b.title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(b.desc), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!on && f != null && b.target > 1) {
                                LinearProgressIndicator(progress = { b.progress(f).toFloat() / b.target },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                                Text("${b.progress(f)} / ${b.target}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (on) Text("✓", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
