package com.kizakiworks.placebolocationaccuracy.ui

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.libxposed.service.XposedService
import com.kizakiworks.placebolocationaccuracy.BuildConfig
import com.kizakiworks.placebolocationaccuracy.PlaceboApp
import com.kizakiworks.placebolocationaccuracy.R
import com.kizakiworks.placebolocationaccuracy.config.ModuleConfig

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
        setupEdgeToEdge()
        setContentView(R.layout.activity_main)
        applySystemBarInsets()
        setupLanguageSwitcher()
        setupFooter()
        PlaceboApp.addServiceListener(serviceListener)
    }

    override fun onDestroy() {
        PlaceboApp.removeServiceListener(serviceListener)
        super.onDestroy()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isNight
            isAppearanceLightNavigationBars = !isNight
        }
    }

    private fun applySystemBarInsets() {
        val container = findViewById<View>(R.id.contentContainer)
        val initialLeft = container.paddingLeft
        val initialTop = container.paddingTop
        val initialRight = container.paddingRight
        val initialBottom = container.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(container) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(
                left = initialLeft + bars.left,
                top = initialTop + bars.top,
                right = initialRight + bars.right,
                bottom = initialBottom + bars.bottom
            )
            windowInsets
        }
    }

    private fun setupLanguageSwitcher() {
        val badgeView = findViewById<TextView>(R.id.tvLanguageBadge)
        badgeView.setText(resolveCurrentLanguageBadgeRes())

        val btnLanguage = findViewById<View>(R.id.btnLanguage)
        btnLanguage.setOnClickListener {
            showLanguageDropdown(btnLanguage)
        }
    }

    private fun resolveCurrentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return LANG_SYSTEM
        val tag = locales.toLanguageTags()
        return when {
            tag.startsWith("zh-TW", ignoreCase = true) ||
                tag.startsWith("zh-Hant", ignoreCase = true) -> LANG_ZH_TW
            tag.startsWith("zh-CN", ignoreCase = true) ||
                tag.startsWith("zh-Hans", ignoreCase = true) -> LANG_ZH_CN
            tag.startsWith("en", ignoreCase = true) -> LANG_EN
            else -> LANG_SYSTEM
        }
    }

    private fun resolveCurrentLanguageBadgeRes(): Int = when (resolveCurrentLanguageTag()) {
        LANG_ZH_TW -> R.string.language_badge_zh_tw
        LANG_ZH_CN -> R.string.language_badge_zh_cn
        LANG_EN -> R.string.language_badge_en
        else -> R.string.language_badge_system
    }

    private fun showLanguageDropdown(anchor: View) {
        val tags = arrayOf(LANG_SYSTEM, LANG_ZH_TW, LANG_ZH_CN, LANG_EN)
        val labels = arrayOf(
            getString(R.string.language_option_system),
            getString(R.string.language_option_zh_tw),
            getString(R.string.language_option_zh_cn),
            getString(R.string.language_option_en)
        )
        val currentTag = resolveCurrentLanguageTag()

        val density = resources.displayMetrics.density
        val contentView = LayoutInflater.from(this).inflate(R.layout.popup_language_menu, null)
        val card = contentView.findViewById<View>(R.id.cardLanguagePopup)
        val itemsContainer = contentView.findViewById<LinearLayout>(R.id.containerLanguageItems)

        val popupWindow = PopupWindow(
            contentView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 0f
        }

        val activeBgColor = ContextCompat.getColor(this, R.color.brand_primary_container)
        val activeTextColor = ContextCompat.getColor(this, R.color.brand_primary)
        val normalTextColor = ContextCompat.getColor(this, R.color.text_primary)
        val itemRadius = 14f * density

        tags.forEachIndexed { index, tag ->
            val isSelected = tag == currentTag
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val hPad = (12 * density).toInt()
                val vPad = (10 * density).toInt()
                setPadding(hPad, vPad, hPad, vPad)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) topMargin = (2 * density).toInt()
                }
                background = GradientDrawable().apply {
                    cornerRadius = itemRadius
                    setColor(if (isSelected) activeBgColor else Color.TRANSPARENT)
                }
                isClickable = true
                isFocusable = true
            }

            val labelView = TextView(this).apply {
                text = labels[index]
                textSize = 14f
                setTextColor(if (isSelected) activeTextColor else normalTextColor)
                setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(labelView)

            if (isSelected) {
                val dot = View(this).apply {
                    val size = (7 * density).toInt()
                    layoutParams = LinearLayout.LayoutParams(size, size)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(activeTextColor)
                    }
                }
                row.addView(dot)
            }

            row.setOnClickListener {
                popupWindow.dismiss()
                if (tag != currentTag) {
                    val targetLocales = if (tag == LANG_SYSTEM) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(tag)
                    }
                    AppCompatDelegate.setApplicationLocales(targetLocales)
                }
            }

            itemsContainer.addView(row)
        }

        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = contentView.measuredWidth
        val shadowPad = (6 * density).toInt()
        val xOffset = anchor.width - popupWidth + shadowPad
        val yOffset = (2 * density).toInt()

        popupWindow.showAsDropDown(anchor, xOffset, yOffset)

        card.pivotX = card.measuredWidth.toFloat()
        card.pivotY = 0f
        card.scaleX = 0.92f
        card.scaleY = 0.85f
        card.alpha = 0f
        card.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(150L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun setupFooter() {
        findViewById<TextView>(R.id.tvFooterVersion).text =
            getString(R.string.footer_version_format, BuildConfig.VERSION_NAME)

        findViewById<View>(R.id.btnGithubRepo).setOnClickListener {
            openExternalUrl(URL_GITHUB_REPO)
        }
        findViewById<View>(R.id.btnAuthorProfile).setOnClickListener {
            openExternalUrl(URL_AUTHOR_PROFILE)
        }
    }

    private fun openExternalUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun applyServiceState(service: XposedService?) {
        val card = findViewById<MaterialCardView>(R.id.cardStatus)
        val heroContent = findViewById<View>(R.id.heroContent)
        val iconContainer = findViewById<FrameLayout>(R.id.statusIconContainer)
        val iconView = findViewById<ImageView>(R.id.ivStatusIcon)
        val title = findViewById<TextView>(R.id.tvStatusTitle)
        val pill = findViewById<TextView>(R.id.tvStatusPill)
        val detail = findViewById<TextView>(R.id.tvStatusDetail)

        if (service != null) {
            val frameworkInfo = runCatching {
                "${service.frameworkName} ${service.frameworkVersion} · API ${service.apiVersion}"
            }.getOrDefault("Modern Xposed · API 101+")

            card.strokeColor = ContextCompat.getColor(this, R.color.status_active_stroke)
            heroContent.setBackgroundResource(R.drawable.bg_hero_active)
            iconContainer.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.status_active_icon_bg)
            )
            iconView.setImageResource(R.drawable.ic_shield_check)
            iconView.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.status_active_icon_tint)
            )

            title.setText(R.string.status_active)
            title.setTextColor(ContextCompat.getColor(this, R.color.status_active_title))

            pill.text = frameworkInfo
            pill.setTextColor(ContextCompat.getColor(this, R.color.status_active_pill_text))
            pill.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.status_active_pill_bg)
            )

            detail.setText(R.string.status_active_detail)
            detail.setTextColor(ContextCompat.getColor(this, R.color.status_active_detail))

            remotePrefs = runCatching {
                service.getRemotePreferences(ModuleConfig.PREFS_NAME)
            }.onFailure {
                Toast.makeText(this, R.string.prefs_error_world_readable, Toast.LENGTH_LONG).show()
            }.getOrNull()
        } else {
            card.strokeColor = ContextCompat.getColor(this, R.color.status_inactive_stroke)
            heroContent.setBackgroundResource(R.drawable.bg_hero_inactive)
            iconContainer.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.status_inactive_icon_bg)
            )
            iconView.setImageResource(R.drawable.ic_shield_alert)
            iconView.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.status_inactive_icon_tint)
            )

            title.setText(R.string.status_inactive)
            title.setTextColor(ContextCompat.getColor(this, R.color.status_inactive_title))

            pill.setText(R.string.status_pill_inactive)
            pill.setTextColor(ContextCompat.getColor(this, R.color.status_inactive_pill_text))
            pill.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.status_inactive_pill_bg)
            )

            detail.setText(R.string.status_inactive_detail)
            detail.setTextColor(ContextCompat.getColor(this, R.color.status_inactive_detail))

            remotePrefs = null
        }

        bindAllSwitches()
    }

    private fun bindAllSwitches() {
        bindSwitch(
            R.id.rowBlockNetworkLocation,
            R.id.switchBlockNetworkLocation,
            ModuleConfig.KEY_BLOCK_NETWORK_LOCATION,
            ModuleConfig.DEFAULT_BLOCK_NETWORK_LOCATION
        )
        bindSwitch(
            R.id.rowBlockGmsWifi,
            R.id.switchBlockGmsWifi,
            ModuleConfig.KEY_BLOCK_GMS_WIFI,
            ModuleConfig.DEFAULT_BLOCK_GMS_WIFI
        )
        bindSwitch(
            R.id.rowBlockGmsCell,
            R.id.switchBlockGmsCell,
            ModuleConfig.KEY_BLOCK_GMS_CELL,
            ModuleConfig.DEFAULT_BLOCK_GMS_CELL
        )
        bindSwitch(
            R.id.rowBlockGmsBle,
            R.id.switchBlockGmsBle,
            ModuleConfig.KEY_BLOCK_GMS_BLE,
            ModuleConfig.DEFAULT_BLOCK_GMS_BLE
        )
        bindSwitch(
            R.id.rowBlockLocationBypass,
            R.id.switchBlockLocationBypass,
            ModuleConfig.KEY_BLOCK_LOCATION_BYPASS,
            ModuleConfig.DEFAULT_BLOCK_LOCATION_BYPASS
        )
        bindSwitch(
            R.id.rowVerboseLogging,
            R.id.switchVerboseLogging,
            ModuleConfig.KEY_VERBOSE_LOGGING,
            ModuleConfig.DEFAULT_VERBOSE_LOGGING
        )
    }

    private fun bindSwitch(rowId: Int, switchId: Int, key: String, defaultValue: Boolean) {
        val row = findViewById<View>(rowId)
        val sw = findViewById<MaterialSwitch>(switchId)
        sw.setOnCheckedChangeListener(null)

        val prefs = remotePrefs
        if (prefs == null) {
            sw.isChecked = defaultValue
            sw.isEnabled = false
            row.isEnabled = false
            row.alpha = 0.62f
            row.setOnClickListener(null)
            return
        }

        sw.isEnabled = true
        row.isEnabled = true
        row.alpha = 1f
        sw.isChecked = runCatching { prefs.getBoolean(key, defaultValue) }.getOrDefault(defaultValue)
        sw.setOnCheckedChangeListener { _, isChecked ->
            runCatching { prefs.edit().putBoolean(key, isChecked).apply() }
        }
        row.setOnClickListener {
            sw.toggle()
        }
    }

    companion object {
        private const val LANG_SYSTEM = ""
        private const val LANG_ZH_TW = "zh-TW"
        private const val LANG_ZH_CN = "zh-CN"
        private const val LANG_EN = "en"
        private const val URL_GITHUB_REPO = "https://github.com/kizaki-R/Placebo-Location-Accuracy"
        private const val URL_AUTHOR_PROFILE = "https://github.com/kizaki-R"
    }
}
