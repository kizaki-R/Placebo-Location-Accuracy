package com.kizakiworks.placebolocationaccuracy.xposed

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object HookUtil {

    fun findClassOrNull(name: String, classLoader: ClassLoader?): Class<*>? {
        return runCatching {
            if (classLoader != null) Class.forName(name, false, classLoader) else Class.forName(name)
        }.getOrNull()
    }

    fun hookAllMethods(
        module: XposedInterface,
        clazz: Class<*>,
        methodName: String,
        hooker: XposedInterface.Hooker
    ): Int {
        var count = 0
        for (method in clazz.declaredMethods) {
            if (method.name != methodName) continue
            val modifiers = method.modifiers
            if (Modifier.isAbstract(modifiers) || method.isBridge || method.isSynthetic) continue
            runCatching {
                module.hook(method).intercept(hooker)
                count++
            }.onFailure {
                HookLogger.warn("Failed hooking ${clazz.simpleName}.$methodName(${method.parameterTypes.joinToString { p -> p.simpleName }})", it)
            }
        }
        return count
    }

    fun findFieldInHierarchy(clazz: Class<*>, fieldName: String): Field? {
        var current: Class<*>? = clazz
        while (current != null) {
            val field = runCatching { current.getDeclaredField(fieldName) }.getOrNull()
            if (field != null) {
                field.isAccessible = true
                return field
            }
            current = current.superclass
        }
        return null
    }

    fun getObjectFieldOrNull(obj: Any?, fieldName: String): Any? {
        if (obj == null) return null
        return runCatching {
            findFieldInHierarchy(obj.javaClass, fieldName)?.get(obj)
        }.getOrNull()
    }

    fun callZeroArgMethodOrNull(obj: Any?, methodName: String): Any? {
        if (obj == null) return null
        return runCatching {
            val method = obj.javaClass.methods.firstOrNull { it.name == methodName && it.parameterCount == 0 }
                ?: return null
            method.invoke(obj)
        }.getOrNull()
    }

    fun defaultReturnFor(executable: Executable): Any? {
        val returnType = (executable as? Method)?.returnType ?: return null
        return when (returnType) {
            Boolean::class.javaPrimitiveType -> false
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            Float::class.javaPrimitiveType -> 0f
            Double::class.javaPrimitiveType -> 0.0
            Short::class.javaPrimitiveType -> 0.toShort()
            Byte::class.javaPrimitiveType -> 0.toByte()
            Char::class.javaPrimitiveType -> '\u0000'
            else -> null
        }
    }
}
