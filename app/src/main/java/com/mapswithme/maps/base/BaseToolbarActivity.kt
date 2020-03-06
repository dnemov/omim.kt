package com.mapswithme.maps.base

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils

abstract class BaseToolbarActivity : BaseMwmFragmentActivity() {
    @CallSuper
    override fun onSafeCreate(savedInstanceState: Bundle?) {
        super.onSafeCreate(savedInstanceState)
        val toolbar = toolbar
        toolbar?.let {
            UiUtils.extendViewWithStatusBar(it)
            val title = toolbarTitle
            if (title == 0) it.title = getTitle() else it.setTitle(title)
            setupHomeButton(it)
            displayToolbarAsActionBar()
        }
    }

    protected open fun setupHomeButton(toolbar: Toolbar) {
        UiUtils.showHomeUpButton(toolbar)
    }

    @get:StringRes
    protected val toolbarTitle: Int
        protected get() = 0

    protected override val fragmentClass: Class<out Fragment>?
        protected get() {
            throw RuntimeException("Must be implemented in child classes!")
        }

    protected override val contentLayoutResId: Int
        protected get() = R.layout.activity_fragment_and_toolbar

    protected override val fragmentContentResId: Int
        protected get() = R.id.fragment_container
}