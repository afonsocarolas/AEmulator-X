/* Added for AEmulator X, 2026-11-02: Android boot history. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import java.io.File
import java.util.zip.ZipFile

/**
 * The original boot screen of each of your firmwares, oldest Android first. Nothing is bundled with the app: a
 * firmware's own bootanimation.zip is played (the original one, even when AEmulator X shows another at boot), and
 * phones without one get Android's classic "ANDROID" logo, drawn the way Android drew it, from the two pictures in
 * the firmware's framework-res.apk (the logo cut out of black, and a shine sliding behind it).
 */
object BootHistory {
    sealed class Source {
        /** a bootanimation.zip (from the firmware) */
        data class Zip(val file: File) : Source()
        /** Android's built-in logo: the two pictures from framework-res.apk */
        data class Classic(val apk: File) : Source()
        /** a maker's own format (Samsung .qmg and the like) that only their phones can play */
        data class Vendor(val file: File) : Source()
        object None : Source()
    }

    data class Item(val img: GuestImage, val source: Source)

    data class Part(val playOnce: Boolean, val count: Int, val pause: Int, val dir: String)
    data class Desc(val width: Int, val height: Int, val fps: Int, val parts: List<Part>)

    const val MASK = "assets/images/android-logo-mask.png"
    const val SHINE = "assets/images/android-logo-shine.png"

    /** desc.txt: "w h fps", then "p|c count pause dir [colour …]" lines; null when it is not one. */
    fun parseDesc(text: String): Desc? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        val head = lines.firstOrNull()?.split(Regex("\\s+")) ?: return null
        if (head.size < 3) return null
        val w = head[0].toIntOrNull() ?: return null; val h = head[1].toIntOrNull() ?: return null
        val fps = head[2].toIntOrNull()?.coerceIn(1, 60) ?: return null
        val parts = lines.drop(1).mapNotNull { l ->
            val p = l.split(Regex("\\s+"))
            if (p.size < 4 || (p[0] != "p" && p[0] != "c")) null
            else {
                // count 0 = loop until the system has booted; here, until the next firmware is shown
                val count = p[1].toIntOrNull() ?: 0
                Part(playOnce = count > 0, count = count, pause = p[2].toIntOrNull() ?: 0, dir = p[3])
            }
        }
        if (w <= 0 || h <= 0 || parts.isEmpty()) return null
        return Desc(w, h, fps, parts)
    }

    /** The picture entries of each part, in playing order. */
    fun frames(names: List<String>, part: String): List<String> =
        names.filter { it.startsWith("$part/") && Regex("\\.(png|jpe?g)$", RegexOption.IGNORE_CASE).containsMatchIn(it) }.sorted()

    /** Where a firmware's original boot screen is, looking past the one AEmulator X may have put in place. */
    fun sourceOf(paths: VmPaths): Source {
        val root = paths.root
        val applied = File(paths.dir, "bootanimation-applied").isFile
        val parked = File(root, "system/.aemu-parked/system#media#bootanimation.zip.x-orig")
        val zips = buildList {
            if (applied) { if (parked.isFile) add(parked) }
            else add(File(root, "system/media/bootanimation.zip"))
            add(File(root, "oem/media/bootanimation.zip")); add(File(root, "product/media/bootanimation.zip"))
        }
        zips.firstOrNull { f -> f.isFile && runCatching { ZipFile(f).use { it.getEntry("desc.txt") != null } }.getOrDefault(false) }?.let { return Source.Zip(it) }
        File(root, "system/media").listFiles()?.firstOrNull { it.extension.equals("qmg", true) && it.name.contains("boot", true) }
            ?.let { return Source.Vendor(it) }
        val fw = File(root, "system/framework/framework-res.apk")
        if (fw.isFile && runCatching { ZipFile(fw).use { it.getEntry(MASK) != null && it.getEntry(SHINE) != null } }.getOrDefault(false))
            return Source.Classic(fw)
        return Source.None
    }

    /** Your firmwares (not containers: they share their base's /system), oldest Android first. Blocking. */
    fun scan(ctx: Context): List<Item> =
        runCatching { ImageStore.list(ctx) }.getOrDefault(emptyList()).filter { it.baseId.isEmpty() }.sortedWith(compareBy({ it.api }, { it.name }))
            .map { Item(it, runCatching { sourceOf(VmPaths(ctx, it.id)) }.getOrDefault(Source.None)) }

    /** Era note shown under each one: an index into the x_bh_era array. */
    fun era(api: Int): Int = when {
        api <= 10 -> 0      // 1.0–2.3
        api <= 13 -> 1      // Honeycomb
        api <= 18 -> 2      // Ice Cream Sandwich, Jelly Bean
        api <= 20 -> 3      // KitKat
        api <= 25 -> 4      // Lollipop to Nougat
        api <= 28 -> 5      // Oreo, Pie
        else -> 6           // 10 and newer
    }
}
