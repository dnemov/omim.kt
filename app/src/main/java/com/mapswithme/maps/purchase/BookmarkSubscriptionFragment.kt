package com.mapswithme.maps.purchase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapswithme.maps.R

class BookmarkSubscriptionFragment : AbstractBookmarkSubscriptionFragment() {
    override fun onSubscriptionCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bookmark_subscription_fragment, container, false)
    }

    override val subscriptionType: SubscriptionType
        get() = SubscriptionType.BOOKMARKS_ALL

    override fun createPurchaseController(): PurchaseController<PurchaseCallback> {
        return PurchaseFactory.createBookmarksAllSubscriptionController(requireContext())
    }

    override fun createFragmentDelegate(fragment: AbstractBookmarkSubscriptionFragment): SubscriptionFragmentDelegate {
        return TwoCardsSubscriptionFragmentDelegate(fragment)
    }
}