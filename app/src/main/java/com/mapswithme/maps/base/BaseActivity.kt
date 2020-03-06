package com.mapswithme.maps.base

import android.app.Activity
import androidx.annotation.StyleRes

interface BaseActivity {
    fun get(): Activity
    @StyleRes
    fun getThemeResourceId(theme: String): Int
}