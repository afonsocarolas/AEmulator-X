/* Added for AEmulator X, 2026-10-15: boot sound picker (X2). GPL-3.0; see LICENSE. */
package app.aemu.core

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class BootSoundTest {
    @Test fun builtInSoundsAreShortAudibleAndEndQuietly() {
        for (name in BootSound.BUILTIN) {
            val pcm = BootSound.synth(name)
            assertTrue(name, pcm.size in 44100 / 2..44100 * 4)
            assertTrue(name, pcm.maxOf { kotlin.math.abs(it.toInt()) } > 8000)
            assertTrue(name, kotlin.math.abs(pcm.last().toInt()) < 200)
        }
    }

    @Test fun choicesThatReplaceTheFirmwareSound() {
        assertFalse(BootSound.replacesFirmware(BootSound.FIRMWARE))
        assertTrue(BootSound.replacesFirmware(BootSound.NONE))
        assertTrue(BootSound.replacesFirmware(BootSound.CHIME))
        assertTrue(BootSound.replacesFirmware("fw:/system/etc/PowerOn.wav"))
        assertEquals("/system/etc/PowerOn.wav", BootSound.fwPath("fw:/system/etc/PowerOn.wav"))
        assertEquals("my.ogg", BootSound.fileName("file:my.ogg"))
    }

    @Test fun firmwareSoundsListBootSoundsFirstThenUiSounds() {
        val root = Files.createTempDirectory("bs").toFile()
        try {
            fun put(p: String) = java.io.File(root, p).apply { parentFile.mkdirs(); writeBytes(byteArrayOf(1, 2, 3)) }
            put("system/media/audio/ui/Lock.ogg")
            put("system/media/audio/notifications/Ding.ogg")
            put("system/etc/PowerOn.wav")
            put("system/etc/hosts")                         // not audio
            put("system/media/audio/ui/readme.txt")         // not audio
            java.io.File(root, "system/media/audio/ui/Empty.ogg").writeBytes(ByteArray(0)) // empty
            assertEquals(listOf("/system/etc/PowerOn.wav", "/system/media/audio/ui/Lock.ogg", "/system/media/audio/notifications/Ding.ogg"),
                BootSound.firmwareSounds(root))
        } finally { root.deleteRecursively() }
    }
}
