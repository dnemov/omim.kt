package com.mapswithme.maps.purchase

import com.mapswithme.maps.R
import com.mapswithme.maps.dialog.AlertDialog
import com.mapswithme.util.UiUtils

enum class BookmarkPaymentState {
    NONE {
        override fun activate(fragment: BookmarkPaymentFragment) {
            throw UnsupportedOperationException("This state can't be used!")
        }
    },
    PRODUCT_DETAILS_LOADING {
        override fun activate(fragment: BookmarkPaymentFragment) {
            showProgress(fragment)
        }
    },
    PRODUCT_DETAILS_LOADED {
        override fun activate(fragment: BookmarkPaymentFragment) {
            hideProgress(fragment)
            fragment.updateProductDetails()
        }
    },
    TRANSACTION_STARTING {
        override fun activate(fragment: BookmarkPaymentFragment) {
            showProgress(fragment)
        }
    },
    TRANSACTION_FAILURE {
        override fun activate(fragment: BookmarkPaymentFragment) {
            UiUtils.hide(fragment.viewOrThrow, R.id.progress)
            val alertDialog =
                AlertDialog.Builder()
                    .setReqCode(PurchaseUtils.REQ_CODE_START_TRANSACTION_FAILURE)
                    .setTitleId(R.string.error_server_title)
                    .setMessageId(R.string.error_server_message)
                    .setPositiveBtnId(R.string.ok)
                    .build()
            alertDialog.show(fragment, name)
        }
    },
    TRANSACTION_STARTED {
        override fun activate(fragment: BookmarkPaymentFragment) {
            hideProgress(fragment)
            fragment.launchBillingFlow()
        }
    },
    PAYMENT_IN_PROGRESS {
        override fun activate(fragment: BookmarkPaymentFragment) { // Do nothing by default.
        }
    },
    PAYMENT_FAILURE {
        override fun activate(fragment: BookmarkPaymentFragment) {
            PurchaseUtils.showPaymentFailureDialog(fragment, name)
        }
    },
    VALIDATION {
        override fun activate(fragment: BookmarkPaymentFragment) {
            showProgress(fragment)
            UiUtils.hide(fragment.viewOrThrow, R.id.cancel_btn)
        }
    },
    VALIDATION_FINISH {
        override fun activate(fragment: BookmarkPaymentFragment) {
            hideProgress(fragment)
            fragment.finishValidation()
        }
    },
    PRODUCT_DETAILS_FAILURE {
        override fun activate(fragment: BookmarkPaymentFragment) {
            PurchaseUtils.showProductDetailsFailureDialog(fragment, name)
        }
    },
    SUBS_PRODUCT_DETAILS_FAILURE {
        override fun activate(fragment: BookmarkPaymentFragment) {
            UiUtils.hide(fragment.viewOrThrow, R.id.buy_subs_container)
            PurchaseUtils.showProductDetailsFailureDialog(fragment, name)
        }
    },
    SUBS_PRODUCT_DETAILS_LOADED {
        override fun activate(fragment: BookmarkPaymentFragment) {
            UiUtils.hide(fragment.viewOrThrow, R.id.subs_progress)
            fragment.updateSubsProductDetails()
        }
    };

    abstract fun activate(fragment: BookmarkPaymentFragment)

    companion object {
        private fun showProgress(fragment: BookmarkPaymentFragment) {
            UiUtils.show(fragment.viewOrThrow, R.id.inapp_progress)
            UiUtils.hide(fragment.viewOrThrow, R.id.buy_inapp_btn)
        }

        private fun hideProgress(fragment: BookmarkPaymentFragment) {
            UiUtils.hide(fragment.viewOrThrow, R.id.inapp_progress)
            UiUtils.show(fragment.viewOrThrow, R.id.buy_inapp_btn)
        }
    }
}