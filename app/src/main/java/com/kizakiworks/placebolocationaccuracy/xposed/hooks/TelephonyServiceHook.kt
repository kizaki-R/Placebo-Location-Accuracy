package com.kizakiworks.placebolocationaccuracy.xposed.hooks

import io.github.libxposed.api.XposedInterface
import com.kizakiworks.placebolocationaccuracy.xposed.ConfigReader
import com.kizakiworks.placebolocationaccuracy.xposed.HookLogger
import com.kizakiworks.placebolocationaccuracy.xposed.HookUtil
import com.kizakiworks.placebolocationaccuracy.xposed.TargetMatcher
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
