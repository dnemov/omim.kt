package com.mapswithme.maps.background

interface NotificationChannelProvider {
    val uGCChannel: String
    fun setUGCChannel()
    val downloadingChannel: String
    fun setDownloadingChannel()
}