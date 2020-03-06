package com.mapswithme.maps.purchase

interface CoreStartTransactionObserver {
    fun onStartTransaction(
        success: Boolean,
        serverId: String,
        vendorId: String
    )
}