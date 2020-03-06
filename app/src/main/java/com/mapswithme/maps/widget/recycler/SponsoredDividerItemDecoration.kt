package com.mapswithme.maps.widget.recycler

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView

internal class SponsoredDividerItemDecoration
/**
 * Creates a divider [RecyclerView.ItemDecoration] that can be used with a
 * [LinearLayoutManager].
 *
 * @param context     Current context, it will be used to access resources.
 * @param orientation Divider orientation. Should be [.HORIZONTAL] or [.VERTICAL].
 */(context: Context?, orientation: Int) :
    DividerItemDecoration(context, orientation) {
    private var mDividerWidth = 0
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        // First element.
        if (parent.getChildAdapterPosition(view) == 0) outRect.left = mDividerWidth
    }

    override fun setDrawable(drawable: Drawable) {
        super.setDrawable(drawable)
        mDividerWidth = drawable.intrinsicWidth
    }
}