package com.mapswithme.maps.content

import android.app.Application
import android.content.Context

open class AbstractContextualListener(private val mApp: Application) {

    val context: Context
        get() = mApp
}
