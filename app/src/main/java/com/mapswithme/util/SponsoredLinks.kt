package com.mapswithme.util

import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language

class SponsoredLinks(private val mDeepLink: String, private val mUniversalLink: String) {
    fun getDeepLink(): String {
        return mDeepLink
    }

    fun getUniversalLink(): String {
        return mUniversalLink
    }

}