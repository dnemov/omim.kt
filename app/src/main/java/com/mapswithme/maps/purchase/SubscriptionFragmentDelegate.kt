package com.mapswithme.maps.purchase

import android.view.View
import androidx.annotation.CallSuper
import com.mapswithme.maps.Framework
import com.mapswithme.maps.R
import com.mapswithme.util.Utils
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName

abstract class SubscriptionFragmentDelegate(val fragment: AbstractBookmarkSubscriptionFragment) {
    abstract fun showButtonProgress()
    abstract fun hideButtonProgress()
    abstract fun onProductDetailsLoading()
    abstract fun onPriceSelection()
    abstract val selectedPeriod: PurchaseUtils.Period
    open fun onReset() { // Do nothing by default.
    }

    @CallSuper
    open fun onCreateView(root: View) {
        val restoreButton =
            root.findViewById<View>(R.id.restore_purchase_btn)
        restoreButton.setOnClickListener { v: View? -> openSubscriptionManagementSettings() }
        val termsOfUse =
            root.findViewById<View>(R.id.term_of_use_link)
        termsOfUse.setOnClickListener { v: View? -> openTermsOfUseLink() }
        val privacyPolicy =
            root.findViewById<View>(R.id.privacy_policy_link)
        privacyPolicy.setOnClickListener { v: View? -> openPrivacyPolicyLink() }
    }

    fun onDestroyView() { // Do nothing by default.
    }

    private fun openSubscriptionManagementSettings() {
        Utils.openUrl(
            fragment.requireContext(),
            "https://play.google.com/store/account/subscriptions"
        )
        Statistics.INSTANCE.trackPurchaseEvent(
            EventName.INAPP_PURCHASE_PREVIEW_RESTORE,
            fragment.subscriptionType.serverId
        )
    }

    private fun openTermsOfUseLink() {
        Utils.openUrl(
            fragment.requireContext(),
            Framework.nativeGetTermsOfUseLink()
        )
    }

    private fun openPrivacyPolicyLink() {
        Utils.openUrl(
            fragment.requireContext(),
            Framework.nativeGetPrivacyPolicyLink()
        )
    }

}