package io.github.placebolocationaccuracy.xposed.hooks

import io.github.libxposed.api.XposedInterface
import io.github.placebolocationaccuracy.xposed.ConfigReader
import io.github.placebolocationaccuracy.xposed.HookLogger
import io.github.placebolocationaccuracy.xposed.HookUtil
import io.github.placebolocationaccuracy.xposed.TargetMatcher
import java.util.Collections

/**
 * Server-side defense-in-depth hook for com.android.phone (PhoneInterfaceManager).
 */
object TelephonyServiceHook {

    fun install(module: XposedInterface, classLoader: ClassLoader) {
        val phoneMgrClass = HookUtil.findClassOrNull(
            "com.android.phone.PhoneInterfaceManager",
            classLoader
        )
        if (phoneMgrClass == null) {
            HookLogger.warn("PhoneInterfaceManager not found in com.android.phone")
            return
        }

        HookUtil.hookAllMethods(module, phoneMgrClass, "getAllCellInfo") { chain ->
            if (ConfigReader.blockGmsCell() && TargetMatcher.isCallingPackageGms(chain.args)) {
                HookLogger.debug("PhoneInterfaceManager.getAllCellInfo -> emptyList for GMS")
                return@hookAllMethods Collections.emptyList<Any>()
            }
            chain.proceed()
        }

        HookUtil.hookAllMethods(module, phoneMgrClass, "getCellLocation") { chain ->
            if (ConfigReader.blockGmsCell() && TargetMatcher.isCallingPackageGms(chain.args)) {
                HookLogger.debug("PhoneInterfaceManager.getCellLocation -> null for GMS")
                return@hookAllMethods null
            }
            chain.proceed()
        }

        HookUtil.hookAllMethods(module, phoneMgrClass, "requestCellInfoUpdate") { chain ->
            if (ConfigReader.blockGmsCell() && TargetMatcher.isCallingPackageGms(chain.args)) {
                HookLogger.debug("PhoneInterfaceManager.requestCellInfoUpdate blocked for GMS")
                return@hookAllMethods null
            }
            chain.proceed()
        }

        HookLogger.info("Hooked PhoneInterfaceManager in com.android.phone")
    }
}
