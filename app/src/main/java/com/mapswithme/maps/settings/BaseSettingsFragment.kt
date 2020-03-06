package com.mapswithme.maps.settings

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.mapswithme.maps.base.BaseMwmFragment

abstract class BaseSettingsFragment : BaseMwmFragment() {
    private var mFrame: View? = null
    private val mSavedPaddings = Rect()
    @get:LayoutRes
    protected abstract val layoutRes: Int

    private fun savePaddings() {
        val parent = mFrame!!.parent as View
        mSavedPaddings[parent.paddingLeft, parent.paddingTop, parent.paddingRight] =
            parent.paddingBottom
    }

    protected fun clearPaddings() {
        (mFrame!!.parent as View).setPadding(0, 0, 0, 0)
    }

    protected fun restorePaddings() {
        (mFrame!!.parent as View).setPadding(
            mSavedPaddings.left,
            mSavedPaddings.top,
            mSavedPaddings.right,
            mSavedPaddings.bottom
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutRes, container, false).also { mFrame = it }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        savePaddings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        restorePaddings()
    }

    protected val settingsActivity: SettingsActivity?
        protected get() = activity as SettingsActivity?
}