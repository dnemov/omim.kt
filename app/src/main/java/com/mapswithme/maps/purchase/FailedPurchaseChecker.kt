package com.mapswithme.maps.purchase

interface FailedPurchaseChecker {
    fun onFailedPurchaseDetected(isDetected: Boolean)
    fun onAuthorizationRequired()
    fun onStoreConnectionFailed()
}