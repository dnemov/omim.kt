package com.mapswithme.maps.tips

import com.mapswithme.maps.MwmActivity

interface ClickInterceptor {
    fun onInterceptClick(activity: MwmActivity)
}