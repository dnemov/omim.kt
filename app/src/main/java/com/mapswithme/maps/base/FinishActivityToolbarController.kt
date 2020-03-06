package com.mapswithme.maps.base

import android.app.Activity
import android.view.View
import com.mapswithme.maps.widget.ToolbarController

class FinishActivityToolbarController(root: View, activity: Activity) :
    ToolbarController(root, activity) {
    override fun onUpClick() {
        activity?.finish()
    }
}