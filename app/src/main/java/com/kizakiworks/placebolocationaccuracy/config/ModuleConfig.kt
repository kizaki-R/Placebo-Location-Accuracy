package com.kizakiworks.placebolocationaccuracy.config

object ModuleConfig {
    const val MODULE_PACKAGE = "com.kizakiworks.placebolocationaccuracy"
    const val PREFS_NAME = "placebo_location_prefs"
    const val LOG_TAG = "PlaceboLA"

    const val KEY_BLOCK_NETWORK_LOCATION = "block_network_location"
    const val KEY_BLOCK_GMS_WIFI = "block_gms_wifi"
    const val KEY_BLOCK_GMS_CELL = "block_gms_cell"
    const val KEY_BLOCK_GMS_BLE = "block_gms_ble"
    const val KEY_BLOCK_LOCATION_BYPASS = "block_location_bypass"
    const val KEY_VERBOSE_LOGGING = "verbose_logging"

    const val DEFAULT_BLOCK_NETWORK_LOCATION = true
    const val DEFAULT_BLOCK_GMS_WIFI = true
    const val DEFAULT_BLOCK_GMS_CELL = true
    const val DEFAULT_BLOCK_GMS_BLE = true
    const val DEFAULT_BLOCK_LOCATION_BYPASS = true
    const val DEFAULT_VERBOSE_LOGGING = false

    const val PROVIDER_NETWORK = "network"
    const val PROVIDER_GPS = "gps"

    val TARGET_GMS_PACKAGES: Set<String> = setOf(
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.gms.location.history"
    )

    fun isTargetGmsPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (packageName in TARGET_GMS_PACKAGES) return true
        return packageName.startsWith("com.google.android.gms.")
    }

    fun isBlockedProvider(provider: String?): Boolean {
        return provider != null && provider.equals(PROVIDER_NETWORK, ignoreCase = true)
    }

    fun isPureGpsProvider(provider: String?): Boolean {
        return provider != null && provider.equals(PROVIDER_GPS, ignoreCase = true)
    }
}
