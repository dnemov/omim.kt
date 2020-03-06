package com.mapswithme.maps.purchase

interface CoreValidationObserver {
    fun onValidatePurchase(
        status: ValidationStatus, serverId: String,
        vendorId: String, purchaseData: String
    )
}