package com.mapswithme.maps.purchase

import com.mapswithme.maps.bookmarks.data.PaymentData

interface PaymentDataParser {
    fun parse(url: String): PaymentData
    fun getParameterByName(url: String, name: String): String?
}