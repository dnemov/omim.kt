package com.mapswithme.maps.ads

import android.os.SystemClock
import com.mapswithme.util.log.LoggerFactory
import java.util.*

class DefaultAdTracker : AdTracker, OnAdCacheModifiedListener {
    override fun onViewShown(provider: String, bannerId: String) {
        val key = BannerKey(provider, bannerId)
        LOGGER.d(
            TAG,
            "onViewShown bannerId = $key"
        )
        var info =
            TRACKS[key]
        if (info == null) {
            info = TrackInfo()
            TRACKS[key] = info
        }
        info.isVisible = true
    }

    override fun onViewHidden(
        provider: String,
        bannerId: String
    ) {
        val key = BannerKey(provider, bannerId)
        LOGGER.d(
            TAG,
            "onViewHidden bannerId = $key"
        )
        val info =
            TRACKS[key]
        if (info != null) info.isVisible = false
    }

    override fun onContentObtained(
        provider: String,
        bannerId: String
    ) {
        val key = BannerKey(provider, bannerId)
        LOGGER.d(
            TAG,
            "onContentObtained bannerId = $key"
        )
        val info =
            TRACKS[key]
                ?: throw AssertionError("A track info must be put in a cache before a content is obtained")
        info.fill()
    }

    override fun isImpressionGood(
        provider: String,
        bannerId: String
    ): Boolean {
        val key = BannerKey(provider, bannerId)
        val info =
            TRACKS[key]
        return info != null && info.showTime > IMPRESSION_TIME_MS
    }

    override fun onRemoved(key: BannerKey) {
        TRACKS.remove(key)
    }

    override fun onPut(key: BannerKey) {
        val info =
            TRACKS[key]
        if (info == null) {
            TRACKS[key] = TrackInfo()
            return
        }
        if (info.showTime != 0L) info.setLastShow(true)
    }

    private class TrackInfo {
        /**
         * A timestamp to track ad visibility
         */
        private var mTimestamp: Long = 0
        /**
         * Accumulates amount of time that ad is already shown.
         */
        var showTime: Long = 0
            private set
        /**
         * Indicates whether the ad view is visible or not.
         */
        private var mVisible = false
        /**
         * Indicates whether the ad content is obtained or not.
         */
        private var mFilled = false
        /**
         * Indicates whether it's the last time when an ad was shown or not.
         */
        private var mLastShow = false

        // No need tracking if the ad is not filled with a content
        // If ad becomes visible, and it's filled with a content the timestamp must be stored.
        var isVisible: Boolean
            get() = mVisible
            set(visible) {
                val wasVisible = mVisible
                mVisible = visible
                // No need tracking if the ad is not filled with a content
                if (!mFilled) return
                // If ad becomes visible, and it's filled with a content the timestamp must be stored.
                if (visible && !wasVisible) {
                    mTimestamp = SystemClock.elapsedRealtime()
                } else if (!visible && wasVisible) {
                    if (mLastShow) {
                        showTime = 0
                        mTimestamp = 0
                        mLastShow = false
                        LOGGER.d(
                            TAG,
                            "it's a last time for this ad"
                        )
                        return
                    }
                    if (mTimestamp == 0L) throw AssertionError("A timestamp mustn't be 0 when ad is hidden!")
                    showTime += SystemClock.elapsedRealtime() - mTimestamp
                    LOGGER.d(
                        TAG,
                        "A show time = " + showTime
                    )
                    mTimestamp = 0
                }
            }

        fun fill() { // If the visible ad is filled with the content the timestamp must be stored
            if (mVisible) mTimestamp = SystemClock.elapsedRealtime()
            mFilled = true
        }

        fun setLastShow(lastShow: Boolean) {
            mLastShow = lastShow
        }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = DefaultAdTracker::class.java.simpleName
        private const val IMPRESSION_TIME_MS = 2500
        private val TRACKS: MutableMap<BannerKey, TrackInfo> =
            HashMap()
    }
}