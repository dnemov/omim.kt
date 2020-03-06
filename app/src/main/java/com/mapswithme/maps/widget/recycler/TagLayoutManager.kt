package com.mapswithme.maps.widget.recycler

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler

/**
 * This LayoutManager designed only for use with RecyclerView.setNestedScrollingEnabled(false)
 * and recycle item must be wrap_content or fixed size
 */
class TagLayoutManager : RecyclerView.LayoutManager() {
    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onLayoutChildren(
        recycler: Recycler,
        state: RecyclerView.State
    ) {
        detachAndScrapAttachedViews(recycler)
        var widthUsed = 0
        var heightUsed = 0
        var lineHeight = 0
        var itemsCountOneLine = 0
        for (i in 0 until itemCount) {
            val child = recycler.getViewForPosition(i)
            addView(child)
            measureChildWithMargins(child, widthUsed, heightUsed)
            var width = getDecoratedMeasuredWidth(child)
            var height = getDecoratedMeasuredHeight(child)
            lineHeight = Math.max(lineHeight, height)
            if (widthUsed + width >= getWidth()) {
                widthUsed = 0
                if (itemsCountOneLine > 0) {
                    itemsCountOneLine = -1
                    heightUsed += lineHeight
                    child.forceLayout()
                    measureChildWithMargins(child, widthUsed, heightUsed)
                    width = getDecoratedMeasuredWidth(child)
                    height = getDecoratedMeasuredHeight(child)
                }
                lineHeight = 0
            }
            layoutDecorated(child, widthUsed, heightUsed, widthUsed + width, heightUsed + height)
            widthUsed += width
            itemsCountOneLine++
        }
    }

    init {
        isAutoMeasureEnabled = true
    }
}