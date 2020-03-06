package com.mapswithme.maps.purchase

enum class BookmarkSubscriptionPaymentState {
    NONE {
        override fun activate(listener: SubscriptionUiChangeListener) {
            listener.onReset()
        }
    },
    PRODUCT_DETAILS_LOADING {
        override fun activate(listener: SubscriptionUiChangeListener) {
            listener.onProductDetailsLoading()
        }
    },
    PRODUCT_DETAILS_FAILURE {
        override fun activate(listener: SubscriptionUiChangeListener) {
            listener.onProductDetailsFailure()
        }
    },
    PAYMENT_FAILURE {
        override fun activate(listener: SubscriptionUiChangeListener) {
            listener.onPaymentFailure()
        }
    },
    PRICE_SELECTION {
        override fun activate(listener: SubscriptionUiChangeListener) {
            listener.onPriceSelection()
        }
    },
    VALIDATION {
        override fun activate(listener: SubscriptionUiChangeListener) {
            listener.onValidating()
        }
    },
    VALIDATION_FINISH {
        override fun activate(listener: SubscriptionUiChangeListener) {
            listener.onValidationFinish()
        }
    },
    PINGING {
        override fun activate(listener: SubscriptionUiChangeListener) {
            listener.onPinging()
        }
    },
    PINGING_FINISH {
        override fun activate(listener: SubscriptionUiChangeListener) {
            listener.onPingFinish()
        }
    },
    CHECK_NETWORK_CONNECTION {
        override fun activate(listener: SubscriptionUiChangeListener) {
            listener.onCheckNetworkConnection()
        }
    };

    abstract fun activate(listener: SubscriptionUiChangeListener)
}