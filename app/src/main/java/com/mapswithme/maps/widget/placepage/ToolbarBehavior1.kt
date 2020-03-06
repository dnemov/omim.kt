package com.mapswithme.maps.widget.placepage

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils

class ToolbarBehavior : ScrollingViewBehavior {
    constructor() { // Do nothing by default.
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
    }

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        return dependency.id == R.id.placepage
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        if (dependency.y == 0f && UiUtils.isHidden(child)) {
            UiUtils.show(child)
            return false
        }
        if (dependency.y > 0 && UiUtils.isVisible(child)) {
            UiUtils.hide(child)
            return false
        }
        return false
    }
}