/* Added for AEmulator X, 2026-10-22: classic wallpaper gallery. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import android.content.Context
import java.io.File
import java.util.zip.ZipFile

/**
 * The wallpapers that came with your own firmwares: the default one in framework-res.apk, the ones the launcher or
 * wallpaper picker offers, and loose ones some makers keep in /system (media/wallpaper folders). Nothing is bundled
 * with the app; the pictures are read from the firmware you imported.
 */
object FirmwareWallpapers {
    /** One picture: inside [apk] at [entry], or the loose [file]. */
    data class Wallpaper(val firmwareId: String, val firmware: String, val api: Int, val name: String, val size: Long,
                         val apk: File? = null, val entry: String? = null, val file: File? = null) {
        val key: String get() = "$firmwareId|${apk?.path ?: file?.path}|$entry"
        fun bytes(): ByteArray? = runCatching {
            if (apk != null && entry != null) ZipFile(apk).use { z -> z.getInputStream(z.getEntry(entry)).use { it.readBytes() } }
            else file?.readBytes()
        }.getOrNull()
    }

    const val MIN_BYTES = 40_000L
    private val PICTURE = Regex("""\.(jpe?g|png|webp)$""", RegexOption.IGNORE_CASE)
    private val SMALL = Regex("""(_small|thumb|preview|_s\.|_icon|^ic_)""", RegexOption.IGNORE_CASE)

    /** Is this file (an apk entry or a loose file) a full-size wallpaper? */
    fun isWallpaper(path: String, size: Long): Boolean {
        if (size < MIN_BYTES || !PICTURE.containsMatchIn(path)) return false
        val base = path.substringAfterLast('/')
        if (SMALL.containsMatchIn(base)) return false
        val dir = path.substringBeforeLast('/', "").lowercase()
        return base.lowercase().contains("wallpaper") || dir.endsWith("wallpaper") || dir.endsWith("wallpapers") ||
            dir.contains("/wallpaper/") || dir.contains("/wallpapers/")
    }

    /** One picture per name: the same wallpaper comes in several screen densities, keep the biggest. */
    fun biggestPerName(list: List<Wallpaper>): List<Wallpaper> =
        list.groupBy { (it.apk?.path ?: "") + "|" + it.name.substringBeforeLast('.').lowercase() }.values.map { g -> g.maxBy { it.size } }

    /** Readable name: "default_wallpaper.jpg" → "Default wallpaper". */
    fun pretty(file: String): String = file.substringAfterLast('/').substringBeforeLast('.').replace('_', ' ').replace('-', ' ')
        .trim().replaceFirstChar { it.uppercase() }

    /** Everything found in one firmware. Blocking (opens a few apks). */
    fun scan(img: GuestImage, root: File): List<Wallpaper> {
        val found = ArrayList<Wallpaper>()
        val sys = File(root, "system")
        val apks = ArrayList<File>()
        File(sys, "framework/framework-res.apk").takeIf { it.isFile }?.let(apks::add)
        for (d in listOf("app", "priv-app")) File(sys, d).walkTopDown().maxDepth(2)
            .filter { it.isFile && it.name.endsWith(".apk", true) && Regex("launcher|wallpaper|theme", RegexOption.IGNORE_CASE).containsMatchIn(it.path) }
            .forEach(apks::add)
        for (apk in apks) runCatching {
            ZipFile(apk).use { z ->
                for (e in z.entries()) if (!e.isDirectory && e.name.startsWith("res/") && isWallpaper(e.name, e.size))
                    found += Wallpaper(img.id, img.name, img.api, pretty(e.name) + "." + e.name.substringAfterLast('.'), e.size, apk = apk, entry = e.name)
            }
        }
        for (d in listOf("media", "wallpaper", "wallpapers")) File(sys, d).takeIf { it.isDirectory }?.walkTopDown()?.maxDepth(4)
            ?.filter { it.isFile && isWallpaper(it.path.replace('\\', '/'), it.length()) }
            ?.forEach { found += Wallpaper(img.id, img.name, img.api, pretty(it.name) + "." + it.extension, it.length(), file = it) }
        return biggestPerName(found).sortedByDescending { it.name.lowercase().startsWith("default") }
    }

    /** All your firmwares (not containers: they share their base's /system), oldest Android first. */
    fun scanAll(ctx: Context): List<Wallpaper> =
        runCatching { ImageStore.list(ctx) }.getOrDefault(emptyList()).filter { it.baseId.isEmpty() }.sortedBy { it.api }
            .flatMap { img -> runCatching { scan(img, VmPaths(ctx, img.id).root) }.getOrDefault(emptyList()) }
}
