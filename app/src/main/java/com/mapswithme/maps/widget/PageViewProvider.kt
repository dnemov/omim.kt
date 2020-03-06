package com.mapswithme.maps.widget

import android.view.View

internal interface PageViewProvider {
    fun findViewByIndex(index: Int): View?
}