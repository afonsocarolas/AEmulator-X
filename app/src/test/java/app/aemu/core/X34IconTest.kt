package app.aemu.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * X34: the Mono icon style was once named ic_launcher_mono, the same name as the main icon's monochrome layer, so
 * the main icon pointed at an adaptive icon that pointed at itself and launchers showed Android's default icon.
 */
class X34IconTest {
    private val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }

    @Test fun adaptiveIconsDoNotShareNamesWithTheirLayers() {
        val adaptive = File(res, "mipmap-anydpi-v26").listFiles()!!.filter { it.extension == "xml" }.map { it.nameWithoutExtension }.toSet()
        val bitmaps = res.listFiles()!!.filter { it.name.startsWith("mipmap-") && !it.name.contains("anydpi") }
            .flatMap { d -> d.listFiles()!!.map { it.nameWithoutExtension } }.toSet()
        assertTrue("same name as a bitmap: ${adaptive intersect bitmaps}", (adaptive intersect bitmaps).isEmpty())
        for (f in File(res, "mipmap-anydpi-v26").listFiles()!!) {
            val refs = Regex("@mipmap/(\\w+)").findAll(f.readText()).map { it.groupValues[1] }.toList()
            assertFalse("${f.name} points at itself", f.nameWithoutExtension in refs)
            assertTrue("${f.name} points at another adaptive icon: $refs", refs.none { it in adaptive })
            assertTrue("${f.name} points at a missing picture: $refs", refs.all { it in bitmaps })
        }
    }

    @Test fun everyLauncherEntryHasAnIcon() {
        val manifest = File(res.parentFile, "AndroidManifest.xml").readText()
        val icons = Regex("""android:icon="@mipmap/(\w+)"""").findAll(manifest).map { it.groupValues[1] }.toSet()
        val adaptive = File(res, "mipmap-anydpi-v26").listFiles()!!.map { it.nameWithoutExtension }.toSet()
        assertTrue(icons.isNotEmpty())
        assertTrue("missing: ${icons - adaptive}", (icons - adaptive).isEmpty())
    }
}
