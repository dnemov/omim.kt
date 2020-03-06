package com.mapswithme.util

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language

object ThemeUtils {
    val THEME_DEFAULT: String = MwmApplication.get().getString(R.string.theme_default)
    val THEME_NIGHT: String = MwmApplication.get().getString(R.string.theme_night)
    val THEME_AUTO: String = MwmApplication.get().getString(R.string.theme_auto)
    private val VALUE_BUFFER = TypedValue()
    @kotlin.jvm.JvmStatic
    @ColorInt
    fun getColor(context: Context, @AttrRes attr: Int): Int {
        require(
            context.theme.resolveAttribute(
                attr,
                VALUE_BUFFER,
                true
            )
        ) { "Failed to resolve color theme attribute" }
        return VALUE_BUFFER.data
    }

    @JvmStatic
    fun getResource(context: Context, @AttrRes attr: Int): Int {
        require(
            context.theme.resolveAttribute(
                attr,
                VALUE_BUFFER,
                true
            )
        ) { "Failed to resolve theme attribute" }
        return VALUE_BUFFER.resourceId
    }

    fun getResource(context: Context, @AttrRes style: Int, @AttrRes attr: Int): Int {
        val styleRef = getResource(context, style)
        val attrs = intArrayOf(attr)
        val ta = context.theme.obtainStyledAttributes(styleRef, attrs)
        ta.getValue(0, VALUE_BUFFER)
        ta.recycle()
        return VALUE_BUFFER.resourceId
    }

    fun themedInflater(src: LayoutInflater, @StyleRes theme: Int): LayoutInflater {
        val wrapper: Context = ContextThemeWrapper(src.context, theme)
        return src.cloneInContext(wrapper)
    }

    val isDefaultTheme: Boolean
        get() = isDefaultTheme(Config.getCurrentUiTheme())

    fun isDefaultTheme(theme: String): Boolean {
        return THEME_DEFAULT == theme
    }

    val isNightTheme: Boolean
        get() = isNightTheme(Config.getCurrentUiTheme())

    fun isNightTheme(theme: String): Boolean {
        return THEME_NIGHT == theme
    }

    val isAutoTheme: Boolean
        get() = THEME_AUTO == Config.getUiThemeSettings()

    fun isAutoTheme(theme: String): Boolean {
        return THEME_AUTO == theme
    }

    fun isValidTheme(theme: String): Boolean {
        return THEME_DEFAULT == theme || THEME_NIGHT == theme
    }

    @StyleRes
    fun getCardBgThemeResourceId(theme: String): Int {
        if (isDefaultTheme(theme)) return R.style.MwmTheme_CardBg
        if (isNightTheme(theme)) return R.style.MwmTheme_Night_CardBg
        throw IllegalArgumentException("Attempt to apply unsupported theme: $theme")
    }

    @StyleRes
    fun getWindowBgThemeResourceId(theme: String): Int {
        if (isDefaultTheme(theme)) return R.style.MwmTheme_WindowBg
        if (isNightTheme(theme)) return R.style.MwmTheme_Night_WindowBg
        throw IllegalArgumentException("Attempt to apply unsupported theme: $theme")
    }
}