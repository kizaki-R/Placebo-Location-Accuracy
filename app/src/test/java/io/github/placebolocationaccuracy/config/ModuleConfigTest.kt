package io.github.placebolocationaccuracy.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleConfigTest {

    @Test
    fun targetGmsPackagesAreMatched() {
        assertTrue(ModuleConfig.isTargetGmsPackage("com.google.android.gms"))
        assertTrue(ModuleConfig.isTargetGmsPackage("com.google.android.gsf"))
        assertTrue(ModuleConfig.isTargetGmsPackage("com.google.android.gms.persistent"))
    }

    @Test
    fun nonGmsPackagesAreNotMatched() {
        assertFalse(ModuleConfig.isTargetGmsPackage("com.android.settings"))
        assertFalse(ModuleConfig.isTargetGmsPackage("com.google.android.apps.maps"))
        assertFalse(ModuleConfig.isTargetGmsPackage(""))
        assertFalse(ModuleConfig.isTargetGmsPackage(null))
    }

    @Test
    fun networkProviderIsBlockedWhilePureGpsRemainsAllowed() {
        assertTrue(ModuleConfig.isBlockedProvider("network"))
        assertTrue(ModuleConfig.isBlockedProvider("NETWORK"))
        assertFalse(ModuleConfig.isBlockedProvider("gps"))
        assertFalse(ModuleConfig.isBlockedProvider("passive"))
        assertFalse(ModuleConfig.isBlockedProvider(null))

        assertTrue(ModuleConfig.isPureGpsProvider("gps"))
        assertFalse(ModuleConfig.isPureGpsProvider("network"))
    }

    @Test
    fun defaultsProtectLocationWithoutVerboseSpam() {
        assertTrue(ModuleConfig.DEFAULT_BLOCK_NETWORK_LOCATION)
        assertTrue(ModuleConfig.DEFAULT_BLOCK_GMS_WIFI)
        assertTrue(ModuleConfig.DEFAULT_BLOCK_GMS_CELL)
        assertTrue(ModuleConfig.DEFAULT_BLOCK_GMS_BLE)
        assertTrue(ModuleConfig.DEFAULT_BLOCK_LOCATION_BYPASS)
        assertFalse(ModuleConfig.DEFAULT_VERBOSE_LOGGING)
    }
}
