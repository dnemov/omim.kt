package com.mapswithme.maps.base

import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import com.mapswithme.maps.R
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.ThemeUtils.isNightTheme
import com.mapswithme.util.UiUtils
import org.alohalytics.Statistics

open class BaseMwmDialogFragment : DialogFragment() {

    protected open val style: Int
        get() = STYLE_NORMAL

    protected open val customTheme: Int
        @StyleRes get() = 0

    protected open val fullscreenTheme: Int
        @StyleRes get() = if (isNightTheme) fullscreenDarkTheme else fullscreenLightTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val style = style
        val theme = customTheme
        if (style != STYLE_NORMAL || theme != 0) setStyle(
            style,
            theme
        )
    }

    override fun onResume() {
        super.onResume()
        Statistics.logEvent(
            "\$onResume", javaClass.simpleName
                    + ":" + UiUtils.deviceOrientationAsString(activity!!)
        )
    }

    override fun onPause() {
        super.onPause()
        Statistics.logEvent("\$onPause", javaClass.simpleName)
    }

    protected open val fullscreenLightTheme: Int
        @StyleRes get() = R.style.MwmTheme_DialogFragment_Fullscreen

    protected open val fullscreenDarkTheme: Int
        @StyleRes get() = R.style.MwmTheme_DialogFragment_Fullscreen_Night

    protected val appContextOrThrow: Application
        get() {
            val context = context
                ?: throw IllegalStateException("Before call this method make sure that the context exists")
            return context.applicationContext as Application
        }

    protected val viewOrThrow: View
        get() = this.view
            ?: throw IllegalStateException("Before call this method make sure that the view exists")

    protected val argumentsOrThrow: Bundle
        get() = arguments ?: throw AssertionError("Arguments must be non-null!")
}