package com.kizakiworks.placebolocationaccuracy.xposed.hooks

import android.bluetooth.le.BluetoothLeScanner
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.rtt.WifiRttManager
import android.os.Build
import android.telephony.TelephonyManager
import io.github.libxposed.api.XposedInterface
import com.kizakiworks.placebolocationaccuracy.config.ModuleConfig
import com.kizakiworks.placebolocationaccuracy.xposed.ConfigReader
import com.kizakiworks.placebolocationaccuracy.xposed.HookLogger
import com.kizakiworks.placebolocationaccuracy.xposed.HookUtil
import java.util.Collections
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Hooks stable Android Framework client APIs inside com.google.android.gms and com.google.android.gsf.
 */
object GmsClientHook {

    fun install(module: XposedInterface, classLoader: ClassLoader, packageName: String) {
        hookWifi(module, classLoader)
        hookTelephony(module)
        hookBluetoothLe(module)
        hookLocationManager(module)
        HookLogger.info("GmsClientHook installed for $packageName (API ${Build.VERSION.SDK_INT})")
    }

    private fun hookWifi(module: XposedInterface, classLoader: ClassLoader) {
        HookUtil.hookAllMethods(module, WifiManager::class.java, "getScanResults") { chain ->
            if (ConfigReader.blockGmsWifi()) {
                HookLogger.debug("GMS WifiManager.getScanResults -> emptyList")
                return@hookAllMethods Collections.emptyList<Any>()
            }
            chain.proceed()
        }

        HookUtil.hookAllMethods(module, WifiManager::class.java, "startScan") { chain ->
            if (ConfigReader.blockGmsWifi()) {
                HookLogger.debug("GMS WifiManager.startScan -> false")
                return@hookAllMethods false
            }
            chain.proceed()
        }

        val wifiScannerClass = HookUtil.findClassOrNull("android.net.wifi.WifiScanner", classLoader)
        if (wifiScannerClass != null) {
            for (name in listOf("startScan", "startBackgroundScan", "startSingleScan")) {
                HookUtil.hookAllMethods(module, wifiScannerClass, name) { chain ->
                    if (ConfigReader.blockGmsWifi()) {
                        HookLogger.debug("GMS WifiScanner.$name blocked")
                        return@hookAllMethods HookUtil.defaultReturnFor(chain.executable)
                    }
                    chain.proceed()
                }
            }
            HookUtil.hookAllMethods(module, wifiScannerClass, "getSingleScanResults") { chain ->
                if (ConfigReader.blockGmsWifi()) {
                    HookLogger.debug("GMS WifiScanner.getSingleScanResults -> emptyList")
                    return@hookAllMethods Collections.emptyList<Any>()
                }
                chain.proceed()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HookUtil.hookAllMethods(module, WifiRttManager::class.java, "startRanging") { chain ->
                if (ConfigReader.blockGmsWifi()) {
                    HookLogger.debug("GMS WifiRttManager.startRanging blocked")
                    return@hookAllMethods null
                }
                chain.proceed()
            }
        }
    }

    private fun hookTelephony(module: XposedInterface) {
        HookUtil.hookAllMethods(module, TelephonyManager::class.java, "getAllCellInfo") { chain ->
            if (ConfigReader.blockGmsCell()) {
                HookLogger.debug("GMS TelephonyManager.getAllCellInfo -> emptyList")
                return@hookAllMethods Collections.emptyList<Any>()
            }
            chain.proceed()
        }

        HookUtil.hookAllMethods(module, TelephonyManager::class.java, "getCellLocation") { chain ->
            if (ConfigReader.blockGmsCell()) {
                HookLogger.debug("GMS TelephonyManager.getCellLocation -> null")
                return@hookAllMethods null
            }
            chain.proceed()
        }

        HookUtil.hookAllMethods(module, TelephonyManager::class.java, "getNeighboringCellInfo") { chain ->
            if (ConfigReader.blockGmsCell()) {
                HookLogger.debug("GMS TelephonyManager.getNeighboringCellInfo -> emptyList")
                return@hookAllMethods Collections.emptyList<Any>()
            }
            chain.proceed()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            HookUtil.hookAllMethods(module, TelephonyManager::class.java, "requestCellInfoUpdate") { chain ->
                if (ConfigReader.blockGmsCell()) {
                    val executor = chain.args.firstOrNull { it is Executor } as? Executor
                    val callback = chain.args.firstOrNull {
                        it != null && it.javaClass.name.contains("CellInfoCallback")
                    }
                    if (executor != null && callback != null) {
                        runCatching {
                            val onCellInfoMethod = callback.javaClass.methods.firstOrNull {
                                it.name == "onCellInfo" && it.parameterCount == 1
                            }
                            if (onCellInfoMethod != null) {
                                executor.execute {
                                    runCatching { onCellInfoMethod.invoke(callback, Collections.emptyList<Any>()) }
                                }
                            }
                        }
                    }
                    HookLogger.debug("GMS TelephonyManager.requestCellInfoUpdate -> empty callback")
                    return@hookAllMethods null
                }
                chain.proceed()
            }
        }
    }

    private fun hookBluetoothLe(module: XposedInterface) {
        HookUtil.hookAllMethods(module, BluetoothLeScanner::class.java, "startScan") { chain ->
            if (ConfigReader.blockGmsBle()) {
                HookLogger.debug("GMS BluetoothLeScanner.startScan blocked")
                return@hookAllMethods HookUtil.defaultReturnFor(chain.executable)
            }
            chain.proceed()
        }
    }

    private fun hookLocationManager(module: XposedInterface) {
        HookUtil.hookAllMethods(module, LocationManager::class.java, "getLastKnownLocation") { chain ->
            if (ConfigReader.blockNetworkLocation()) {
                val provider = chain.args.firstOrNull { it is String } as? String
                if (ModuleConfig.isBlockedProvider(provider)) {
                    HookLogger.debug("GMS LocationManager.getLastKnownLocation($provider) -> null")
                    return@hookAllMethods null
                }
            }
            chain.proceed()
        }

        for (methodName in listOf("requestLocationUpdates", "requestSingleUpdate")) {
            HookUtil.hookAllMethods(module, LocationManager::class.java, methodName) { chain ->
                if (ConfigReader.blockNetworkLocation()) {
                    val provider = chain.args.firstOrNull { it is String } as? String
                    if (ModuleConfig.isBlockedProvider(provider)) {
                        HookLogger.debug("GMS LocationManager.$methodName($provider) blocked")
                        return@hookAllMethods null
                    }
                }
                chain.proceed()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HookUtil.hookAllMethods(module, LocationManager::class.java, "getCurrentLocation") { chain ->
                if (ConfigReader.blockNetworkLocation()) {
                    val provider = chain.args.firstOrNull { it is String } as? String
                    if (ModuleConfig.isBlockedProvider(provider)) {
                        val executor = chain.args.firstOrNull { it is Executor } as? Executor
                        @Suppress("UNCHECKED_CAST")
                        val consumer = chain.args.firstOrNull { it is Consumer<*> } as? Consumer<Any?>
                        if (executor != null && consumer != null) {
                            runCatching { executor.execute { consumer.accept(null) } }
                        }
                        HookLogger.debug("GMS LocationManager.getCurrentLocation($provider) -> null")
                        return@hookAllMethods null
                    }
                }
                chain.proceed()
            }
        }
    }
}
