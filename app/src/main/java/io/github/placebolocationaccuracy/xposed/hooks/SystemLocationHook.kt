package io.github.placebolocationaccuracy.xposed.hooks

import android.location.Location
import android.os.Build
import io.github.libxposed.api.XposedInterface
import io.github.placebolocationaccuracy.config.ModuleConfig
import io.github.placebolocationaccuracy.xposed.ConfigReader
import io.github.placebolocationaccuracy.xposed.HookLogger
import io.github.placebolocationaccuracy.xposed.HookUtil
import io.github.placebolocationaccuracy.xposed.TargetMatcher

/**
 * Hooks Android system_server location stack across Android 9-16+ using Modern Xposed API 101+.
 */
object SystemLocationHook {

    fun install(module: XposedInterface, classLoader: ClassLoader) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hookLocationProviderManager(module, classLoader)
        }
        hookLocationManagerService(module, classLoader)
    }

    private fun hookLocationProviderManager(module: XposedInterface, classLoader: ClassLoader) {
        val managerClass = HookUtil.findClassOrNull(
            "com.android.server.location.provider.LocationProviderManager",
            classLoader
        )
        if (managerClass == null) {
            HookLogger.warn("LocationProviderManager not found on API ${Build.VERSION.SDK_INT}")
            return
        }

        HookUtil.hookAllMethods(module, managerClass, "onReportLocation") { chain ->
            if (ConfigReader.blockNetworkLocation()) {
                val providerName = getProviderManagerName(chain.thisObject)
                if (ModuleConfig.isBlockedProvider(providerName)) {
                    HookLogger.debug("system_server: dropped onReportLocation for provider=$providerName")
                    return@hookAllMethods null
                }
            }
            chain.proceed()
        }

        val requestMethods = listOf("getLastLocation", "getCurrentLocation", "registerLocationRequest")
        for (methodName in requestMethods) {
            HookUtil.hookAllMethods(module, managerClass, methodName) { chain ->
                val providerName = getProviderManagerName(chain.thisObject)
                if (ConfigReader.blockNetworkLocation() && ModuleConfig.isBlockedProvider(providerName)) {
                    HookLogger.debug("system_server: blocked LPM.$methodName for provider=$providerName")
                    return@hookAllMethods HookUtil.defaultReturnFor(chain.executable)
                }
                if (ConfigReader.blockLocationBypass() && TargetMatcher.isCallingPackageGms(chain.args)) {
                    stripBypassFromArgs(chain.args)
                }
                chain.proceed()
            }
        }

        HookLogger.info("Hooked LocationProviderManager (API ${Build.VERSION.SDK_INT})")
    }

    private fun hookLocationManagerService(module: XposedInterface, classLoader: ClassLoader) {
        val candidateNames = listOf(
            "com.android.server.location.LocationManagerService",
            "com.android.server.LocationManagerService"
        )
        val lmsClass = candidateNames.firstNotNullOfOrNull {
            HookUtil.findClassOrNull(it, classLoader)
        }
        if (lmsClass == null) {
            HookLogger.warn("LocationManagerService not found on API ${Build.VERSION.SDK_INT}")
            return
        }

        val queryMethods = listOf(
            "getLastLocation",
            "getCurrentLocation",
            "registerLocationListener",
            "registerLocationPendingIntent",
            "requestLocationUpdates"
        )
        for (methodName in queryMethods) {
            HookUtil.hookAllMethods(module, lmsClass, methodName) { chain ->
                val providerArg = extractProviderArg(chain.args)
                if (ConfigReader.blockNetworkLocation() && ModuleConfig.isBlockedProvider(providerArg)) {
                    HookLogger.debug("LMS.$methodName blocked for provider=$providerArg")
                    return@hookAllMethods HookUtil.defaultReturnFor(chain.executable)
                }
                if (ConfigReader.blockLocationBypass() && TargetMatcher.isCallingPackageGms(chain.args)) {
                    stripBypassFromArgs(chain.args)
                }
                chain.proceed()
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val legacyReportMethods = listOf(
                "handleLocationChanged",
                "handleLocationChangedLocked",
                "reportLocation"
            )
            for (methodName in legacyReportMethods) {
                HookUtil.hookAllMethods(module, lmsClass, methodName) { chain ->
                    if (ConfigReader.blockNetworkLocation()) {
                        val location = chain.args.firstOrNull { it is Location } as? Location
                        if (location != null && ModuleConfig.isBlockedProvider(location.provider)) {
                            HookLogger.debug("LMS.$methodName dropped legacy network location")
                            return@hookAllMethods null
                        }
                    }
                    chain.proceed()
                }
            }
        }

        HookLogger.info("Hooked ${lmsClass.name} (API ${Build.VERSION.SDK_INT})")
    }

    private fun getProviderManagerName(managerObj: Any?): String? {
        if (managerObj == null) return null
        return (HookUtil.getObjectFieldOrNull(managerObj, "mName") as? String)
            ?: (HookUtil.callZeroArgMethodOrNull(managerObj, "getName") as? String)
    }

    private fun extractProviderArg(args: List<Any?>): String? {
        for (arg in args) {
            if (arg is String && (ModuleConfig.isBlockedProvider(arg) || ModuleConfig.isPureGpsProvider(arg))) {
                return arg
            }
            if (arg != null && arg.javaClass.name == "android.location.LocationRequest") {
                val p = HookUtil.callZeroArgMethodOrNull(arg, "getProvider") as? String
                if (p != null) return p
            }
        }
        return null
    }

    private fun stripBypassFromArgs(args: List<Any?>) {
        for (arg in args) {
            if (arg == null) continue
            val clsName = arg.javaClass.name
            if (clsName == "android.location.LocationRequest" ||
                clsName == "android.location.LastLocationRequest"
            ) {
                runCatching {
                    val field = HookUtil.findFieldInHierarchy(arg.javaClass, "mLocationSettingsIgnored")
                    if (field != null && field.getBoolean(arg)) {
                        field.setBoolean(arg, false)
                        HookLogger.debug("Stripped mLocationSettingsIgnored from $clsName")
                    }
                }
            }
        }
    }
}
