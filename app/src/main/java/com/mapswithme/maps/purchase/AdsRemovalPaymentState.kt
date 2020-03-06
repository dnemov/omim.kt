package com.mapswithme.maps.purchase

import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import com.mapswithme.maps.R

import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics

enum class AdsRemovalPaymentState {
    NONE {
        override fun activate(dialog: AdsRemovalPurchaseDialog) {
            throw UnsupportedOperationException("This state can't be used!")
        }
    },
    LOADING {
        override fun activate(dialog: AdsRemovalPurchaseDialog) {
            val view =
                getDialogViewOrThrow(dialog)
            UiUtils.hide(
                view, R.id.title, R.id.image, R.id.pay_button_container, R.id.explanation,
                R.id.explanation_items
            )
            val progressLayout =
                view.findViewById<View>(R.id.progress_layout)
            val message = progressLayout.findViewById<TextView>(R.id.message)
            message.setText(R.string.purchase_loading)
            UiUtils.show(progressLayout)
            dialog.queryPurchaseDetails()
        }
    },
    PRICE_SELECTION {
        override fun activate(dialog: AdsRemovalPurchaseDialog) {
            val view =
                getDialogViewOrThrow(dialog)
            UiUtils.hide(view, R.id.progress_layout, R.id.explanation_items)
            UiUtils.show(view, R.id.title, R.id.image, R.id.pay_button_container, R.id.explanation)
            val title = view.findViewById<TextView>(R.id.title)
            title.setText(R.string.remove_ads_title)
            val image = view.findViewById<View>(R.id.image)
            alignPayButtonBelow(
                view,
                if (image == null) R.id.title else R.id.image
            )
            dialog.updatePaymentButtons()
            Statistics.INSTANCE.trackPurchasePreviewShow(
                SubscriptionType.ADS_REMOVAL.serverId,
                SubscriptionType.ADS_REMOVAL.vendor,
                SubscriptionType.ADS_REMOVAL.yearlyProductId
            )
        }
    },
    EXPLANATION {
        override fun activate(dialog: AdsRemovalPurchaseDialog) {
            val view =
                getDialogViewOrThrow(dialog)
            UiUtils.hide(view, R.id.image, R.id.explanation, R.id.progress_layout)
            UiUtils.show(view, R.id.title, R.id.explanation_items, R.id.pay_button_container)
            val title = view.findViewById<TextView>(R.id.title)
            title.setText(R.string.why_support)
            alignPayButtonBelow(view, R.id.explanation_items)
            dialog.updatePaymentButtons()
        }
    },
    VALIDATION {
        override fun activate(dialog: AdsRemovalPurchaseDialog) {
            val view =
                getDialogViewOrThrow(dialog)
            UiUtils.hide(
                view, R.id.title, R.id.image, R.id.pay_button_container, R.id.explanation,
                R.id.explanation_items
            )
            val progressLayout =
                view.findViewById<View>(R.id.progress_layout)
            val message = progressLayout.findViewById<TextView>(R.id.message)
            message.setText(R.string.please_wait)
            UiUtils.show(progressLayout)
        }
    },
    PAYMENT_FAILURE {
        override fun activate(dialog: AdsRemovalPurchaseDialog) {
            PurchaseUtils.showPaymentFailureDialog(dialog, name)
        }
    },
    PRODUCT_DETAILS_FAILURE {
        override fun activate(dialog: AdsRemovalPurchaseDialog) {
            PurchaseUtils.showProductDetailsFailureDialog(dialog, name)
        }
    },
    VALIDATION_FINISH {
        override fun activate(dialog: AdsRemovalPurchaseDialog) {
            dialog.finishValidation()
        }
    };

    abstract fun activate(dialog: AdsRemovalPurchaseDialog)

    companion object {
        private fun alignPayButtonBelow(view: View, @IdRes anchor: Int) {
            val payButton =
                view.findViewById<View>(R.id.pay_button_container)
            val params =
                payButton.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.BELOW, anchor)
        }

        private fun getDialogViewOrThrow(dialog: DialogFragment): View {
            return dialog.view
                ?: throw IllegalStateException("Before call this method make sure that the dialog exists")
        }
    }
}