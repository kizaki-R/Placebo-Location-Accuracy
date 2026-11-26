package com.kizakiworks.placebolocationaccuracy

import android.app.Application
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArraySet

class PlaceboApp : Application() {

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                currentService = service
                listeners.forEach { it.invoke(service) }
            }

            override fun onServiceDied(service: XposedService) {
                if (currentService == service) {
                    currentService = null
                    listeners.forEach { it.invoke(null) }
                }
            }
        })
    }

    companion object {
        @Volatile
        var currentService: XposedService? = null
            private set

        private val listeners = CopyOnWriteArraySet<(XposedService?) -> Unit>()

        fun addServiceListener(listener: (XposedService?) -> Unit) {
            listeners.add(listener)
            listener.invoke(currentService)
        }

        fun removeServiceListener(listener: (XposedService?) -> Unit) {
            listeners.remove(listener)
        }
    }
}
