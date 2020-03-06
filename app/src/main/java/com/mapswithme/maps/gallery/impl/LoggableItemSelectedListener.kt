package com.mapswithme.maps.gallery.impl

import android.app.Activity
import com.mapswithme.maps.discovery.ItemType
import com.mapswithme.maps.gallery.Items

abstract class LoggableItemSelectedListener<I : Items.Item?>(
    context: Activity,
    protected val type: ItemType
) : BaseItemSelectedListener<I>(context) {
    override fun onMoreItemSelected(item: I) {
        super.onMoreItemSelected(item)
        onMoreItemSelectedInternal(item)
        type.moreClickEvent.log()
    }

    override fun onItemSelected(item: I, position: Int) {
        super.onItemSelected(item, position)
        onItemSelectedInternal(item, position)
        type.itemClickEvent.log()
    }

    protected abstract fun onMoreItemSelectedInternal(item: I)
    protected abstract fun onItemSelectedInternal(item: I, position: Int)

}