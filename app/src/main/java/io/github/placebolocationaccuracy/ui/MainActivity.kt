package io.github.placebolocationaccuracy.ui

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.libxposed.service.XposedService
import io.github.placebolocationaccuracy.PlaceboApp
import io.github.placebolocationaccuracy.R
import io.github.placebolocationaccuracy.config.ModuleConfig

class MainActivity : AppCompatActivity() {

    private var remotePrefs: SharedPreferences? = null

    private val serviceListener: (XposedService?) -> Unit = { service ->
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                applyServiceState(service)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        PlaceboApp.addServiceListener(serviceListener)
    }

    override fun onDestroy() {
        PlaceboApp.removeServiceListener(serviceListener)
        super.onDestroy()
    }

    private fun applyServiceState(service: XposedService?) {
        val card = findViewById<MaterialCardView>(R.id.cardStatus)
        val title = findViewById<TextView>(R.id.tvStatusTitle)
        val detail = findViewById<TextView>(R.id.tvStatusDetail)

        if (service != null) {
            title.setText(R.string.status_active)
            val frameworkInfo = runCatching {
                "${service.frameworkName} ${service.frameworkVersion} (API ${service.apiVersion})"
            }.getOrDefault("Modern Xposed API 101+")
            detail.text = getString(R.string.status_active_detail_with_framework, frameworkInfo)
            card.strokeColor = Color.parseColor("#00C853")

            remotePrefs = runCatching {
                service.getRemotePreferences(ModuleConfig.PREFS_NAME)
            }.onFailure {
                Toast.makeText(this, R.string.prefs_error_world_readable, Toast.LENGTH_LONG).show()
            }.getOrNull()
        } else {
            title.setText(R.string.status_inactive)
            detail.setText(R.string.status_inactive_detail)
            card.strokeColor = Color.parseColor("#D32F2F")
            remotePrefs = null
        }

        bindAllSwitches()
    }

    private fun bindAllSwitches() {
        bindSwitch(
            R.id.switchBlockNetworkLocation,
            ModuleConfig.KEY_BLOCK_NETWORK_LOCATION,
            ModuleConfig.DEFAULT_BLOCK_NETWORK_LOCATION
        )
        bindSwitch(
            R.id.switchBlockGmsWifi,
            ModuleConfig.KEY_BLOCK_GMS_WIFI,
            ModuleConfig.DEFAULT_BLOCK_GMS_WIFI
        )
        bindSwitch(
            R.id.switchBlockGmsCell,
            ModuleConfig.KEY_BLOCK_GMS_CELL,
            ModuleConfig.DEFAULT_BLOCK_GMS_CELL
        )
        bindSwitch(
            R.id.switchBlockGmsBle,
            ModuleConfig.KEY_BLOCK_GMS_BLE,
            ModuleConfig.DEFAULT_BLOCK_GMS_BLE
        )
        bindSwitch(
            R.id.switchBlockLocationBypass,
            ModuleConfig.KEY_BLOCK_LOCATION_BYPASS,
            ModuleConfig.DEFAULT_BLOCK_LOCATION_BYPASS
        )
        bindSwitch(
            R.id.switchVerboseLogging,
            ModuleConfig.KEY_VERBOSE_LOGGING,
            ModuleConfig.DEFAULT_VERBOSE_LOGGING
        )
    }

    private fun bindSwitch(viewId: Int, key: String, defaultValue: Boolean) {
        val sw = findViewById<MaterialSwitch>(viewId)
        sw.setOnCheckedChangeListener(null)

        val prefs = remotePrefs
        if (prefs == null) {
            sw.isChecked = defaultValue
            sw.isEnabled = false
            return
        }

        sw.isEnabled = true
        sw.isChecked = runCatching { prefs.getBoolean(key, defaultValue) }.getOrDefault(defaultValue)
        sw.setOnCheckedChangeListener { _, isChecked ->
            runCatching { prefs.edit().putBoolean(key, isChecked).apply() }
        }
    }
}
