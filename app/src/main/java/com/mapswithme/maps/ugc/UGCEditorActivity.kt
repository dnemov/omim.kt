package com.mapswithme.maps.ugc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.maps.metrics.UserActionsLogger.logUgcEditorOpened
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.statistics.Statistics

class UGCEditorActivity : BaseMwmFragmentActivity() {
    @StyleRes
    override fun getThemeResourceId(theme: String): Int {
        return ThemeUtils.getCardBgThemeResourceId(theme)
    }

    override val fragmentClass: Class<out Fragment>
        protected get() = UGCEditorFragment::class.java

    override fun onBackPressed() {
        Statistics.INSTANCE.trackEvent(Statistics.EventName.UGC_REVIEW_CANCEL)
        super.onBackPressed()
    }

    companion object {
        fun start(activity: Activity, params: EditParams) {
            Statistics.INSTANCE.trackUGCStart(
                false,
                params.isFromPP,
                params.isFromNotification
            )
            logUgcEditorOpened()
            val i = Intent(activity, UGCEditorActivity::class.java)
            val args = Bundle()
            args.putParcelable(UGCEditorFragment.Companion.ARG_FEATURE_ID, params.featureId)
            args.putString(UGCEditorFragment.Companion.ARG_TITLE, params.title)
            args.putInt(UGCEditorFragment.Companion.ARG_DEFAULT_RATING, params.defaultRating)
            args.putParcelableArrayList(
                UGCEditorFragment.Companion.ARG_RATING_LIST,
                params.ratings
            )
            args.putBoolean(UGCEditorFragment.Companion.ARG_CAN_BE_REVIEWED, params.canBeReviewed())
            args.putDouble(UGCEditorFragment.Companion.ARG_LAT, params.lat)
            args.putDouble(UGCEditorFragment.Companion.ARG_LON, params.lon)
            args.putString(UGCEditorFragment.Companion.ARG_ADDRESS, params.address)
            i.putExtras(args)
            activity.startActivity(i)
        }
    }
}