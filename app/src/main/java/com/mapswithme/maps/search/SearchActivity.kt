package com.mapswithme.maps.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.core.app.NavUtils
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.maps.activity.CustomNavigateUpListener
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.maps.base.OnBackPressListener
import com.mapswithme.maps.purchase.AdsRemovalPurchaseControllerProvider
import com.mapswithme.maps.purchase.PurchaseCallback
import com.mapswithme.maps.purchase.PurchaseController
import com.mapswithme.maps.purchase.PurchaseFactory.createAdsRemovalPurchaseController
import com.mapswithme.util.ThemeUtils

class SearchActivity : BaseMwmFragmentActivity(), CustomNavigateUpListener,
    AdsRemovalPurchaseControllerProvider {
    override var adsRemovalPurchaseController: PurchaseController<PurchaseCallback>? = null

    override fun onSafeCreate(savedInstanceState: Bundle?) {
        super.onSafeCreate(savedInstanceState)
        adsRemovalPurchaseController = createAdsRemovalPurchaseController(this)
        adsRemovalPurchaseController!!.initialize(this)
    }

    @CallSuper
    override fun onSafeDestroy() {
        super.onSafeDestroy()
        if (adsRemovalPurchaseController != null) adsRemovalPurchaseController!!.destroy()
    }

    @StyleRes
    override fun getThemeResourceId(theme: String): Int {
        return ThemeUtils.getCardBgThemeResourceId(theme)
    }

    override val fragmentClass: Class<out Fragment>
        protected get() = SearchFragment::class.java

    override fun useTransparentStatusBar(): Boolean {
        return false
    }

    override fun useColorStatusBar(): Boolean {
        return true
    }

    override fun customOnNavigateUp() {
        val manager = supportFragmentManager
        if (manager.backStackEntryCount == 0) {
            for (fragment in manager.fragments) {
                if (fragment is HotelsFilterHolder) {
                    val holder = fragment as HotelsFilterHolder
                    val filter = holder.hotelsFilter
                    val params = holder.filterParams
                    if (filter != null || params != null) {
                        val intent = NavUtils.getParentActivityIntent(this)
                        intent!!.putExtra(FilterActivity.EXTRA_FILTER, filter)
                        intent.putExtra(FilterActivity.EXTRA_FILTER_PARAMS, params)
                        NavUtils.navigateUpTo(this, intent)
                        return
                    }
                }
            }
            NavUtils.navigateUpFromSameTask(this)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            return
        }
        manager.popBackStack()
    }

    override fun onBackPressed() {
        for (f in supportFragmentManager.fragments) if (f is OnBackPressListener && (f as OnBackPressListener).onBackPressed()) return
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    companion object {
        const val EXTRA_QUERY = "search_query"
        const val EXTRA_LOCALE = "locale"
        const val EXTRA_SEARCH_ON_MAP = "search_on_map"
        fun start(
            activity: Activity, query: String?,
            filter: HotelsFilter?, params: BookingFilterParams?
        ) {
            start(
                activity, query, null /* locale */, false /* isSearchOnMap */,
                filter, params
            )
        }

        fun start(
            activity: Activity, query: String?, locale: String?,
            isSearchOnMap: Boolean, filter: HotelsFilter?,
            params: BookingFilterParams?
        ) {
            val i = Intent(activity, SearchActivity::class.java)
            val args = Bundle()
            args.putString(EXTRA_QUERY, query)
            args.putString(EXTRA_LOCALE, locale)
            args.putBoolean(EXTRA_SEARCH_ON_MAP, isSearchOnMap)
            args.putParcelable(FilterActivity.EXTRA_FILTER, filter)
            args.putParcelable(FilterActivity.EXTRA_FILTER_PARAMS, params)
            i.putExtras(args)
            activity.startActivity(i)
            activity.overridePendingTransition(R.anim.search_fade_in, R.anim.search_fade_out)
        }
    }
}