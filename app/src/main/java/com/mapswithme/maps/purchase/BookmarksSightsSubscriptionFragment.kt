package com.mapswithme.maps.purchase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapswithme.maps.R

class BookmarksSightsSubscriptionFragment : AbstractBookmarkSubscriptionFragment() {
    override fun createPurchaseController(): PurchaseController<PurchaseCallback> {
        return PurchaseFactory.createBookmarksSightsSubscriptionController(requireContext())
    }

    override fun createFragmentDelegate(fragment: AbstractBookmarkSubscriptionFragment): SubscriptionFragmentDelegate {
        return TwoButtonsSubscriptionFragmentDelegate(fragment)
    }

    override fun onSubscriptionCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sightseeing_subscription, container, false)
    }

    override val subscriptionType: SubscriptionType
        get() = SubscriptionType.BOOKMARKS_SIGHTS
}