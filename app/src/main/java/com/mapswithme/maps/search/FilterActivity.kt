package com.mapswithme.maps.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.mapswithme.maps.activity.CustomNavigateUpListener
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.statistics.Statistics

class FilterActivity : BaseMwmFragmentActivity(),
    FilterFragment.Listener, CustomNavigateUpListener {
    override fun useColorStatusBar(): Boolean {
        return true
    }

    override fun getThemeResourceId(theme: String): Int {
        return ThemeUtils.getCardBgThemeResourceId(theme)
    }

    override val fragmentClass: Class<out Fragment?>
        protected get() = FilterFragment::class.java

    override fun onFilterApply(
        filter: HotelsFilter?,
        params: BookingFilterParams?
    ) {
        setResult(filter, params, ACTION_FILTER_APPLY)
    }

    private fun setResult(
        filter: HotelsFilter?, params: BookingFilterParams?,
        action: String
    ) {
        val i = Intent(action)
        i.putExtra(EXTRA_FILTER, filter)
        i.putExtra(EXTRA_FILTER_PARAMS, params)
        setResult(Activity.RESULT_OK, i)
        finish()
    }

    override fun customOnNavigateUp() {
        Statistics.INSTANCE.trackFilterEvent(
            Statistics.EventName.SEARCH_FILTER_CANCEL,
            Statistics.EventParam.HOTEL
        )
        finish()
    }

    override fun onBackPressed() {
        Statistics.INSTANCE.trackFilterEvent(
            Statistics.EventName.SEARCH_FILTER_CANCEL,
            Statistics.EventParam.HOTEL
        )
        super.onBackPressed()
    }

    companion object {
        const val REQ_CODE_FILTER = 101
        const val EXTRA_FILTER = "extra_filter"
        const val EXTRA_FILTER_PARAMS = "extra_filter_params"
        const val ACTION_FILTER_APPLY = "action_filter_apply"
        fun startForResult(
            activity: Activity, filter: HotelsFilter?,
            params: BookingFilterParams?, requestCode: Int
        ) {
            val i = buildFilterIntent(activity, filter, params)
            activity.startActivityForResult(i, requestCode)
        }

        @JvmStatic
        fun startForResult(
            fragment: Fragment, filter: HotelsFilter?,
            params: BookingFilterParams?, requestCode: Int
        ) {
            val i =
                buildFilterIntent(fragment.activity!!, filter, params)
            fragment.startActivityForResult(i, requestCode)
        }

        private fun buildFilterIntent(
            activity: Activity, filter: HotelsFilter?,
            params: BookingFilterParams?
        ): Intent {
            val i = Intent(activity, FilterActivity::class.java)
            val args = Bundle()
            args.putParcelable(FilterFragment.Companion.ARG_FILTER, filter)
            args.putParcelable(FilterFragment.Companion.ARG_FILTER_PARAMS, params)
            i.putExtras(args)
            return i
        }
    }
}