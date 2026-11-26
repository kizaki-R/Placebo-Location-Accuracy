package com.kizakiworks.placebolocationaccuracy.xposed

import android.content.SharedPreferences
import io.github.libxposed.api.XposedInterface
import com.kizakiworks.placebolocationaccuracy.config.ModuleConfig

object ConfigReader {

    @Volatile
    private var remotePrefs: SharedPreferences? = null

    fun init(module: XposedInterface) {
        remotePrefs = runCatching {
            module.getRemotePreferences(ModuleConfig.PREFS_NAME)
        }.onFailure {
            HookLogger.warn("RemotePreferences unavailable, using safe defaults", it)
        }.getOrNull()
    }

    private fun readBool(key: String, defaultValue: Boolean): Boolean {
        val prefs = remotePrefs ?: return defaultValue
        return runCatching { prefs.getBoolean(key, defaultValue) }.getOrDefault(defaultValue)
    }

    fun blockNetworkLocation(): Boolean =
        readBool(ModuleConfig.KEY_BLOCK_NETWORK_LOCATION, ModuleConfig.DEFAULT_BLOCK_NETWORK_LOCATION)

    fun blockGmsWifi(): Boolean =
        readBool(ModuleConfig.KEY_BLOCK_GMS_WIFI, ModuleConfig.DEFAULT_BLOCK_GMS_WIFI)

    fun blockGmsCell(): Boolean =
        readBool(ModuleConfig.KEY_BLOCK_GMS_CELL, ModuleConfig.DEFAULT_BLOCK_GMS_CELL)

    fun blockGmsBle(): Boolean =
        readBool(ModuleConfig.KEY_BLOCK_GMS_BLE, ModuleConfig.DEFAULT_BLOCK_GMS_BLE)

    fun blockLocationBypass(): Boolean =
        readBool(ModuleConfig.KEY_BLOCK_LOCATION_BYPASS, ModuleConfig.DEFAULT_BLOCK_LOCATION_BYPASS)

    fun verboseLogging(): Boolean =
        readBool(ModuleConfig.KEY_VERBOSE_LOGGING, ModuleConfig.DEFAULT_VERBOSE_LOGGING)
}
