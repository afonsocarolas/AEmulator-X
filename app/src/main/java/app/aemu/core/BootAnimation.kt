/* Added for AEmulator X, 2026-10-17: custom boot animation. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * The firmware's boot animation, chosen per VM ([VmSettings.bootAnim]): "" = the firmware's own, "x" = AEmulator X's
 * (generated here), "custom" = a bootanimation.zip the user picked. Android's bootanimation reads
 * /system/media/bootanimation.zip (4.x first looks at /data/local/bootanimation.zip), so the chosen zip goes to both;
 * the firmware's own file is parked and put back when the choice returns to "". Vendor boot screens that are not a
 * bootanimation.zip (Samsung .qmg, some HTC builds) are not affected.
 */
object BootAnimation {
    const val FIRMWARE = ""
    const val X = "x"
    const val CUSTOM = "custom"

    fun customFile(paths: VmPaths) = File(paths.dir, "bootanimation-custom.zip")
    private fun marker(paths: VmPaths) = File(paths.dir, "bootanimation-applied")
    private fun sys(paths: VmPaths) = File(paths.root, "system/media/bootanimation.zip")
    private fun local(paths: VmPaths) = File(paths.root, "data/local/bootanimation.zip")
    private fun parked(paths: VmPaths) = File(paths.root, "system/.aemu-parked/system#media#bootanimation.zip.x-orig")

    /** Copies a picked zip in after checking it is a boot animation (desc.txt inside). Blocking. */
    fun importCustom(ctx: Context, paths: VmPaths, uri: Uri): Boolean = runCatching {
        val tmp = File(paths.dir, "bootanimation-custom.part")
        ctx.contentResolver.openInputStream(uri)!!.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
        val ok = runCatching { ZipFile(tmp).use { z -> z.getEntry("desc.txt") != null } }.getOrDefault(false)
        if (!ok) { tmp.delete(); return false }
        customFile(paths).delete(); tmp.renameTo(customFile(paths))
    }.getOrDefault(false)

    /**
     * Puts the chosen animation in place (or the firmware's own back) before boot. [w]×[h]: guest screen.
     * [sharedSystem]: a container shares its base's /system, so only its own /data/local copy is touched (4.x reads it).
     */
    fun apply(paths: VmPaths, choice: String, w: Int, h: Int, sharedSystem: Boolean, log: (String) -> Unit) = runCatching {
        val local = local(paths); val marker = marker(paths)
        if (sharedSystem) {
            val src = when (choice) { CUSTOM -> customFile(paths).takeIf { it.isFile }; X -> builtIn(paths, w, h); else -> null }
            if (src == null) { if (marker.isFile) { local.delete(); marker.delete() } }
            else { local.parentFile?.mkdirs(); src.copyTo(local, overwrite = true); marker.writeText("local") }
            return@runCatching
        }
        val sys = sys(paths); val parked = parked(paths)
        if (choice == FIRMWARE) {
            if (!marker.isFile) return@runCatching
            val hadOwn = marker.readText().trim() == "had"
            sys.delete(); local.delete()
            if (hadOwn && parked.isFile) parked.copyTo(sys, overwrite = true)
            marker.delete()
            log("boot animation: firmware's own restored")
            return@runCatching
        }
        val src = if (choice == CUSTOM) customFile(paths).takeIf { it.isFile } else builtIn(paths, w, h)
        if (src == null) { log("boot animation: the chosen file is missing, firmware's own kept"); return@runCatching }
        if (!marker.isFile) {
            if (sys.isFile && !parked.isFile) { parked.parentFile?.mkdirs(); sys.copyTo(parked, overwrite = true) }
            marker.writeText(if (sys.isFile || parked.isFile) "had" else "absent")
        }
        sys.parentFile?.mkdirs(); src.copyTo(sys, overwrite = true)
        local.parentFile?.mkdirs(); src.copyTo(local, overwrite = true)
        log("boot animation: ${if (choice == CUSTOM) "custom" else "AEmulator X"} (${src.length() / 1024} KB)")
    }.onFailure { log("boot animation: ${it.message}") }

    /** AEmulator X's own animation for a [w]×[h] screen, generated once and cached in the VM folder. */
    fun builtIn(paths: VmPaths, w: Int, h: Int): File? {
        val side = (minOf(w, h) * 0.6f).toInt().coerceIn(96, 720)
        val f = File(paths.dir, "bootanimation-x-$side.zip")
        if (f.isFile) return f
        return runCatching { f.writeBytes(generate(side)); f }.getOrNull()
    }

    /**
     * desc.txt + PNG frames, stored uncompressed (old bootanimation builds cannot read deflated entries):
     *   part0, played once: the gradient X fades and zooms in (24 frames);
     *   part1, looped until boot completes: its glow breathes (30 frames).
     */
    internal fun generate(side: Int): ByteArray {
        val fps = 24
        val intro = 24; val loop = 30
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(name: String, bytes: ByteArray) {
                val e = ZipEntry(name).apply {
                    method = ZipEntry.STORED; size = bytes.size.toLong(); compressedSize = bytes.size.toLong()
                    crc = CRC32().apply { update(bytes) }.value
                }
                zip.putNextEntry(e); zip.write(bytes); zip.closeEntry()
            }
            put("desc.txt", "$side $side $fps\np 1 0 part0\np 0 0 part1\n".toByteArray())
            for (i in 0 until intro) put("part0/%03d.png".format(i), frame(side, appear = (i + 1f) / intro, glow = 0.5f))
            for (i in 0 until loop) {
                val glow = 0.5f + 0.5f * kotlin.math.sin(2 * Math.PI * i / loop).toFloat()
                put("part1/%03d.png".format(i), frame(side, appear = 1f, glow = glow))
            }
        }
        return out.toByteArray()
    }

    private fun frame(side: Int, appear: Float, glow: Float): ByteArray {
        val bmp = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.BLACK)
        val ease = 1 - (1 - appear) * (1 - appear)
        val scale = 0.6f + 0.4f * ease
        val alpha = (255 * ease).toInt().coerceIn(0, 255)
        val text = side * 0.62f * scale
        val shader = LinearGradient(0f, side * 0.2f, side.toFloat(), side * 0.8f,
            intArrayOf(0xFF7C4DFF.toInt(), 0xFFE040FB.toInt(), 0xFF18FFFF.toInt()), null, Shader.TileMode.CLAMP)
        val cx = side / 2f
        val baseline = side / 2f + text * 0.36f
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = text; textAlign = Paint.Align.CENTER
            color = 0xFFE040FB.toInt(); this.alpha = (alpha * (0.25f + 0.45f * glow)).toInt()
            maskFilter = BlurMaskFilter(side * 0.06f, BlurMaskFilter.Blur.NORMAL)
        }
        c.drawText("X", cx, baseline, glowPaint)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = text; textAlign = Paint.Align.CENTER
            this.shader = shader; this.alpha = alpha
        }
        c.drawText("X", cx, baseline, p)
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = side * 0.075f; textAlign = Paint.Align.CENTER; color = Color.WHITE; this.alpha = (alpha * 0.8f).toInt()
            letterSpacing = 0.25f
        }
        c.drawText("AEMULATOR", cx, side * 0.9f, small)
        return ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 100, it); bmp.recycle() }.toByteArray()
    }
}
