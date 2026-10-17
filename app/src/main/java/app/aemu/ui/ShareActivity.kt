/* Added for AEmulator X, 2026-10-17: "Send to emulated phone" share target. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.ui

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import app.aemu.R
import app.aemu.core.LiveVmStatus

/**
 * Receives files from Android's Share sheet and hands them to the running system's screen (VmActivity, in the :vm
 * process), which copies them into the firmware's Download folder and installs APKs. Without a running system it
 * says so and opens the library.
 */
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uris = shared(intent)
        val active = runCatching { LiveVmStatus(filesDir).activeId() }.getOrNull()
        when {
            uris.isEmpty() -> {}
            active == null -> {
                Toast.makeText(this, R.string.x_send_no_vm, Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            else -> {
                val clip = ClipData.newRawUri("files", uris.first()).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
                startActivity(Intent(this, VmActivity::class.java).setAction(VmActivity.ACTION_SEND_FILES)
                    .putExtra(VmActivity.EXTRA_ID, active)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .also { it.clipData = clip })
            }
        }
        finish()
    }

    private fun shared(i: Intent?): List<Uri> {
        i ?: return emptyList()
        val out = ArrayList<Uri>()
        @Suppress("DEPRECATION")
        when (i.action) {
            Intent.ACTION_SEND -> (i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let(out::add)
            Intent.ACTION_SEND_MULTIPLE -> i.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(out::addAll)
        }
        i.clipData?.let { c -> for (n in 0 until c.itemCount) c.getItemAt(n).uri?.let(out::add) }
        return out.distinct()
    }
}
