package com.mapswithme.maps.base

import android.os.Bundle
import android.view.View
import com.mapswithme.maps.widget.ToolbarController

open class BaseMwmToolbarFragment : BaseAsyncOperationFragment() {
    protected lateinit var toolbarController: ToolbarController
        private set

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        toolbarController = onCreateToolbarController(view)
    }

    protected open fun onCreateToolbarController(root: View): ToolbarController {
        return ToolbarController(root, activity!!)
    }

}