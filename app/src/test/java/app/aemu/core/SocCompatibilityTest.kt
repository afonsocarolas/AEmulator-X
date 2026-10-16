package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class SocCompatibilityTest {
    @Test fun recognizesNamedFamiliesAndQualcommPartNumbers() {
        assertEquals(SocCompatibility.Family.SNAPDRAGON, SocCompatibility.family("SM8450", ""))
        assertEquals(SocCompatibility.Family.SNAPDRAGON, SocCompatibility.family("Snapdragon 8 Gen 3", ""))
        assertEquals(SocCompatibility.Family.SNAPDRAGON, SocCompatibility.family("", "qcom"))
        assertEquals(SocCompatibility.Family.DIMENSITY, SocCompatibility.family("MediaTek Dimensity 9200", ""))
        assertEquals(SocCompatibility.Family.DIMENSITY, SocCompatibility.family("MT6985", ""))
        assertEquals(SocCompatibility.Family.DIMENSITY, SocCompatibility.family("", "mt6833"))
        assertEquals(SocCompatibility.Family.TENSOR, SocCompatibility.family("Google Tensor G4", ""))
        assertEquals(SocCompatibility.Family.TENSOR, SocCompatibility.family("", "gs101"))
    }
    // AEmulator X: Helio G81 Ultra (MT6769), as SOC_MODEL on Android 12+ or Build.HARDWARE before that
    @Test fun recognizesHelioG81UltraPlatform() {
        assertEquals(SocCompatibility.Family.HELIO, SocCompatibility.family("MT6769Z", ""))
        assertEquals(SocCompatibility.Family.HELIO, SocCompatibility.family("MT6769", "mt6769"))
        assertEquals(SocCompatibility.Family.HELIO, SocCompatibility.family("", "mt6769"))
        assertEquals(SocCompatibility.Family.HELIO, SocCompatibility.family("MediaTek Helio G81 Ultra", ""))
        assertEquals(SocCompatibility.Family.HELIO, SocCompatibility.family("Helio G85", ""))
    }
    // AEmulator X: Kirin 980 (Huawei P30 Pro); EMUI on Android 9/10 has no SOC_MODEL, only Build.HARDWARE
    @Test fun recognizesKirin980() {
        assertEquals(SocCompatibility.Family.KIRIN, SocCompatibility.family("", "kirin980"))
        assertEquals(SocCompatibility.Family.KIRIN, SocCompatibility.family("Kirin 980", ""))
        assertEquals(SocCompatibility.Family.KIRIN, SocCompatibility.family("HiSilicon Kirin980", ""))
        assertEquals(SocCompatibility.Family.KIRIN, SocCompatibility.family("hi3680", ""))
    }
    @Test fun unknownOrOtherMediaTekIsNotSilentlyWhitelisted() {
        for (model in listOf("", "unknown", "Exynos 2400", "Unisoc T610", "Helio G99", "MT6789", "MT9999", "kirin970", "Kirin 990"))
            assertEquals(SocCompatibility.Family.UNVERIFIED, SocCompatibility.family(model, ""))
    }
}
