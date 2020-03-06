package com.mapswithme.maps.auth

import android.content.Intent

interface TargetFragmentCallback {
    fun onTargetFragmentResult(resultCode: Int, data: Intent?)
    val isTargetAdded: Boolean
}