package com.mapswithme.maps.widget.placepage

import android.text.TextUtils
import androidx.annotation.IntDef
import androidx.annotation.UiThread
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.bookmarks.data.Metadata
import com.mapswithme.maps.gallery.Image
import com.mapswithme.maps.review.Review
import com.mapswithme.maps.ugc.UGC
import com.mapswithme.util.NetworkPolicy
import java.lang.ref.WeakReference
import java.util.*

@UiThread
class Sponsored private constructor(
    val rating: String, @UGC.Impress val impress: Int, val price: String,
    val url: String, val deepLink: String, val descriptionUrl: String,
    val moreUrl: String, val reviewUrl: String, @field:SponsoredType @get:SponsoredType
    @param:SponsoredType val type: Int,
    val partnerIndex: Int, val partnerName: String
) {
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(
        TYPE_NONE,
        TYPE_BOOKING,
        TYPE_OPENTABLE,
        TYPE_PARTNER,
        TYPE_HOLIDAY,
        TYPE_PROMO_CATALOG_CITY,
        TYPE_PROMO_CATALOG_SIGHTSEEINGS,
        TYPE_PROMO_CATALOG_OUTDOOR
    )
    annotation class SponsoredType

    class FacilityType(val key: String, val name: String)

    class NearbyObject(
        val category: String, val title: String,
        val distance: String, val latitude: Double, val longitude: Double
    )

    class HotelInfo(
        val mDescription: String?,
        val mPhotos: Array<Image>?,
        val mFacilities: Array<FacilityType>?,
        val mReviews: Array<Review>?,
        val mNearby: Array<NearbyObject>?,
        val mReviewsAmount: Long
    )

    interface OnPriceReceivedListener {
        /**
         * This method is called from the native core on the UI thread
         * when the Hotel price will be obtained
         *
         * @param priceInfo
         */
        @UiThread
        fun onPriceReceived(priceInfo: HotelPriceInfo)
    }

    interface OnHotelInfoReceivedListener {
        /**
         * This method is called from the native core on the UI thread
         * when the Hotel information will be obtained
         *
         * @param id A hotel id
         * @param info A hotel info
         */
        @UiThread
        fun onHotelInfoReceived(id: String, info: HotelInfo)
    }

    var id: String? = null
        private set

    fun updateId(point: MapObject) {
        id =
            point.getMetadata(Metadata.MetadataType.FMD_SPONSORED_ID)
    }

    companion object {
        // Order is important, must match place_page_info.hpp/SponsoredType.
        const val TYPE_NONE = 0
        const val TYPE_BOOKING = 1
        const val TYPE_OPENTABLE = 2
        const val TYPE_PARTNER = 3
        const val TYPE_HOLIDAY = 4
        const val TYPE_PROMO_CATALOG_CITY = 5
        const val TYPE_PROMO_CATALOG_SIGHTSEEINGS = 6
        const val TYPE_PROMO_CATALOG_OUTDOOR = 7
        // Hotel ID -> Price
        private val sPriceCache: MutableMap<String, HotelPriceInfo> =
            HashMap()
        // Hotel ID -> Description
        private val sInfoCache: MutableMap<String, HotelInfo> =
            HashMap()
        private var sPriceListener =
            WeakReference<OnPriceReceivedListener?>(null)
        private var sInfoListener =
            WeakReference<OnHotelInfoReceivedListener?>(null)

        @JvmStatic
        fun setPriceListener(listener: OnPriceReceivedListener) {
            sPriceListener =
                WeakReference(listener)
        }

        @JvmStatic
        fun setInfoListener(listener: OnHotelInfoReceivedListener) {
            sInfoListener =
                WeakReference(listener)
        }

        /**
         * Make request to obtain hotel price information.
         * This method also checks cache for requested hotel id
         * and if cache exists - call [onPriceReceived][.onPriceReceived] immediately
         * @param id A Hotel id
         * @param currencyCode A user currency
         * @param policy A network policy
         */
        @JvmStatic
        fun requestPrice(
            id: String, currencyCode: String,
            policy: NetworkPolicy
        ) {
            val p = sPriceCache[id]
            p?.let { onPriceReceived(it) }
            nativeRequestPrice(policy, id, currencyCode)
        }

        @JvmStatic
        fun requestInfo(
            sponsored: Sponsored,
            locale: String, policy: NetworkPolicy
        ) {
            val id = sponsored.id ?: return
            when (sponsored.type) {
                TYPE_BOOKING -> requestHotelInfo(
                    id,
                    locale,
                    policy
                )
                TYPE_OPENTABLE -> {
                }
                TYPE_NONE -> {
                }
            }
        }

        /**
         * Make request to obtain hotel information.
         * This method also checks cache for requested hotel id
         * and if cache exists - call [onHotelInfoReceived][.onHotelInfoReceived] immediately
         * @param id A Hotel id
         * @param locale A user locale
         * @param policy A network policy
         */

        @JvmStatic
        private fun requestHotelInfo(
            id: String, locale: String,
            policy: NetworkPolicy
        ) {
            val info = sInfoCache[id]
            info?.let { onHotelInfoReceived(id, it) }
            nativeRequestHotelInfo(policy, id, locale)
        }

        @JvmStatic
        fun onPriceReceived(priceInfo: HotelPriceInfo) {
            if (TextUtils.isEmpty(priceInfo.price)) return
            sPriceCache.put(priceInfo.id, priceInfo)
            val listener = sPriceListener.get()
            listener?.onPriceReceived(priceInfo)
        }

        @JvmStatic
        fun onHotelInfoReceived(id: String, info: HotelInfo) {
            sInfoCache.put(id, info)
            val listener = sInfoListener.get()
            listener?.onHotelInfoReceived(id, info)
        }

        @JvmStatic
        fun getPackageName(@SponsoredType type: Int): String {
            return when (type) {
                TYPE_BOOKING -> "com.booking"
                else -> throw AssertionError("Unsupported sponsored type: $type")
            }
        }

        @JvmStatic external fun nativeGetCurrent(): Sponsored?
        @JvmStatic private external fun nativeRequestPrice(
            policy: NetworkPolicy,
            id: String, currencyCode: String
        )

        @JvmStatic private external fun nativeRequestHotelInfo(
            policy: NetworkPolicy,
            id: String, locale: String
        )
    }

}