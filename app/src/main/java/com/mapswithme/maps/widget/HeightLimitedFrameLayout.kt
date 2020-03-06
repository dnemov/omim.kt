package com.mapswithme.maps.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils
import java.util.*

/**
 * FrameLayout which presses out the children which can not be fully fit by height.<br></br>
 * Child views should be marked with `@string/tag_height_limited` tag.
 */
class HeightLimitedFrameLayout(
    context: Context?,
    attrs: AttributeSet?
) : FrameLayout(context!!, attrs) {
    private var mTag: String? = null
    private val mLimitedViews: MutableList<View> =
        ArrayList()

    private fun collectViews(v: View) {
        if (mTag == null) mTag = MwmApplication.get().getString(R.string.tag_height_limited)
        if (mTag == v.tag) {
            mLimitedViews.add(v)
            return
        }
        if (v is ViewGroup) {
            val vg = v
            for (i in 0 until vg.childCount) collectViews(vg.getChildAt(i))
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (!isInEditMode) collectViews(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isInEditMode) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        for (v in mLimitedViews) UiUtils.show(v)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        if (measuredHeight > MeasureSpec.getSize(heightMeasureSpec)) for (v in mLimitedViews) UiUtils.hide(
            v
        )
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}