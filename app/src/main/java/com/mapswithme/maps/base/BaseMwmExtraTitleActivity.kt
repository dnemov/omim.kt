package com.mapswithme.maps.base

import android.os.Bundle
import androidx.annotation.CallSuper
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils

abstract class BaseMwmExtraTitleActivity : BaseMwmFragmentActivity() {
    @CallSuper
    override fun onSafeCreate(savedInstanceState: Bundle?) {
        super.onSafeCreate(savedInstanceState)
        val bundle = intent.extras
        val title = bundle?.getString(EXTRA_TITLE) ?: ""
        val toolbar = toolbar
        UiUtils.extendViewWithStatusBar(toolbar!!)
        toolbar.title = title
        UiUtils.showHomeUpButton(toolbar)
        displayToolbarAsActionBar()
    }

    override val contentLayoutResId: Int
        get() = R.layout.activity_fragment_and_toolbar

    override val fragmentContentResId: Int
        get() = R.id.fragment_container

    companion object {

        const val EXTRA_TITLE = "activity_title"
    }
}