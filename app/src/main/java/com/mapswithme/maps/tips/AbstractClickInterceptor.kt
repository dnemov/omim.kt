package com.mapswithme.maps.tips

import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.metrics.UserActionsLogger.logTipClickedEvent

abstract class AbstractClickInterceptor internal constructor(val type: Tutorial) :
    ClickInterceptor {

    override fun onInterceptClick(activity: MwmActivity) {
        logTipClickedEvent(type, TutorialAction.ACTION_CLICKED)
        onInterceptClickInternal(activity)
    }

    abstract fun onInterceptClickInternal(activity: MwmActivity)

}