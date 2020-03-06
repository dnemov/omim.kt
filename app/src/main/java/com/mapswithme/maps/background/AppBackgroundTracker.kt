package com.mapswithme.maps.background

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.SparseArray
import androidx.annotation.UiThread
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.Listeners
import com.mapswithme.util.log.LoggerFactory
import java.lang.ref.WeakReference

/**
 * Helper class that detects when the application goes to background and back to foreground.
 * <br></br>Must be created as early as possible, i.e. in Application.onCreate().
 */
class AppBackgroundTracker {
    private val mTransitionListeners =
        Listeners<OnTransitionListener>()
    private val mVisibleAppLaunchListeners =
        Listeners<OnVisibleAppLaunchListener>()
    private var mActivities =
        SparseArray<WeakReference<Activity>>()
    @Volatile
    var isForeground = false
        private set
    private val mTransitionProc: Runnable = object : Runnable {
        override fun run() {
            val newArray =
                SparseArray<WeakReference<Activity>>()
            for (i in 0 until mActivities.size()) {
                val key = mActivities.keyAt(i)
                val ref = mActivities[key]
                val activity = ref.get()
                if (activity != null && !activity.isFinishing) newArray.put(key, ref)
            }
            mActivities = newArray
            val old: Boolean = isForeground
            isForeground = mActivities.size() > 0
            if (isForeground != old) notifyTransitionListeners()
        }
    }
    /** @noinspection FieldCanBeLocal
     */
    private val mAppLifecycleCallbacks: ActivityLifecycleCallbacks =
        object : ActivityLifecycleCallbacks {
            private fun onActivityChanged() {
                com.mapswithme.util.concurrency.UiThread.cancelDelayedTasks(mTransitionProc)
                com.mapswithme.util.concurrency.UiThread.runLater(
                    mTransitionProc,
                    TRANSITION_DELAY_MS.toLong()
                )
            }

            override fun onActivityStarted(activity: Activity) {
                LOGGER.d(
                    TAG,
                    "onActivityStarted activity = $activity"
                )
                if (mActivities.size() == 0) notifyVisibleAppLaunchListeners()
                mActivities.put(
                    activity.hashCode(),
                    WeakReference(activity)
                )
                onActivityChanged()
            }

            override fun onActivityStopped(activity: Activity) {
                LOGGER.d(
                    TAG,
                    "onActivityStopped activity = $activity"
                )
                mActivities.remove(activity.hashCode())
                onActivityChanged()
            }

            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: Bundle?
            ) {
                LOGGER.d(
                    TAG,
                    "onActivityCreated activity = $activity"
                )
            }

            override fun onActivityDestroyed(activity: Activity) {
                LOGGER.d(
                    TAG,
                    "onActivityDestroyed activity = $activity"
                )
            }

            override fun onActivityResumed(activity: Activity) {
                LOGGER.d(
                    TAG,
                    "onActivityResumed activity = $activity"
                )
            }

            override fun onActivityPaused(activity: Activity) {
                LOGGER.d(
                    TAG,
                    "onActivityPaused activity = $activity"
                )
            }

            override fun onActivitySaveInstanceState(
                activity: Activity,
                outState: Bundle
            ) {
                LOGGER.d(
                    TAG,
                    "onActivitySaveInstanceState activity = $activity"
                )
            }
        }

    interface OnTransitionListener {
        fun onTransit(foreground: Boolean)
    }

    interface OnVisibleAppLaunchListener {
        fun onVisibleAppLaunch()
    }

    private fun notifyTransitionListeners() {
        for (listener in mTransitionListeners) listener.onTransit(isForeground)
        mTransitionListeners.finishIterate()
    }

    private fun notifyVisibleAppLaunchListeners() {
        for (listener in mVisibleAppLaunchListeners) listener.onVisibleAppLaunch()
        mVisibleAppLaunchListeners.finishIterate()
    }

    fun addListener(listener: OnTransitionListener) {
        mTransitionListeners.register(listener)
    }

    fun removeListener(listener: OnTransitionListener) {
        mTransitionListeners.unregister(listener)
    }

    fun addListener(listener: OnVisibleAppLaunchListener) {
        mVisibleAppLaunchListeners.register(listener)
    }

    fun removeListener(listener: OnVisibleAppLaunchListener) {
        mVisibleAppLaunchListeners.unregister(listener)
    }

    @get:UiThread
    val topActivity: Activity?
        get() = if (mActivities.size() == 0) null else mActivities[mActivities.keyAt(0)].get()

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = AppBackgroundTracker::class.java.simpleName
        private const val TRANSITION_DELAY_MS = 1000
    }

    init {
        MwmApplication.get().registerActivityLifecycleCallbacks(mAppLifecycleCallbacks)
    }
}