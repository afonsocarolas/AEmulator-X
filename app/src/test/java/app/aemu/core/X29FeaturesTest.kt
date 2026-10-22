package app.aemu.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class X29FeaturesTest {
    @Test fun bootLogoBasics() {
        assertEquals("480 576 24\np 1 0 part0\np 0 0 part1\n", BootLogoMaker.desc(480, 576))
        assertEquals(480 to 576, BootLogoMaker.size(480, 800))
        assertEquals(720 to 864, BootLogoMaker.size(1440, 2560))                 // never huge
        assertEquals(160 to 192, BootLogoMaker.size(100, 100))                   // never tiny
        assertEquals(0, BootLogoMaker.typed("Chris", 0f))
        assertEquals(5, BootLogoMaker.typed("Chris", 0.85f))
        assertEquals(5, BootLogoMaker.typed("Chris", 1f))
        assertEquals(1f, BootLogoMaker.ease(1f), 1e-6f); assertEquals(0f, BootLogoMaker.ease(0f), 1e-6f)
        assertTrue(BootLogoMaker.PALETTES.all { it.size == 3 })
    }

    @Test fun voiceInNewLanguages() {
        assertEquals(VoiceCommands.Action.HOME, VoiceCommands.match(listOf("accueil")))
        assertEquals(VoiceCommands.Action.BACK, VoiceCommands.match(listOf("Zurück bitte")))
        assertEquals(VoiceCommands.Action.SCREENSHOT, VoiceCommands.match(listOf("zrób zrzut ekranu")))
        assertEquals(VoiceCommands.Action.HOME, VoiceCommands.match(listOf("ekran główny")))
        assertEquals(VoiceCommands.Action.VOLUME_UP, VoiceCommands.match(listOf("lauter")))
        assertEquals(VoiceCommands.Action.SCREENSHOT, VoiceCommands.match(listOf("צילום מסך")))       // Hebrew
        assertEquals(VoiceCommands.Action.HOME, VoiceCommands.match(listOf("מסך הבית")))
        assertEquals(VoiceCommands.Action.BACK, VoiceCommands.match(listOf("חזרה")))
    }

    /** Every language has every AEmulator X text (a missing one would quietly show up in English). */
    @Test fun translationsComplete() {
        val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        fun keys(dir: String) = Regex("<string name=\"([^\"]+)\"").findAll(File(res, "$dir/strings_x.xml").readText()).map { it.groupValues[1] }.toSet()
        val en = keys("values")
        for (lang in listOf("ru", "pt-rBR", "es", "pl", "fr", "de", "iw")) {
            val missing = en - keys("values-$lang")
            assertTrue("$lang is missing $missing", missing.isEmpty())
        }
    }
}
