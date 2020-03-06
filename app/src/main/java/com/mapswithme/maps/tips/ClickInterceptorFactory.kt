package com.mapswithme.maps.tips

import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.BookmarkCategoriesActivity
import com.mapswithme.maps.bookmarks.BookmarksCatalogActivity.Companion.startForResult
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.maplayer.Mode
import com.mapswithme.util.UTM

internal object ClickInterceptorFactory {
    @JvmStatic
    fun createActivateSubwayLayerListener(): ClickInterceptor {
        return ActivateSubwayLayer()
    }

    @JvmStatic
    fun createOpenDiscoveryScreenListener(): ClickInterceptor {
        return OpenDiscoveryScreen()
    }

    @JvmStatic
    fun createSearchHotelsListener(): ClickInterceptor {
        return SearchHotels()
    }

    @JvmStatic
    fun createOpenBookmarksCatalogListener(): ClickInterceptor {
        return OpenBookmarksCatalog()
    }

    internal class OpenBookmarksCatalog :
        AbstractClickInterceptor(Tutorial.BOOKMARKS) {
        override fun onInterceptClickInternal(activity: MwmActivity) {
            val catalogUrl =
                BookmarkManager.INSTANCE.getCatalogFrontendUrl(UTM.UTM_TIPS_AND_TRICKS)
            startForResult(
                activity,
                BookmarkCategoriesActivity.REQ_CODE_DOWNLOAD_BOOKMARK_CATEGORY, catalogUrl
            )
        }
    }

    internal class ActivateSubwayLayer :
        AbstractClickInterceptor(Tutorial.MAP_LAYERS) {
        override fun onInterceptClickInternal(activity: MwmActivity) {
            Mode.SUBWAY.setEnabled(activity, true)
            activity.onSubwayLayerSelected()
        }
    }

    internal class SearchHotels : AbstractClickInterceptor(Tutorial.SEARCH) {
        override fun onInterceptClickInternal(activity: MwmActivity) {
            activity.showSearch(activity.getString(R.string.hotel))
        }
    }

    internal class OpenDiscoveryScreen :
        AbstractClickInterceptor(Tutorial.DISCOVERY) {
        override fun onInterceptClickInternal(activity: MwmActivity) {
            activity.showDiscovery()
        }
    }
}