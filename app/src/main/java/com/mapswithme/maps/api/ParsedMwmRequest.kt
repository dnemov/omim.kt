package com.mapswithme.maps.api

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.text.TextUtils
import com.mapswithme.maps.Framework

class ParsedMwmRequest
/**
 * Do not use constructor externally. Use [ParsedMwmRequest.extractFromIntent] instead.
 */
private constructor() {
    // Response data
    // caller info
    var callerInfo: ApplicationInfo? = null
        private set
    // title
    var title: String? = null
        private set
    // pending intent to call back
    private var mPendingIntent: PendingIntent? = null
    // return on ballon click
    private var mReturnOnBalloonClick = false
    // pick point mode
    private var mPickPoint = false
    // custom button name
    var customButtonName: String? = null
        private set
    // response data
    private var mHasPoint = false
    var lat = 0.0
        private set
    var lon = 0.0
        private set
    private var mZoomLevel = 0.0
    var name: String? = null
        private set
    private var mId: String? = null

    fun hasCustomButtonName(): Boolean {
        return !TextUtils.isEmpty(customButtonName)
    }

    fun hasTitle(): Boolean {
        return title != null
    }

    // Request data
    fun hasPoint(): Boolean {
        return mHasPoint
    }

    fun setHasPoint(hasPoint: Boolean) {
        mHasPoint = hasPoint
    }

    fun hasPendingIntent(): Boolean {
        return mPendingIntent != null
    }

    fun doReturnOnBalloonClick(): Boolean {
        return mReturnOnBalloonClick
    }

    fun setPointData(
        lat: Double,
        lon: Double,
        name: String?,
        id: String?
    ) {
        this.lat = lat
        this.lon = lon
        this.name = name
        mId = id
    }

    fun getIcon(context: Context): Drawable {
        return context.packageManager.getApplicationIcon(callerInfo)
    }

    fun getCallerName(context: Context): CharSequence {
        return context.packageManager.getApplicationLabel(callerInfo)
    }

    fun sendResponse(context: Context?, success: Boolean): Boolean {
        if (hasPendingIntent()) {
            mZoomLevel = Framework.nativeGetDrawScale().toDouble()
            val i = Intent()
            if (success) {
                i.putExtra(Const.EXTRA_MWM_RESPONSE_POINT_LAT, lat)
                    .putExtra(Const.EXTRA_MWM_RESPONSE_POINT_LON, lon)
                    .putExtra(Const.EXTRA_MWM_RESPONSE_POINT_NAME, name)
                    .putExtra(Const.EXTRA_MWM_RESPONSE_POINT_ID, mId)
                    .putExtra(Const.EXTRA_MWM_RESPONSE_ZOOM, mZoomLevel)
            }
            try {
                mPendingIntent!!.send(
                    context,
                    if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED,
                    i
                )
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    fun sendResponseAndFinish(activity: Activity, success: Boolean) {
        activity.runOnUiThread {
            sendResponse(activity, success)
            activity.finish()
        }
    }

    companion object {
        @JvmStatic
        @Volatile
        var currentRequest: ParsedMwmRequest? = null

        @JvmStatic
        val isPickPointMode: Boolean
            get() = hasRequest() && currentRequest!!.mPickPoint

        @JvmStatic
        fun hasRequest(): Boolean {
            return currentRequest != null
        }

        /**
         * Build request from intent extras.
         */
        @JvmStatic
        fun extractFromIntent(data: Intent): ParsedMwmRequest {
            val request = ParsedMwmRequest()
            request.callerInfo =
                data.getParcelableExtra(Const.EXTRA_CALLER_APP_INFO)
            request.title = data.getStringExtra(Const.EXTRA_TITLE)
            request.mReturnOnBalloonClick = data.getBooleanExtra(
                Const.EXTRA_RETURN_ON_BALLOON_CLICK,
                false
            )
            request.mPickPoint =
                data.getBooleanExtra(Const.EXTRA_PICK_POINT, false)
            request.customButtonName =
                data.getStringExtra(Const.EXTRA_CUSTOM_BUTTON_NAME)
            if (data.getBooleanExtra(
                    Const.EXTRA_HAS_PENDING_INTENT,
                    false
                )
            ) request.mPendingIntent =
                data.getParcelableExtra(Const.EXTRA_CALLER_PENDING_INTENT)
            return request
        }
    }
}