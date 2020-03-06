package com.mapswithme.maps.tips

import android.app.Activity
import android.view.View
import com.mapswithme.maps.MwmActivity
import com.mapswithme.util.statistics.Statistics

abstract class TutorialClickListener(
    private val mActivity: Activity,
    private val mTutorial: Tutorial
) :
    View.OnClickListener {
    override fun onClick(v: View) {
        val tutorial = Tutorial.requestCurrent(mActivity, mActivity.javaClass)
        if (tutorial === mTutorial) {
            val mwmActivity = mActivity as MwmActivity
            val interceptor = tutorial.createClickInterceptor()
            interceptor.onInterceptClick(mwmActivity)
            Statistics.INSTANCE.trackTipsEvent(
                Statistics.EventName.TIPS_TRICKS_CLICK,
                tutorial.ordinal
            )
            return
        }
        onProcessClick(v)
    }

    abstract fun onProcessClick(view: View)

}