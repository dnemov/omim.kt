package com.mapswithme.maps.purchase

import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils

class TwoCardsSubscriptionFragmentDelegate internal constructor(fragment: AbstractBookmarkSubscriptionFragment) :
    SubscriptionFragmentDelegate(fragment) {
    public override fun onCreateView(root: View) {
        super.onCreateView(root)
        val annualPriceCard: CardView = root.findViewById(R.id.annual_price_card)
        val monthlyPriceCard: CardView = root.findViewById(R.id.monthly_price_card)
        val annualCardListener = AnnualCardClickListener(
            monthlyPriceCard,
            annualPriceCard
        )
        annualPriceCard.setOnClickListener(annualCardListener)
        val monthlyCardListener = MonthlyCardClickListener(
            monthlyPriceCard,
            annualPriceCard
        )
        monthlyPriceCard.setOnClickListener(monthlyCardListener)
        val continueBtn = root.findViewById<View>(R.id.continue_btn)
        continueBtn.setOnClickListener { v: View? -> onContinueButtonClicked() }
        annualPriceCard.isSelected = true
        monthlyPriceCard.isSelected = false
        annualPriceCard.cardElevation = fragment.resources
            .getDimension(R.dimen.margin_base_plus_quarter)
    }

    private fun updatePaymentButtons() {
        updateYearlyButton()
        updateMonthlyButton()
    }

    private fun updateYearlyButton() {
        val details =
            fragment.getProductDetailsForPeriod(PurchaseUtils.Period.P1Y)
        val price =
            Utils.formatCurrencyString(details.price, details.currencyCode)
        val priceView =
            fragment.viewOrThrow.findViewById<TextView>(R.id.annual_price)
        priceView.text = price
        val savingView = fragment.viewOrThrow.findViewById<TextView>(R.id.sale)
        val text = fragment.getString(
            R.string.annual_save_component,
            fragment.calculateYearlySaving()
        )
        savingView.text = text
    }

    private fun updateMonthlyButton() {
        val details =
            fragment.getProductDetailsForPeriod(PurchaseUtils.Period.P1M)
        val price =
            Utils.formatCurrencyString(details.price, details.currencyCode)
        val priceView =
            fragment.viewOrThrow.findViewById<TextView>(R.id.monthly_price)
        priceView.text = price
    }

    override val selectedPeriod: PurchaseUtils.Period
        get() {
            val annualCard: CardView = fragment.viewOrThrow.findViewById(R.id.annual_price_card)
            return if (annualCard.cardElevation > 0) PurchaseUtils.Period.P1Y else PurchaseUtils.Period.P1M
        }

    public override fun onReset() {
        hideAllUi()
    }

    private fun hideAllUi() {
        UiUtils.hide(fragment.viewOrThrow, R.id.root_screen_progress, R.id.content_view)
    }

    private fun onContinueButtonClicked() {
        fragment.pingBookmarkCatalog()
        fragment.trackPayEvent()
    }

    public override fun showButtonProgress() {
        UiUtils.hide(fragment.viewOrThrow, R.id.continue_btn)
        UiUtils.show(fragment.viewOrThrow, R.id.progress)
    }

    public override fun hideButtonProgress() {
        UiUtils.hide(fragment.viewOrThrow, R.id.progress)
        UiUtils.show(fragment.viewOrThrow, R.id.continue_btn)
    }

    private fun showRootScreenProgress() {
        UiUtils.show(fragment.viewOrThrow, R.id.root_screen_progress)
        UiUtils.hide(fragment.viewOrThrow, R.id.content_view)
    }

    private fun hideRootScreenProgress() {
        UiUtils.hide(fragment.viewOrThrow, R.id.root_screen_progress)
        UiUtils.show(fragment.viewOrThrow, R.id.content_view)
    }

    public override fun onProductDetailsLoading() {
        showRootScreenProgress()
    }

    public override fun onPriceSelection() {
        hideRootScreenProgress()
        updatePaymentButtons()
    }

    private inner class AnnualCardClickListener internal constructor(
        private val mMonthlyPriceCard: CardView,
        private val mAnnualPriceCard: CardView
    ) : View.OnClickListener {
        override fun onClick(v: View) {
            mMonthlyPriceCard.cardElevation = DEF_ELEVATION.toFloat()
            mAnnualPriceCard.cardElevation = fragment.resources
                .getDimension(R.dimen.margin_base_plus_quarter)
            if (!mAnnualPriceCard.isSelected) fragment.trackYearlyProductSelected()
            mMonthlyPriceCard.isSelected = false
            mAnnualPriceCard.isSelected = true
        }

    }

    private inner class MonthlyCardClickListener internal constructor(
        private val mMonthlyPriceCard: CardView,
        private val mAnnualPriceCard: CardView
    ) : View.OnClickListener {
        override fun onClick(v: View) {
            mMonthlyPriceCard.cardElevation = fragment.resources
                .getDimension(R.dimen.margin_base_plus_quarter)
            mAnnualPriceCard.cardElevation = DEF_ELEVATION.toFloat()
            if (!mMonthlyPriceCard.isSelected) fragment.trackMonthlyProductSelected()
            mMonthlyPriceCard.isSelected = true
            mAnnualPriceCard.isSelected = false
        }

    }

    companion object {
        private const val DEF_ELEVATION = 0
    }
}