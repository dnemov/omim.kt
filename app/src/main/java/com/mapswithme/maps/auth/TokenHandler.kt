package com.mapswithme.maps.auth

import android.content.Intent
import com.mapswithme.maps.Framework.AuthTokenType

internal interface TokenHandler {
    fun checkToken(requestCode: Int, data: Intent): Boolean
    val token: String?
    @get:AuthTokenType
    val type: Int
}