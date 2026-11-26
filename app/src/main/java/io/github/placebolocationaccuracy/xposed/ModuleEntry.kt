package io.github.placebolocationaccuracy.xposed

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import io.github.placebolocationaccuracy.config.ModuleConfig
import io.github.placebolocationaccuracy.xposed.hooks.BluetoothServiceHook
import io.github.placebolocationaccuracy.xposed.hooks.GmsClientHook
import io.github.placebolocationaccuracy.xposed.hooks.SystemLocationHook
import io.github.placebolocationaccuracy.xposed.hooks.SystemWifiHook
import io.github.placebolocationaccuracy.xposed.hooks.TelephonyServiceHook

class ModuleEntry : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        HookLogger.init(this)
        ConfigReader.init(this)
        HookLogger.info("Module loaded in ${param.processName} (framework=${frameworkName} ${frameworkVersion}, libxposed API ${apiVersion})")
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        HookLogger.info("Loading hooks into system_server")
        SystemLocationHook.install(this, param.classLoader)
        SystemWifiHook.install(this, param.classLoader)
    }

    override fun onPackageReady(param: PackageReadyParam) {
        when (param.packageName) {
            "com.android.phone" -> {
                TelephonyServiceHook.install(this, param.classLoader)
            }

            "com.android.bluetooth" -> {
                BluetoothServiceHook.install(this, param.classLoader)
            }

            else -> {
                if (ModuleConfig.isTargetGmsPackage(param.packageName)) {
                    GmsClientHook.install(this, param.classLoader, param.packageName)
                }
            }
        }
    }
}
