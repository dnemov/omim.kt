package com.mapswithme.maps.gallery.impl

import android.app.Activity
import android.text.TextUtils
import com.mapswithme.maps.bookmarks.BookmarkCategoriesActivity
import com.mapswithme.maps.bookmarks.BookmarksCatalogActivity.Companion.startForResult
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.gallery.ItemSelectedListener
import com.mapswithme.maps.promo.PromoEntity
import com.mapswithme.util.UTM
import com.mapswithme.util.statistics.Destination
import com.mapswithme.util.statistics.GalleryPlacement
import com.mapswithme.util.statistics.GalleryType
import com.mapswithme.util.statistics.Statistics

class RegularCatalogPromoListener(
    private val mActivity: Activity,
    private val mPlacement: GalleryPlacement
) :
    ItemSelectedListener<PromoEntity> {
    override fun onItemSelected(item: PromoEntity, position: Int) {
        if (TextUtils.isEmpty(item.url)) return
        val utmContentUrl = BookmarkManager.INSTANCE.injectCatalogUTMContent(
            item.url!!,
            UTM.UTM_CONTENT_DETAILS
        )
        startForResult(
            mActivity, BookmarkCategoriesActivity.REQ_CODE_DOWNLOAD_BOOKMARK_CATEGORY,
            utmContentUrl
        )
        Statistics.INSTANCE.trackGalleryProductItemSelected(
            GalleryType.PROMO, mPlacement, position,
            Destination.CATALOGUE
        )
    }

    override fun onMoreItemSelected(item: PromoEntity) {
        if (TextUtils.isEmpty(item.url)) return
        val utmContentUrl = BookmarkManager.INSTANCE.injectCatalogUTMContent(
            item.url!!,
            UTM.UTM_CONTENT_MORE
        )
        startForResult(
            mActivity,
            BookmarkCategoriesActivity.REQ_CODE_DOWNLOAD_BOOKMARK_CATEGORY,
            utmContentUrl
        )
        Statistics.INSTANCE.trackGalleryEvent(
            Statistics.EventName.PP_SPONSOR_MORE_SELECTED,
            GalleryType.PROMO,
            mPlacement
        )
    }

    override fun onActionButtonSelected(
        item: PromoEntity,
        position: Int
    ) { // Method not called.
    }

}