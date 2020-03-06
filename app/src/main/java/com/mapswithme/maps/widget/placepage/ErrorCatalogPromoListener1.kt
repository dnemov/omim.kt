package com.mapswithme.maps.widget.placepage

import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.gallery.ItemSelectedListener
import com.mapswithme.maps.gallery.Items
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.NetworkPolicy
import com.mapswithme.util.NetworkPolicy.NetworkPolicyListener
import com.mapswithme.util.Utils

class ErrorCatalogPromoListener<T : Items.Item?>(
    protected val activity: FragmentActivity,
    private val mListener: NetworkPolicyListener
) : ItemSelectedListener<T> {
    override fun onMoreItemSelected(item: T) {}
    override fun onActionButtonSelected(item: T, position: Int) {}
    override fun onItemSelected(item: T, position: Int) {
        if (ConnectionState.isConnected) NetworkPolicy.checkNetworkPolicy(
            activity.supportFragmentManager,
            mListener,
            true
        ) else Utils.showSystemSettings(activity)
    }

}