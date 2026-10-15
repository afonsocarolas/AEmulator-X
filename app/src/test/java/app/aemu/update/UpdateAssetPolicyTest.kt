package app.aemu.update

import org.junit.Assert.*
import org.junit.Test

class UpdateAssetPolicyTest {
    private val plus = "AEmulator-X-0.0.0.2.13-x1-app.aemu.x.apk"

    @Test fun choosesPlusApkRegardlessOfAssetOrder() {
        assertEquals(plus, listOf(plus, "notes.txt").firstOrNull { UpdateAssetPolicy.matches(it, "app.aemu.x") })
        assertEquals(plus, listOf("notes.txt", plus).firstOrNull { UpdateAssetPolicy.matches(it, "app.aemu.x") })
    }
    @Test fun neverMatchesOldSunsetOrCloneApks() {
        assertFalse(UpdateAssetPolicy.matches("AEmulator-Sunset-0.0.0.3-sunset.30-app.aemu.apk", "app.aemu.x"))
        assertFalse(UpdateAssetPolicy.matches("AEmulator-Sunset-0.0.0.3-sunset.30-app.aemu.clone.apk", "app.aemu.x"))
    }
    @Test fun otherPackagesNeverMatch() {
        assertFalse(UpdateAssetPolicy.matches(plus, "app.aemu"))
        assertFalse(UpdateAssetPolicy.matches(plus, "app.aemu.clone"))
        assertFalse(UpdateAssetPolicy.matches(plus, "other.app"))
    }
    @Test fun matchingIsCaseInsensitiveButRequiresExactPackageSuffix() {
        assertTrue(UpdateAssetPolicy.matches(plus.uppercase(), "app.aemu.x"))
        assertFalse(UpdateAssetPolicy.matches("$plus.zip", "app.aemu.x"))
        assertFalse(UpdateAssetPolicy.matches("$plus.asc", "app.aemu.x"))
        assertFalse(UpdateAssetPolicy.matches("prefix-app.aemu.x.fake.apk", "app.aemu.x"))
    }
}
