package com.mapswithme.maps.bookmarks

import com.mapswithme.maps.bookmarks.data.PaymentData

interface BookmarkDownloadCallback {
    fun onAuthorizationRequired()
    fun onPaymentRequired(data: PaymentData)
}