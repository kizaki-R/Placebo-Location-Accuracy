package com.kizakiworks.placebolocationaccuracy.xposed

import android.util.Log
import io.github.libxposed.api.XposedInterface
import com.kizakiworks.placebolocationaccuracy.config.ModuleConfig

object HookLogger {
    @Volatile
    private var xposed: XposedInterface? = null

    fun init(module: XposedInterface) {
        xposed = module
    }

    fun info(message: String) {
        Log.i(ModuleConfig.LOG_TAG, message)
        runCatching { xposed?.log(Log.INFO, ModuleConfig.LOG_TAG, message) }
    }

    fun debug(message: String) {
        if (ConfigReader.verboseLogging()) {
            Log.d(ModuleConfig.LOG_TAG, message)
        }
    }

    fun warn(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(ModuleConfig.LOG_TAG, message, throwable)
            runCatching { xposed?.log(Log.WARN, ModuleConfig.LOG_TAG, message, throwable) }
        } else {
            Log.w(ModuleConfig.LOG_TAG, message)
            runCatching { xposed?.log(Log.WARN, ModuleConfig.LOG_TAG, message) }
        }
    }
}
