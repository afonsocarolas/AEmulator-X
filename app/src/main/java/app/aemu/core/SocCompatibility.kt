/* AEmulator Sunset addition, 2026-10-04. GPL-3.0; see LICENSE.
 * Modified for AEmulator X, 2026-10-15: Helio G81 Ultra (MT6769 platform); 2026-10-16: Kirin 980 (Hi3680). */
package app.aemu.core

import java.util.Locale

/** Advisory family recognition, not a claim that every chip/ROM in a family boots. */
internal object SocCompatibility {
    enum class Family { SNAPDRAGON, DIMENSITY, TENSOR, HELIO, KIRIN, UNVERIFIED }
    /**
     * AEmulator X: only the MT6769 platform (Helio G81 / G81 Ultra and its siblings G70/G80/G85/G88: 2x Cortex-A75 +
     * 6x A55, Mali-G52 MC2). Its Mali GPU takes the existing Mali paths (libglserverd_kk_mali, no GLES3, no hwui PBOs).
     * Other Helio chips stay unverified.
     */
    private val helioPlatforms = setOf("6769")
    // Conservative, finite mobile platform allowlist; never accept every MediaTek chip.
    private val dimensityPlatforms = setOf("6833", "6835", "6853", "6855", "6858", "6873", "6877", "6878",
        "6879", "6881", "6883", "6885", "6886", "6889", "6891", "6893", "6895", "6896", "6897", "6899",
        "6983", "6985", "6989", "6991", "6993")
    fun family(model: String, hardware: String): Family {
        val chip = "$model $hardware".lowercase(Locale.ROOT)
        return when {
            Regex("\\bsnapdragon\\b|\\b(sm|sdm|msm|apq)\\d{4}\\w*\\b|\\bqcom\\b").containsMatchIn(chip) -> Family.SNAPDRAGON
            Regex("\\bdimensity\\b").containsMatchIn(chip) ||
                Regex("\\bmt(\\d{4})[a-z0-9]*\\b").findAll(chip).any { it.groupValues[1] in dimensityPlatforms } -> Family.DIMENSITY
            Regex("\\btensor\\b|\\bgs(101|201)\\b").containsMatchIn(chip) -> Family.TENSOR
            Regex("\\bhelio\\s*g(70|80|81|85|88)\\b").containsMatchIn(chip) ||
                Regex("\\bmt(\\d{4})[a-z0-9]*\\b").findAll(chip).any { it.groupValues[1] in helioPlatforms } -> Family.HELIO
            // AEmulator X: HiSilicon Kirin 980 (Huawei P30 / P30 Pro, Mate 20 series; Hi3680: 2x A76 + 2x A76 + 4x A55,
            // Mali-G76 MP10 on the existing Mali paths). Before Android 12 there is no SOC_MODEL, only
            // Build.HARDWARE = "kirin980". Other Kirin chips stay unverified.
            Regex("\\bkirin\\s*980\\b|\\bhi3680\\b").containsMatchIn(chip) -> Family.KIRIN
            else -> Family.UNVERIFIED
        }
    }
}
