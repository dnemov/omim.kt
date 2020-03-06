package com.mapswithme.maps.gallery.impl

import android.app.Activity
import com.mapswithme.maps.gallery.ItemSelectedListener
import com.mapswithme.maps.gallery.Items
import com.mapswithme.util.Utils

open class BaseItemSelectedListener<I : Items.Item?>(protected val context: Activity) :
    ItemSelectedListener<I> {

    override fun onItemSelected(item: I, position: Int) {
        openUrl(item)
    }

    override fun onMoreItemSelected(item: I) {
        openUrl(item)
    }

    protected open fun openUrl(item: I) {
        Utils.openUrl(context, item!!.url)
    }

    override fun onActionButtonSelected(item: I, position: Int) {
        openUrl(item)
    }

}