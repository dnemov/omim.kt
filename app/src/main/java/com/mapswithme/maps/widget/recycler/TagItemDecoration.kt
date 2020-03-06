package com.mapswithme.maps.widget.recycler

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

/**
 * Adds interior dividers to a RecyclerView with a TagLayoutManager or its
 * subclass.
 */
open class TagItemDecoration(protected val divider: Drawable) :
    ItemDecoration() {
    /**
     * Draws horizontal and vertical dividers onto the parent RecyclerView.
     *
     * @param canvas The [Canvas] onto which dividers will be drawn
     * @param parent The RecyclerView onto which dividers are being added
     * @param state  The current RecyclerView.State of the RecyclerView
     */
    override fun onDraw(
        canvas: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (state.isMeasuring) return
        val parentRight = parent.width - parent.paddingRight
        val parentLeft = parent.paddingLeft
        var lastHeight = Int.MIN_VALUE
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            if (child.top <= lastHeight) {
                divider.setBounds(
                    child.left - divider.intrinsicWidth,
                    child.top,
                    child.left,
                    child.bottom
                )
            } else {
                divider.setBounds(
                    parentLeft,
                    child.top - divider.intrinsicHeight,
                    parentRight,
                    child.top
                )
            }
            divider.draw(canvas)
            lastHeight = child.top
        }
    }

    /**
     * Determines the size and location of offsets between items in the parent
     * RecyclerView.
     *
     * @param outRect The [Rect] of offsets to be added around the child
     * view
     * @param view    The child view to be decorated with an offset
     * @param parent  The RecyclerView onto which dividers are being added
     * @param state   The current RecyclerView.State of the RecyclerView
     */
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.left = divider.intrinsicWidth
        outRect.top = divider.intrinsicHeight
    }

}