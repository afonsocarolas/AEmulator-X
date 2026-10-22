package app.aemu.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class X30WallpaperTest {
    @Test fun whatCountsAsAWallpaper() {
        assertTrue(FirmwareWallpapers.isWallpaper("res/drawable-nodpi/default_wallpaper.jpg", 200_000))
        assertTrue(FirmwareWallpapers.isWallpaper("res/drawable-hdpi/wallpaper_galaxy.jpg", 150_000))
        assertTrue(FirmwareWallpapers.isWallpaper("system/media/wallpapers/sky.png", 300_000))          // in a wallpapers folder
        assertTrue(FirmwareWallpapers.isWallpaper("res/drawable/magic_wallpaper.webp", 90_000))
        assertFalse(FirmwareWallpapers.isWallpaper("res/drawable-hdpi/wallpaper_galaxy_small.jpg", 150_000))   // a thumbnail
        assertFalse(FirmwareWallpapers.isWallpaper("res/drawable-hdpi/ic_wallpaper.png", 150_000))             // an icon
        assertFalse(FirmwareWallpapers.isWallpaper("res/drawable-nodpi/default_wallpaper.jpg", 5_000))         // too small
        assertFalse(FirmwareWallpapers.isWallpaper("res/raw/wallpaper.xml", 200_000))                          // not a picture
        assertFalse(FirmwareWallpapers.isWallpaper("system/media/audio/ringtone.png", 200_000))                // not a wallpaper
    }

    @Test fun onePerNameBiggestWins() {
        val apk = File("Launcher2.apk")
        fun w(name: String, size: Long) = FirmwareWallpapers.Wallpaper("id", "fw", 10, name, size, apk = apk, entry = "res/x/$name")
        val list = FirmwareWallpapers.biggestPerName(listOf(w("Wallpaper galaxy.jpg", 100_000), w("Wallpaper galaxy.jpg", 300_000), w("Default wallpaper.jpg", 50_000)))
        assertEquals(2, list.size)
        assertEquals(300_000L, list.first { it.name.startsWith("Wallpaper") }.size)
        assertEquals("Default wallpaper", FirmwareWallpapers.pretty("res/drawable-nodpi/default_wallpaper.jpg"))
    }
}
