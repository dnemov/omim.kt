package com.mapswithme.maps.purchase

import android.view.View
import androidx.annotation.CallSuper
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.SubscriptionButton
import com.mapswithme.util.Utils

internal class TwoButtonsSubscriptionFragmentDelegate(fragment: AbstractBookmarkSubscriptionFragment) :
    SubscriptionFragmentDelegate(fragment) {
    private lateinit var mAnnualButton: SubscriptionButton
    private lateinit var mMonthlyButton: SubscriptionButton
    override var selectedPeriod =
        PurchaseUtils.Period.P1Y
        private set

    @CallSuper
    public override fun onCreateView(root: View) {
        super.onCreateView(root)
        mAnnualButton = root.findViewById(R.id.annual_button)
        mAnnualButton.setOnClickListener { v: View? ->
            onSubscriptionButtonClicked(
                PurchaseUtils.Period.P1Y
            )
        }
        mMonthlyButton = root.findViewById(R.id.monthly_button)
        mMonthlyButton.setOnClickListener { v: View? ->
            onSubscriptionButtonClicked(
                PurchaseUtils.Period.P1M
            )
        }
    }

    private fun onSubscriptionButtonClicked(period: PurchaseUtils.Period) {
        val productDetails = fragment.productDetails
        if (productDetails == null || productDetails.isEmpty()) return
        selectedPeriod = period
        fragment.pingBookmarkCatalog()
    }

    public override fun onProductDetailsLoading() {
        mAnnualButton.showProgress()
        mMonthlyButton.showProgress()
    }

    public override fun onPriceSelection() {
        mAnnualButton.hideProgress()
        mMonthlyButton.hideProgress()
        updatePaymentButtons()
    }

    private fun updatePaymentButtons() {
        updateYearlyButton()
        updateMonthlyButton()
    }

    private fun updateMonthlyButton() {
        val details =
            fragment.getProductDetailsForPeriod(PurchaseUtils.Period.P1Y)
        val price =
            Utils.formatCurrencyString(details.price, details.currencyCode)
        mAnnualButton.setPrice(price)
        mAnnualButton.setName(fragment.getString(R.string.annual_subscription_title))
        mAnnualButton.setSale(fragment.getString(R.string.all_pass_screen_best_value))
    }

    private fun updateYearlyButton() {
        val details =
            fragment.getProductDetailsForPeriod(PurchaseUtils.Period.P1M)
        val price =
            Utils.formatCurrencyString(details.price, details.currencyCode)
        mMonthlyButton.setPrice(price)
        mMonthlyButton.setName(fragment.getString(R.string.montly_subscription_title))
    }

    public override fun showButtonProgress() {
        if (selectedPeriod == PurchaseUtils.Period.P1Y) mAnnualButton.showProgress() else mMonthlyButton.showProgress()
    }

    public override fun hideButtonProgress() {
        if (selectedPeriod == PurchaseUtils.Period.P1Y) mAnnualButton.hideProgress() else mMonthlyButton.hideProgress()
    }
}