package io.github.placebolocationaccuracy.xposed.hooks

import android.os.Build
import io.github.libxposed.api.XposedInterface
import io.github.placebolocationaccuracy.xposed.ConfigReader
import io.github.placebolocationaccuracy.xposed.HookLogger
import io.github.placebolocationaccuracy.xposed.HookUtil
import io.github.placebolocationaccuracy.xposed.TargetMatcher
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hooks Wi-Fi scanning inside system_server for GMS only (including Android 11-16 APEX service-wifi.jar).
 */
object SystemWifiHook {
    private val wifiServiceHooked = AtomicBoolean(false)

    fun install(module: XposedInterface, systemClassLoader: ClassLoader) {
        tryHookWifiClasses(module, systemClassLoader)

        val ssmClass = HookUtil.findClassOrNull(
            "com.android.server.SystemServiceManager",
            systemClassLoader
        )
        if (ssmClass == null) {
            HookLogger.warn("SystemServiceManager not found on API ${Build.VERSION.SDK_INT}")
            return
        }

        val hooker = XposedInterface.Hooker { chain ->
            val result = chain.proceed()
            if (!wifiServiceHooked.get() && result != null) {
                val name = result.javaClass.name
                if (name.contains("WifiService", ignoreCase = true) ||
                    name.contains("WifiScanningService", ignoreCase = true)
                ) {
                    result.javaClass.classLoader?.let { wifiCl ->
                        tryHookWifiClasses(module, wifiCl)
                    }
                }
            }
            result
        }

        HookUtil.hookAllMethods(module, ssmClass, "startService", hooker)
        HookUtil.hookAllMethods(module, ssmClass, "startServiceFromJar", hooker)
    }

    private fun tryHookWifiClasses(module: XposedInterface, classLoader: ClassLoader) {
        val wifiImplClass = HookUtil.findClassOrNull(
            "com.android.server.wifi.WifiServiceImpl",
            classLoader
        ) ?: return

        if (!wifiServiceHooked.compareAndSet(false, true)) return

        runCatching {
            HookUtil.hookAllMethods(module, wifiImplClass, "getScanResults") { chain ->
                if (ConfigReader.blockGmsWifi() && TargetMatcher.isCallingPackageGms(chain.args)) {
                    HookLogger.debug("WifiServiceImpl.getScanResults -> emptyList for GMS")
                    return@hookAllMethods Collections.emptyList<Any>()
                }
                chain.proceed()
            }

            HookUtil.hookAllMethods(module, wifiImplClass, "startScan") { chain ->
                if (ConfigReader.blockGmsWifi() && TargetMatcher.isCallingPackageGms(chain.args)) {
                    HookLogger.debug("WifiServiceImpl.startScan -> false for GMS")
                    return@hookAllMethods false
                }
                chain.proceed()
            }

            HookLogger.info("Hooked WifiServiceImpl via ${classLoader.javaClass.simpleName}")
        }.onFailure {
            wifiServiceHooked.set(false)
            HookLogger.warn("Failed hooking WifiServiceImpl", it)
        }
    }
}
