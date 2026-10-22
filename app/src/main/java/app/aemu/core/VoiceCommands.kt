/* Added for AEmulator X, 2026-10-20: voice commands. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

import java.text.Normalizer

/** What a spoken phrase asks for, in English, Russian, Portuguese, Spanish, French, German or Polish. */
object VoiceCommands {
    enum class Action { SCREENSHOT, HOME, BACK, RECENTS, MENU, VOLUME_UP, VOLUME_DOWN, POWER, REPLAY, GIF }

    // checked in this order: "volume up" must win over "up", "go back" over "go home" spelled alike, etc.
    private val WORDS: List<Pair<Action, List<String>>> = listOf(
        Action.SCREENSHOT to listOf("capture d ecran", "bildschirmfoto", "zrzut", "zrzut ekranu", "screenshot", "screen shot", "take a picture", "capture", "print screen",
            "скриншот", "снимок", "captura", "print", "foto da tela", "tirar foto", "haz una foto", "pantallazo"),
        Action.REPLAY to listOf("rejouer", "revoir", "wiederholung", "powtorka", "replay", "instant replay", "rewind", "повтор", "перемотка", "repetir", "repeticao", "repeticion", "rebobinar", "voltar o tempo"),
        Action.GIF to listOf("gif", "гиф", "гифка"),
        Action.VOLUME_UP to listOf("monte le son", "plus fort", "volume plus", "lauter", "glosniej", "głośniej", "volume up", "louder", "громче", "aumentar volume", "aumentar o volume", "mais alto", "sube el volumen", "subir volumen", "mas alto"),
        Action.VOLUME_DOWN to listOf("baisse le son", "moins fort", "volume moins", "leiser", "ciszej", "volume down", "quieter", "softer", "тише", "diminuir volume", "diminuir o volume", "mais baixo", "baja el volumen", "bajar volumen", "mas bajo"),
        Action.RECENTS to listOf("recents", "applications recentes", "letzte apps", "zuletzt", "ostatnie", "recent", "recents", "apps", "multitask", "недавние", "последние", "recentes", "aplicativos", "recientes", "aplicaciones"),
        Action.BACK to listOf("retour", "zuruck", "zurück", "wstecz", "cofnij", "back", "go back", "назад", "voltar", "atras", "volver", "regresar"),
        Action.HOME to listOf("accueil", "startseite", "startbildschirm", "ekran glowny", "ekran główny", "pulpit", "home", "go home", "домой", "главный экран", "домашний", "inicio", "tela inicial", "casa", "pantalla de inicio"),
        Action.MENU to listOf("menü", "menu", "меню"),
        Action.POWER to listOf("verrouiller", "eteindre l ecran", "sperren", "bildschirm aus", "zablokuj", "power", "lock", "sleep", "питание", "заблокировать", "блокировка", "desligar tela", "bloquear", "apagar pantalla", "energia"),
    )

    /** Lower case, no accents, one space between words. */
    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^\\p{L}\\p{N} ]+"), " ").replace(Regex("\\s+"), " ").trim()

    /** The first action found in any of the recognizer's guesses (best first); null when none matches. */
    fun match(guesses: List<String>): Action? {
        for (g in guesses) {
            val text = " " + normalize(g) + " "
            for ((action, words) in WORDS) for (w in words) {
                val n = normalize(w)
                if (n.isNotEmpty() && text.contains(" $n ")) return action
            }
        }
        return null
    }
}
