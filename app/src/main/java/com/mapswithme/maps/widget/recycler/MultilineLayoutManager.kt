package com.mapswithme.maps.widget.recycler

import android.view.View
import androidx.annotation.Dimension
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler

class MultilineLayoutManager : RecyclerView.LayoutManager() {
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
            if (width > getWidth() - widthUsed) width =
                squeezeChildIntoLine(widthUsed, heightUsed, child)
            var height = getDecoratedMeasuredHeight(child)
            lineHeight = Math.max(lineHeight, height)
            if (widthUsed + width > getWidth()) {
                widthUsed = 0
                if (itemsCountOneLine > 0) {
                    itemsCountOneLine = 0
                    heightUsed += lineHeight
                    child.forceLayout()
                    measureChildWithMargins(child, widthUsed, heightUsed)
                    width = getDecoratedMeasuredWidth(child)
                    if (width > getWidth() - widthUsed) width =
                        squeezeChildIntoLine(widthUsed, heightUsed, child)
                    height = getDecoratedMeasuredHeight(child)
                }
                lineHeight = 0
            }
            layoutDecorated(child, widthUsed, heightUsed, widthUsed + width, heightUsed + height)
            widthUsed += width
            itemsCountOneLine++
        }
    }

    private fun squeezeChildIntoLine(
        widthUsed: Int,
        heightUsed: Int,
        child: View
    ): Int {
        if (child !is SqueezingInterface) return getDecoratedMeasuredWidth(child)
        val availableWidth = width - widthUsed - getDecoratedRight(child)
        if (availableWidth > (child as SqueezingInterface).minimumAcceptableSize) {
            (child as SqueezingInterface).squeezeTo(availableWidth)
            child.forceLayout()
            measureChildWithMargins(child, widthUsed, heightUsed)
        }
        return getDecoratedMeasuredWidth(child)
    }

    interface SqueezingInterface {
        fun squeezeTo(@Dimension width: Int)
        @get:Dimension
        val minimumAcceptableSize: Int
    }

    init {
        isAutoMeasureEnabled = true
    }
}