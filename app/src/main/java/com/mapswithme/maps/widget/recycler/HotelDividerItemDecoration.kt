package com.mapswithme.maps.widget.recycler

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView

internal class HotelDividerItemDecoration
/**
 * Creates a divider [RecyclerView.ItemDecoration] that can be used with a
 * [LinearLayoutManager].
 *
 * @param context     Current context, it will be used to access resources.
 * @param orientation Divider orientation. Should be [.HORIZONTAL] or [.VERTICAL].
 */(context: Context?, orientation: Int) :
    DividerItemDecoration(context, orientation) {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val itemCount = state.itemCount
        val itemPosition = parent.getChildAdapterPosition(view)
        // Last position.
        if (itemPosition != RecyclerView.NO_POSITION && itemPosition > 0 && itemCount > 0 && itemPosition == itemCount - 1
        ) {
            outRect[0, 0, 0] = 0
        }
    }
}