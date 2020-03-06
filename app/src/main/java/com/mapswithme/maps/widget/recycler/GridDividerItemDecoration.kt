package com.mapswithme.maps.widget.recycler

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

/**
 * Adds interior dividers to a RecyclerView with a GridLayoutManager.
 */
class GridDividerItemDecoration
/**
 * Sole constructor. Takes in [Drawable] objects to be used as
 * horizontal and vertical dividers.
 *
 * @param horizontalDivider A divider `Drawable` to be drawn on the
 * rows of the grid of the RecyclerView
 * @param verticalDivider   A divider `Drawable` to be drawn on the
 * columns of the grid of the RecyclerView
 * @param numColumns        The number of columns in the grid of the RecyclerView
 */(
    private val mHorizontalDivider: Drawable,
    private val mVerticalDivider: Drawable, private val mNumColumns: Int
) : ItemDecoration() {
    /**
     * Draws horizontal and/or vertical dividers onto the parent RecyclerView.
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
        drawHorizontalDividers(canvas, parent)
        drawVerticalDividers(canvas, parent)
    }

    /**
     * Determines the size and location of offsets between items in the parent
     * RecyclerView.
     *
     * @param outRect The [Rect] of offsets to be added around the child view
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
        val childIsInLeftmostColumn =
            parent.getChildAdapterPosition(view) % mNumColumns == 0
        if (!childIsInLeftmostColumn) outRect.left = mHorizontalDivider.intrinsicWidth
        val childIsInFirstRow = parent.getChildAdapterPosition(view) < mNumColumns
        if (!childIsInFirstRow) outRect.top = mVerticalDivider.intrinsicHeight
    }

    /**
     * Adds horizontal dividers to a RecyclerView with a GridLayoutManager or
     * its subclass.
     *
     * @param canvas The [Canvas] onto which dividers will be drawn
     * @param parent The RecyclerView onto which dividers are being added
     */
    private fun drawHorizontalDividers(
        canvas: Canvas,
        parent: RecyclerView
    ) {
        val parentTop = parent.paddingTop
        val parentBottom = parent.height - parent.paddingBottom
        for (i in 0 until mNumColumns) {
            val child = parent.getChildAt(i)
            val params =
                child.layoutParams as RecyclerView.LayoutParams
            val parentLeft = child.right + params.rightMargin
            val parentRight = parentLeft + mHorizontalDivider.intrinsicWidth
            mHorizontalDivider.setBounds(parentLeft, parentTop, parentRight, parentBottom)
            mHorizontalDivider.draw(canvas)
        }
    }

    /**
     * Adds vertical dividers to a RecyclerView with a GridLayoutManager or its
     * subclass.
     *
     * @param canvas The [Canvas] onto which dividers will be drawn
     * @param parent The RecyclerView onto which dividers are being added
     */
    private fun drawVerticalDividers(
        canvas: Canvas,
        parent: RecyclerView
    ) {
        val parentLeft = parent.paddingLeft
        val parentRight = parent.width - parent.paddingRight
        val childCount = parent.childCount
        val numRows = (childCount + (mNumColumns - 1)) / mNumColumns
        for (i in 0 until numRows - 1) {
            val child = parent.getChildAt(i * mNumColumns)
            val params =
                child.layoutParams as RecyclerView.LayoutParams
            val parentTop = child.bottom + params.bottomMargin
            val parentBottom = parentTop + mVerticalDivider.intrinsicHeight
            mVerticalDivider.setBounds(parentLeft, parentTop, parentRight, parentBottom)
            mVerticalDivider.draw(canvas)
        }
    }

}