package com.mapswithme.maps.bookmarks

interface BookmarkDownloadHandler {
    fun onAuthorizationRequired()
    fun onPaymentRequired()
}