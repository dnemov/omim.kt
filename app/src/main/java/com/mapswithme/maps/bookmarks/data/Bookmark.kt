package com.mapswithme.maps.bookmarks.data

import android.annotation.SuppressLint
import androidx.annotation.IntRange
import com.mapswithme.maps.Framework
import com.mapswithme.maps.ads.Banner
import com.mapswithme.maps.ads.LocalAdInfo
import com.mapswithme.maps.routing.RoutePointInfo
import com.mapswithme.maps.search.HotelsFilter.HotelType
import com.mapswithme.maps.search.Popularity
import com.mapswithme.maps.search.PriceFilterView.PriceDef
import com.mapswithme.maps.ugc.UGC
import com.mapswithme.util.Constants

// TODO consider refactoring to remove hack with MapObject unmarshalling itself and Bookmark at the same time.
@SuppressLint("ParcelCreator")
class Bookmark(
    featureId: FeatureId, @IntRange(from = 0) private var mCategoryId: Long,
    @IntRange(from = 0) val bookmarkId: Long,
    title: String?,
    secondaryTitle: String?,
    subtitle: String?,
    address: String?,
    banners: Array<Banner>?,
    reachableByTaxiTypes: IntArray?,
    bookingSearchUrl: String?,
    localAdInfo: LocalAdInfo?,
    routePointInfo: RoutePointInfo?,
    @OpeningMode openingMode: Int,
    shouldShowUGC: Boolean,
    canBeRated: Boolean,
    canBeReviewed: Boolean,
    ratings: Array<UGC.Rating>?,
    hotelType: HotelType?, @PriceDef priceRate: Int,
    popularity: Popularity,
    description: String,
    rawTypes: Array<String>?
) : MapObject(
    featureId, BOOKMARK, title!!, secondaryTitle, subtitle!!, address!!, 0.0, 0.0, "",
    banners, reachableByTaxiTypes, bookingSearchUrl, localAdInfo, routePointInfo,
    openingMode, shouldShowUGC, canBeRated, canBeReviewed, ratings, hotelType, priceRate,
    popularity, description, RoadWarningMarkType.UNKNOWN.ordinal, rawTypes
) {
    private var mIcon: Icon
    private val mMerX: Double
    private val mMerY: Double
    private fun initXY() {
        setLat(
            Math.toDegrees(
                2.0 * Math.atan(
                    Math.exp(
                        Math.toRadians(
                            mMerY
                        )
                    )
                ) - Math.PI / 2.0
            )
        )
        setLon(mMerX)
    }

    override val scale: Double
        get() = BookmarkManager.INSTANCE.getBookmarkScale(bookmarkId)

    fun getDistanceAndAzimuth(
        cLat: Double,
        cLon: Double,
        north: Double
    ): DistanceAndAzimut {
        return Framework.nativeGetDistanceAndAzimuth(mMerX, mMerY, cLat, cLon, north)
    }

    private val iconInternal: Icon
        private get() = Icon(
            BookmarkManager.INSTANCE.getBookmarkColor(bookmarkId),
            BookmarkManager.INSTANCE.getBookmarkIcon(bookmarkId)
        )

    val icon: Icon?
        get() = mIcon

    val categoryName: String
        get() = BookmarkManager.INSTANCE.getCategoryById(mCategoryId).name

    fun setParams(
        title: String,
        icon: Icon?,
        description: String
    ) {
        BookmarkManager.INSTANCE.notifyParametersUpdating(this, title, icon, description)
        if (icon != null) mIcon = icon
        this.title = title
        this.description = description
    }

    var categoryId: Long
        get() = mCategoryId
        set(catId) {
            BookmarkManager.INSTANCE.notifyCategoryChanging(this, catId)
            mCategoryId = catId
        }

    val bookmarkDescription: String
        get() = BookmarkManager.INSTANCE.getBookmarkDescription(bookmarkId)

    fun getGe0Url(addName: Boolean): String {
        return BookmarkManager.INSTANCE.encode2Ge0Url(bookmarkId, addName)
    }

    fun getHttpGe0Url(addName: Boolean): String {
        return getGe0Url(addName).replaceFirst(
            Constants.Url.GE0_PREFIX.toRegex(),
            Constants.Url.HTTP_GE0_PREFIX
        )
    }

    init {
        mIcon = iconInternal
        val ll = BookmarkManager.INSTANCE.getBookmarkXY(bookmarkId)
        mMerX = ll.x
        mMerY = ll.y
        initXY()
    }
}