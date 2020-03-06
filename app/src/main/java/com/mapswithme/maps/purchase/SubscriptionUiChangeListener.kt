package com.mapswithme.maps.purchase

interface SubscriptionUiChangeListener {
    fun onReset()
    fun onProductDetailsLoading()
    fun onProductDetailsFailure()
    fun onPaymentFailure()
    fun onPriceSelection()
    fun onValidating()
    fun onValidationFinish()
    fun onPinging()
    fun onPingFinish()
    fun onCheckNetworkConnection()
}