package com.mapswithme.maps.onboarding

import android.app.Activity

interface OnIntroductionButtonClickListener {
    fun onIntroductionButtonClick(activity: Activity, deeplink: String)
}