package io.github.placebolocationaccuracy.xposed.hooks

import io.github.libxposed.api.XposedInterface
import io.github.placebolocationaccuracy.xposed.ConfigReader
import io.github.placebolocationaccuracy.xposed.HookLogger
import io.github.placebolocationaccuracy.xposed.HookUtil
import io.github.placebolocationaccuracy.xposed.TargetMatcher

/**
 * Server-side defense-in-depth hook for com.android.bluetooth across Android 13-16+.
 */
object BluetoothServiceHook {

    private val candidateClasses = listOf(
        "com.android.bluetooth.gatt.GattService",
        "com.android.bluetooth.le_scan.ScanController",
        "com.android.bluetooth.le_scan.TransitionalScanHelper"
    )

    fun install(module: XposedInterface, classLoader: ClassLoader) {
        var hookedCount = 0
        for (className in candidateClasses) {
            val clazz = HookUtil.findClassOrNull(className, classLoader) ?: continue
            for (methodName in listOf("startScan", "startScanForIntent")) {
                val count = HookUtil.hookAllMethods(module, clazz, methodName) { chain ->
                    if (ConfigReader.blockGmsBle() && TargetMatcher.isCallingPackageGms(chain.args)) {
                        HookLogger.debug("${clazz.simpleName}.$methodName blocked for GMS")
                        return@hookAllMethods HookUtil.defaultReturnFor(chain.executable)
                    }
                    chain.proceed()
                }
                if (count > 0) hookedCount++
            }
        }
        HookLogger.info("BluetoothServiceHook installed ($hookedCount method group(s) hooked)")
    }
}
