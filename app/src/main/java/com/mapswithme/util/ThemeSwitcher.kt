package com.mapswithme.util

import android.app.Activity
import android.location.Location
import com.mapswithme.maps.Framework
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.downloader.DownloaderStatusIcon
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.routing.RoutingController
import com.mapswithme.util.concurrency.UiThread

object ThemeSwitcher {
    private const val CHECK_INTERVAL_MS = 30 * 60 * 1000.toLong()
    private var sRendererActive = false
    private val AUTO_THEME_CHECKER: Runnable = object : Runnable {
        override fun run() {
            var theme: String = ThemeUtils.THEME_DEFAULT
            if (RoutingController.get().isNavigating) {
                val last: Location? = LocationHelper.INSTANCE.savedLocation
                theme = if (last == null) {
                    Config.getCurrentUiTheme()
                } else {
                    val day: Boolean = Framework.nativeIsDayTime(
                        System.currentTimeMillis() / 1000,
                        last.latitude, last.longitude
                    )
                    if (day) ThemeUtils.THEME_DEFAULT else ThemeUtils.THEME_NIGHT
                }
            }
            setThemeAndMapStyle(theme)
            UiThread.cancelDelayedTasks(this)
            if (ThemeUtils.isAutoTheme) UiThread.runLater(
                this,
                CHECK_INTERVAL_MS
            )
        }
    }

    /**
     * Changes the UI theme of application and the map style if necessary. If the contract regarding
     * the input parameter is broken, the UI will be frozen during attempting to change the map style
     * through the synchronous method [Framework.nativeSetMapStyle].
     *
     * @param isRendererActive Indicates whether OpenGL renderer is active or not. Must be
     * `true` only if the map is rendered and visible on the screen
     * at this moment, otherwise `false`.
     */
    @androidx.annotation.UiThread
    fun restart(isRendererActive: Boolean) {
        sRendererActive = isRendererActive
        val theme: String = Config.getUiThemeSettings()
        if (ThemeUtils.isAutoTheme(theme)) {
            AUTO_THEME_CHECKER.run()
        }
        UiThread.cancelDelayedTasks(AUTO_THEME_CHECKER)
        setThemeAndMapStyle(theme)
    }

    private fun setThemeAndMapStyle(theme: String) {
        val oldTheme: String = Config.getCurrentUiTheme()
        Config.setCurrentUiTheme(theme)
        changeMapStyle(theme, oldTheme)
    }

    @androidx.annotation.UiThread
    private fun changeMapStyle(newTheme: String, oldTheme: String) {
        @Framework.MapStyle var style: Int =
            if (RoutingController.get().isVehicleNavigation) Framework.MAP_STYLE_VEHICLE_CLEAR else Framework.MAP_STYLE_CLEAR
        if (ThemeUtils.isNightTheme(newTheme)) style =
            if (RoutingController.get().isVehicleNavigation) Framework.MAP_STYLE_VEHICLE_DARK else Framework.MAP_STYLE_DARK
        if (newTheme != oldTheme) {
            SetMapStyle(style)
            DownloaderStatusIcon.clearCache()
            val a: Activity? = MwmApplication.backgroundTracker()?.topActivity
            if (a != null && !a.isFinishing) a.recreate()
        } else { // If the UI theme is not changed we just need to change the map style if needed.
            val currentStyle: Int = Framework.nativeGetMapStyle()
            if (currentStyle == style) return
            SetMapStyle(style)
        }
    }

    private fun SetMapStyle(@Framework.MapStyle style: Int) { // If rendering is not active we can mark map style, because all graphics
// will be recreated after rendering activation.
        if (sRendererActive) Framework.nativeSetMapStyle(style) else Framework.nativeMarkMapStyle(
            style
        )
    }
}