/* Added for AEmulator X, 2026-10-21: Device museum. GPL-3.0; see LICENSE and NOTICE.md. */
package app.aemu.core

/**
 * Famous Android devices, oldest first. Drawn in code (no photos): [ratio] is height / width of the body, the other
 * fields say what to draw. The fun fact of each is the same index in the x_museum_facts string array.
 */
object DeviceMuseum {
    enum class Nav { KEYS, CAPACITIVE, HOME_BUTTON, ON_SCREEN }

    data class Device(
        val name: String, val maker: String, val year: Int, val android: String, val api: Int,
        val color: Long, val ratio: Float, val nav: Nav,
        val keyboard: Boolean = false, val trackball: Boolean = false, val tablet: Boolean = false,
    )

    val ALL = listOf(
        Device("T-Mobile G1", "HTC", 2008, "1.0", 1, 0xFF2B2B2B, 1.5f, Nav.KEYS, keyboard = true, trackball = true),
        Device("HTC Magic", "HTC", 2009, "1.5 Cupcake", 3, 0xFFEDEDED, 1.68f, Nav.KEYS, trackball = true),
        Device("Motorola Droid", "Motorola", 2009, "2.0 Eclair", 5, 0xFF3A3A3A, 1.55f, Nav.CAPACITIVE, keyboard = true),
        Device("Nexus One", "HTC", 2010, "2.1 Eclair", 7, 0xFF5D4E3C, 1.8f, Nav.CAPACITIVE, trackball = true),
        Device("Galaxy S", "Samsung", 2010, "2.1 Eclair", 7, 0xFF111111, 1.83f, Nav.HOME_BUTTON),
        Device("Nexus S", "Samsung", 2010, "2.3 Gingerbread", 9, 0xFF141414, 1.9f, Nav.CAPACITIVE),
        Device("Motorola Xoom", "Motorola", 2011, "3.0 Honeycomb", 11, 0xFF1F1F1F, 0.66f, Nav.ON_SCREEN, tablet = true),
        Device("Galaxy S II", "Samsung", 2011, "2.3 Gingerbread", 10, 0xFF0D0D0D, 1.82f, Nav.HOME_BUTTON),
        Device("Galaxy Nexus", "Samsung", 2011, "4.0 Ice Cream Sandwich", 14, 0xFF181818, 1.85f, Nav.ON_SCREEN),
        Device("Nexus 7", "Asus", 2012, "4.1 Jelly Bean", 16, 0xFF242424, 1.6f, Nav.ON_SCREEN, tablet = true),
        Device("Nexus 5", "LG", 2013, "4.4 KitKat", 19, 0xFFE53935, 1.98f, Nav.ON_SCREEN),
        Device("Nexus 6", "Motorola", 2014, "5.0 Lollipop", 21, 0xFF263D5A, 1.9f, Nav.ON_SCREEN),
        Device("Nexus 5X", "LG", 2015, "6.0 Marshmallow", 23, 0xFF9FD6D2, 2.0f, Nav.ON_SCREEN),
        Device("Pixel", "Google", 2016, "7.1 Nougat", 25, 0xFF1565C0, 2.0f, Nav.ON_SCREEN),
        Device("Pixel 3", "Google", 2018, "9 Pie", 28, 0xFFF8BBD0, 2.05f, Nav.ON_SCREEN),
    )

    /** Your firmwares that run the same Android version family as [d]. */
    fun matches(d: Device, apis: Collection<Int>): Boolean = apis.any { DailyPick.factGroup(it) == DailyPick.factGroup(d.api) }
}
