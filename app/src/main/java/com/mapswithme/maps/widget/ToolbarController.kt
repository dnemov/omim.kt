package com.mapswithme.maps.widget

import android.app.Activity
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils

open class ToolbarController(root: View, val activity: Activity?) {
    val toolbar: Toolbar
    private val mNavigationClickListener =
        View.OnClickListener { onUpClick() }

    private fun setSupportActionBar() {
        val appCompatActivity = activity as AppCompatActivity
        appCompatActivity.setSupportActionBar(toolbar)
    }

    protected open fun useExtendedToolbar(): Boolean {
        return true
    }

    private fun setupNavigationListener() {
        val customNavigationButton =
            toolbar.findViewById<View>(R.id.back)
        if (customNavigationButton != null) {
            customNavigationButton.setOnClickListener(mNavigationClickListener)
        } else {
            UiUtils.showHomeUpButton(toolbar)
            toolbar.setNavigationOnClickListener(mNavigationClickListener)
        }
    }

    @get:IdRes
    private val toolbarId: Int
        private get() = R.id.toolbar

    open fun onUpClick() {
        Utils.navigateToParent(activity)
    }

    fun setTitle(title: CharSequence?): ToolbarController {
        supportActionBar.title = title
        return this
    }

    fun setTitle(@StringRes title: Int): ToolbarController {
        supportActionBar.setTitle(title)
        return this
    }

    private val supportActionBar: ActionBar
        private get() {
            val appCompatActivity = activity as AppCompatActivity
            return appCompatActivity.supportActionBar!!
        }

    init {
        toolbar = root.findViewById(toolbarId)
        if (useExtendedToolbar()) UiUtils.extendViewWithStatusBar(toolbar)
        setupNavigationListener()
        setSupportActionBar()
    }
}