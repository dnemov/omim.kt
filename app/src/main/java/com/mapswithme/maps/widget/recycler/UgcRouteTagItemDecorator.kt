package com.mapswithme.maps.widget.recycler

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager

class UgcRouteTagItemDecorator(divider: Drawable) :
    TagItemDecoration(divider) {
    private var mCurrentOffset = 0
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        if (hasSpaceFromRight(
                outRect,
                view,
                parent
            )
        ) mCurrentOffset += view.width + outRect.left else mCurrentOffset = 0
        outRect.left = if (mCurrentOffset == 0) 0 else divider.intrinsicWidth / 2
        outRect.right = divider.intrinsicWidth / 2
        val flexboxLayoutManager =
            parent.layoutManager as FlexboxLayoutManager?
        val isFirstLine =
            isFirstLineItem(view, parent, flexboxLayoutManager!!)
        if (isFirstLine) outRect.top = 0
    }

    private fun hasSpaceFromRight(
        outRect: Rect, view: View,
        parent: RecyclerView
    ): Boolean {
        val padding = parent.paddingLeft + parent.right
        return mCurrentOffset + view.width + outRect.left < parent.width - padding
    }

    companion object {
        private fun isFirstLineItem(
            view: View, parent: RecyclerView,
            layoutManager: FlexboxLayoutManager
        ): Boolean {
            val flexLines = layoutManager.flexLines
            if (flexLines == null || flexLines.isEmpty()) return true
            val flexLine = flexLines.iterator().next()
            val position = parent.layoutManager!!.getPosition(view)
            val itemCount = flexLine.itemCount
            return position < itemCount
        }
    }
}