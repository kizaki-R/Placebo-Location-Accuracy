package io.github.placebolocationaccuracy.xposed

import android.content.Context
import android.os.Binder
import android.os.Process
import io.github.placebolocationaccuracy.config.ModuleConfig
import java.util.concurrent.ConcurrentHashMap

object TargetMatcher {
    private val uidCache = ConcurrentHashMap<Int, Boolean>()

    fun isCallingPackageGms(args: List<Any?>?): Boolean {
        if (args != null) {
            for (arg in args) {
                when (arg) {
                    is String -> {
                        if (ModuleConfig.isTargetGmsPackage(arg)) return true
                    }
                    else -> {
                        val extracted = extractPackageNameFromIdentity(arg)
                        if (ModuleConfig.isTargetGmsPackage(extracted)) return true
                    }
                }
            }
        }
        return isCallingUidGms()
    }

    fun isCallingUidGms(): Boolean {
        val uid = Binder.getCallingUid()
        if (uid <= Process.SYSTEM_UID) return false
        return isUidGms(uid)
    }

    fun isUidGms(uid: Int): Boolean {
        if (uid <= Process.SYSTEM_UID) return false
        uidCache[uid]?.let { return it }

        val resolved = resolveUidIsGms(uid)
        if (resolved != null) {
            uidCache[uid] = resolved
            return resolved
        }
        return false
    }

    fun extractPackageNameFromIdentity(obj: Any?): String? {
        if (obj == null) return null
        val className = obj.javaClass.name
        if (!className.contains("CallerIdentity") &&
            !className.contains("AttributionSource") &&
            !className.contains("WorkSource")
        ) {
            return null
        }
        return runCatching {
            val fromMethod = HookUtil.callZeroArgMethodOrNull(obj, "getPackageName") as? String
            fromMethod ?: (HookUtil.getObjectFieldOrNull(obj, "mPackageName") as? String)
                ?: (HookUtil.getObjectFieldOrNull(obj, "packageName") as? String)
        }.getOrNull()
    }

    private fun resolveUidIsGms(uid: Int): Boolean? {
        return runCatching {
            val context = obtainSystemOrAppContext() ?: return null
            val packages = context.packageManager?.getPackagesForUid(uid) ?: return null
            packages.any { ModuleConfig.isTargetGmsPackage(it) }
        }.getOrNull()
    }

    private fun obtainSystemOrAppContext(): Context? {
        return runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentApp = activityThreadClass.getMethod("currentApplication").invoke(null) as? Context
            if (currentApp != null) return currentApp

            val currentThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
                ?: return null
            activityThreadClass.getMethod("getSystemContext").invoke(currentThread) as? Context
        }.getOrNull()
    }
}
