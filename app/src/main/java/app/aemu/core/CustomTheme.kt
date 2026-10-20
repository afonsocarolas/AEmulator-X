/* Added for AEmulator X, 2026-10-20: the theme maker's own colour style. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

/**
 * A colour style made by the user: the main colour of the whole app ([seed]) and the three brand gradient colours.
 * It is shared as a short text code, e.g. `AEX-THEME:Night Sky:3949AB:1A237E:7C4DFF:00E5FF`, so friends can paste it.
 * Colours are RGB without alpha (always opaque).
 */
data class CustomTheme(val name: String, val seed: Int, val a: Int, val b: Int, val c: Int) {
    fun code(): String = listOf(PREFIX, clean(name), hex(seed), hex(a), hex(b), hex(c)).joinToString(":")

    companion object {
        const val ID = "custom"
        const val PREFIX = "AEX-THEME"
        val DEFAULT = CustomTheme("My theme", 0xFF6A3DF0.toInt(), 0xFF7C4DFF.toInt(), 0xFFE040FB.toInt(), 0xFF18FFFF.toInt())

        private fun hex(c: Int) = "%06X".format(c and 0xFFFFFF)
        private fun clean(n: String) = n.replace(":", " ").replace("\n", " ").trim().take(30).ifEmpty { "My theme" }

        /** Reads a code (also when pasted with text around it); null when there is none. */
        fun parse(text: String?): CustomTheme? {
            if (text == null) return null
            val at = text.indexOf(PREFIX)
            if (at < 0) return null
            val parts = text.substring(at).lineSequence().first().trim().split(":")
            if (parts.size < 6) return null
            val cols = parts.subList(parts.size - 4, parts.size).map { p ->
                val h = p.trim().removePrefix("#")
                if (h.length != 6) return null
                (h.toLongOrNull(16) ?: return null).toInt() or 0xFF000000.toInt()
            }
            val name = clean(parts.subList(1, parts.size - 4).joinToString(" "))
            return CustomTheme(name, cols[0], cols[1], cols[2], cols[3])
        }
    }
}
