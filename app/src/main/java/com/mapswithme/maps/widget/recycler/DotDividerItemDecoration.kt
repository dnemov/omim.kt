package com.mapswithme.maps.widget.recycler

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.Dimension
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class DotDividerItemDecoration(
    private val mDivider: Drawable, @field:Dimension @param:Dimension private val mHorizontalMargin: Int,
    @field:Dimension @param:Dimension private val mVerticalMargin: Int
) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.right = mHorizontalMargin
        outRect.bottom = mVerticalMargin
    }

    override fun onDraw(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (state.isMeasuring) return
        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            val centerX = mHorizontalMargin / 2 + child.right
            val centerY = child.height / 2 + child.top
            val left = centerX - mDivider.intrinsicWidth / 2
            val right = left + mDivider.intrinsicWidth
            val top = centerY - mDivider.intrinsicHeight / 2
            val bottom = top + mDivider.intrinsicHeight
            mDivider.setBounds(left, top, right, bottom)
            mDivider.draw(c)
        }
    }

}