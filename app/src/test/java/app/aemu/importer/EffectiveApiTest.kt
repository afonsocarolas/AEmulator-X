package app.aemu.importer

import org.junit.Assert.assertEquals
import org.junit.Test

class EffectiveApiTest {
    @Test fun previewBuildReportingPreviousSdkIsTreatedAsNougat() {
        // Wear 2.0 developer preview "bass NVD36F": sdk=23, codename=N, release=N
        assertEquals(24, Analyzer.effectiveApi(23, "N", "N"))
    }

    @Test fun nougatMr1PreviewIsApi25() = assertEquals(25, Analyzer.effectiveApi(24, "NMR1", "NMR1"))

    @Test fun releaseBuildsKeepTheirSdk() {
        assertEquals(23, Analyzer.effectiveApi(23, "REL", "6.0.1"))
        assertEquals(24, Analyzer.effectiveApi(24, "REL", "7.0"))
        assertEquals(19, Analyzer.effectiveApi(19, null, "4.4.2"))
    }

    @Test fun finalNougatIsNotLowered() = assertEquals(24, Analyzer.effectiveApi(24, "N", "7.0"))

    @Test fun missingSdkFallsBackToCodenameOrRelease() {
        assertEquals(24, Analyzer.effectiveApi(null, "N", null))
        assertEquals(24, Analyzer.effectiveApi(null, null, "N"))
        assertEquals(10, Analyzer.effectiveApi(null, null, "2.3.4"))
    }

    // AEmulator X: 1.0–2.1 used to be guessed as KitKat (19)
    @Test fun oldReleasesWithoutSdkAreRecognised() {
        assertEquals(1, Analyzer.effectiveApi(null, null, "1.0"))
        assertEquals(2, Analyzer.effectiveApi(null, null, "1.1"))
        assertEquals(3, Analyzer.effectiveApi(null, null, "1.5"))
        assertEquals(4, Analyzer.effectiveApi(null, null, "1.6"))
        assertEquals(5, Analyzer.effectiveApi(null, null, "2.0"))
        assertEquals(6, Analyzer.effectiveApi(null, null, "2.0.1"))
        assertEquals(7, Analyzer.effectiveApi(null, null, "2.1-update1"))
        assertEquals(8, Analyzer.effectiveApi(null, null, "2.2.1"))
        assertEquals(7, Analyzer.effectiveApi(7, null, "2.1"))
    }
}
