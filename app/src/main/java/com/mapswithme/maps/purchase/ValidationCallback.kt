package com.mapswithme.maps.purchase

interface ValidationCallback {
    fun onValidate(purchaseData: String, status: ValidationStatus)
}