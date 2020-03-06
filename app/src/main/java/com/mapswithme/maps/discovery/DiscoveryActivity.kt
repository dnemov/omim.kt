package com.mapswithme.maps.discovery

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.maps.activity.CustomNavigateUpListener
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.maps.bookmarks.BookmarkCategoriesActivity
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.gallery.Items.SearchItem
import com.mapswithme.maps.search.FilterActivity

class DiscoveryActivity : BaseMwmFragmentActivity(), CustomNavigateUpListener,
    DiscoveryFragment.DiscoveryListener {
    override val fragmentClass: Class<out Fragment?>
        protected get() = DiscoveryFragment::class.java

    override fun customOnNavigateUp() {
        finish()
    }

    override fun onRouteToDiscoveredObject(`object`: MapObject) {
        val intent = Intent(ACTION_ROUTE_TO)
        setResult(`object`, intent)
    }

    override fun onShowDiscoveredObject(`object`: MapObject) {
        val intent = Intent(ACTION_SHOW_ON_MAP)
        setResult(`object`, intent)
    }

    override fun onShowFilter() {
        FilterActivity.startForResult(this, null, null, FilterActivity.REQ_CODE_FILTER)
    }

    override fun onShowSimilarObjects(
        item: SearchItem,
        type: ItemType
    ) {
        val intent = Intent()
            .setAction(ACTION_SHOW_FILTER_RESULTS)
            .putExtra(
                EXTRA_FILTER_SEARCH_QUERY,
                getString(type.searchCategory)
            )
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun setResult(`object`: MapObject, intent: Intent) {
        intent.putExtra(EXTRA_DISCOVERY_OBJECT, `object`)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            FilterActivity.REQ_CODE_FILTER -> {
                if (data == null) return
                data.action = ACTION_SHOW_FILTER_RESULTS
                data.putExtra(
                    EXTRA_FILTER_SEARCH_QUERY,
                    getString(R.string.hotel)
                )
                setResult(Activity.RESULT_OK, data)
                finish()
            }
            BookmarkCategoriesActivity.REQ_CODE_DOWNLOAD_BOOKMARK_CATEGORY -> {
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_DISCOVERY_OBJECT = "extra_discovery_object"
        const val EXTRA_FILTER_SEARCH_QUERY = "extra_filter_search_query"
        const val ACTION_ROUTE_TO = "action_route_to"
        const val ACTION_SHOW_ON_MAP = "action_show_on_map"
        const val ACTION_SHOW_FILTER_RESULTS = "action_show_filter_results"
    }
}