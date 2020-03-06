package com.mapswithme.maps.base

import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.mapswithme.util.Config
import com.mapswithme.util.CrashlyticsUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.ViewServer
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.Statistics

open class BaseActivityDelegate(private val mActivity: BaseActivity) {
    private var mThemeName: String? = null
    fun onNewIntent(intent: Intent) {
        logLifecycleMethod("onNewIntent($intent)")
    }

    fun onCreate() {
        logLifecycleMethod("onCreate()")
        mThemeName = Config.getCurrentUiTheme()
        if (!TextUtils.isEmpty(mThemeName)) mActivity.get().setTheme(
            mActivity.getThemeResourceId(
                mThemeName!!
            )
        )
    }

    fun onSafeCreate() {
        logLifecycleMethod("onSafeCreate()")
    }

    fun onSafeDestroy() {
        logLifecycleMethod("onSafeDestroy()")
    }

    fun onDestroy() {
        logLifecycleMethod("onDestroy()")
        ViewServer.get(mActivity.get())?.removeWindow(mActivity.get())
    }

    fun onPostCreate() {
        logLifecycleMethod("onPostCreate()")
        ViewServer.get(mActivity.get())?.addWindow(mActivity.get())
    }

    fun onStart() {
        logLifecycleMethod("onStart()")
        Statistics.INSTANCE.startActivity(mActivity.get())
    }

    fun onStop() {
        logLifecycleMethod("onStop()")
        Statistics.INSTANCE.stopActivity(mActivity.get())
    }

    fun onResume() {
        logLifecycleMethod("onResume()")
        org.alohalytics.Statistics.logEvent(
            "\$onResume", mActivity.javaClass.simpleName + ":" +
                    UiUtils.deviceOrientationAsString(mActivity.get())
        )
        ViewServer.get(mActivity.get())?.setFocusedWindow(mActivity.get())
    }

    fun onPause() {
        logLifecycleMethod("onPause()")
        org.alohalytics.Statistics.logEvent("\$onPause", mActivity.javaClass.simpleName)
    }

    fun onPostResume() {
        logLifecycleMethod("onPostResume()")
        if (!TextUtils.isEmpty(mThemeName) && mThemeName == Config.getCurrentUiTheme()) return
        // Workaround described in https://code.google.com/p/android/issues/detail?id=93731
        UiThread.runLater(Runnable { mActivity.get().recreate() })
    }

    private fun logLifecycleMethod(method: String) {
        val msg =
            mActivity.javaClass.simpleName + ": " + method + " activity: " + mActivity
        CrashlyticsUtils.log(Log.INFO, TAG, msg)
        LOGGER.i(TAG, msg)
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = BaseActivityDelegate::class.java.simpleName
    }

}