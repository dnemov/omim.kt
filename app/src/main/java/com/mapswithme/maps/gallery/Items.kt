package com.mapswithme.maps.gallery

import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.gallery.Constants.TYPE_MORE
import com.mapswithme.maps.gallery.Constants.TYPE_PRODUCT
import com.mapswithme.maps.search.Popularity
import com.mapswithme.maps.search.SearchResult

class Items {
    open class LocalExpertItem(
        @Constants.ViewType viewType: Int, title: String,
        url: String?, val photoUrl: String?, val price: Double,
        val currency: String, val rating: Double
    ) : RegularAdapterStrategy.Item(viewType, title, null, url)

    class LocalExpertMoreItem(url: String?) : LocalExpertItem(
        TYPE_MORE, MwmApplication.get().getString(R.string.placepage_more_button), url,
        null, 0.0, "", 0.0
    )

    open class SearchItem : RegularAdapterStrategy.Item {
        private val mResult: SearchResult

        constructor(result: SearchResult) : super(
            TYPE_PRODUCT,
            result.name,
            result.description?.featureType,
            null
        ) {
            mResult = result
        }

        constructor(title: String) : super(TYPE_MORE, title, null, null) {
            mResult = SearchResult.EMPTY
        }

        val distance: String
            get() {
                val d = mResult.description
                return if (d != null) d.distance else ""
            }

        val lat: Double
            get() = mResult.lat

        val lon: Double
            get() = mResult.lon

        val stars: Int
            get() = if (mResult.description == null) 0 else mResult.description.stars

        val rating: Float
            get() = if (mResult.description == null) com.mapswithme.util.Constants.Rating.RATING_INCORRECT_VALUE else mResult.description.rating

        val price: String?
            get() = if (mResult.description == null) null else mResult.description.pricing

        val featureType: String?
            get() = if (mResult.description == null) null else mResult.description.featureType

        val popularity: Popularity
            get() = mResult.getPopularity()
    }

    class MoreSearchItem :
        SearchItem(MwmApplication.get().getString(R.string.placepage_more_button))

    open class Item(
        val title: String, val url: String?,
        val subtitle: String?
    )
}