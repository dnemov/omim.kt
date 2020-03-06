package com.mapswithme.maps.bookmarks.data

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import androidx.annotation.IntDef
import com.mapswithme.maps.ads.Banner
import com.mapswithme.maps.ads.LocalAdInfo
import com.mapswithme.maps.bookmarks.data.Metadata.MetadataType
import com.mapswithme.maps.routing.RoutePointInfo
import com.mapswithme.maps.search.HotelsFilter.HotelType
import com.mapswithme.maps.search.Popularity
import com.mapswithme.maps.search.PopularityProvider
import com.mapswithme.maps.search.PriceFilterView
import com.mapswithme.maps.search.PriceFilterView.PriceDef
import com.mapswithme.maps.taxi.TaxiType
import com.mapswithme.maps.ugc.UGC
import com.mapswithme.util.sharing.ShareableInfoProvider
import kotlinx.android.parcel.Parcelize
import java.util.*

// TODO(yunikkk): Refactor. Displayed information is different from edited information, and it's better to
// separate them. Simple getters from jni place_page::Info and osm::EditableFeature should be enough.
@Parcelize
open class MapObject(
    val featureId: FeatureId, @field:MapObjectType @get:MapObjectType
    @MapObjectType var mMapObjectType: Int,
    var title: String,
    var secondaryTitle: String?,
    var subtitle: String,
    private val mAddress: String,
    private var mLat: Double,
    private var mLon: Double,
    var metadata: Metadata,
    var apiId: String?,
    var banners: Array<Banner>? = null,
    val taxiTypes: IntArray?,
    var bookingSearchUrl: String?,
    val localAdInfo: LocalAdInfo?,
    val routePointInfo: RoutePointInfo?, @field:OpeningMode @get:OpeningMode
    @param:OpeningMode val openingMode: Int,
    val shouldShowUGC: Boolean,
    val canBeRated: Boolean,
    val canBeReviewed: Boolean,
    val ratings: Array<UGC.Rating>?,
    var hotelType: HotelType?,
    @PriceDef var priceRate: Int,
    private val mPopularity: Popularity,
    var description: String,
    var roadWarningType: Int,
    var rawTypes: Array<String>?
) : Parcelable, PopularityProvider, ShareableInfoProvider {
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(
        POI,
        API_POINT,
        BOOKMARK,
        MY_POSITION,
        SEARCH
    )
    annotation class MapObjectType

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(
        OPENING_MODE_PREVIEW,
        OPENING_MODE_PREVIEW_PLUS,
        OPENING_MODE_DETAILS,
        OPENING_MODE_FULL
    )
    annotation class OpeningMode

    private var reachableByTaxiTypes: MutableList<TaxiType>? = null
    val roadWarningMarkType: RoadWarningMarkType
    var defaultRatings: ArrayList<UGC.Rating>? = null

    constructor(
        featureId: FeatureId, @MapObjectType mapObjectType: Int,
        title: String,
        secondaryTitle: String?,
        subtitle: String,
        address: String,
        lat: Double,
        lon: Double,
        apiId: String?,
        banners: Array<Banner>?,
        types: IntArray?,
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
        roadWarningType: Int,
        rawTypes: Array<String>?
    ) : this(
        featureId, mapObjectType, title, secondaryTitle,
        subtitle, address, lat, lon, Metadata(), apiId, banners,
        types, bookingSearchUrl, localAdInfo, routePointInfo, openingMode, shouldShowUGC,
        canBeRated, canBeReviewed, ratings, hotelType, priceRate, popularity, description,
        roadWarningType, rawTypes
    ) {
    }

    /**
     * If you override [.equals] it is also required to override [.hashCode].
     * MapObject does not participate in any sets or other collections that need `hashCode()`.
     * So `sameAs()` serves as `equals()` but does not break the equals+hashCode contract.
     */
    fun sameAs(other: MapObject?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (javaClass != other.javaClass) return false
        return if (featureId != FeatureId.EMPTY && other.featureId != FeatureId.EMPTY) featureId == other.featureId else java.lang.Double.doubleToLongBits(
            lon
        ) == java.lang.Double.doubleToLongBits(
            other.lon
        ) &&
                java.lang.Double.doubleToLongBits(lat) == java.lang.Double.doubleToLongBits(other.lat)
    }


    fun getMetadata(type: MetadataType?): String {
        val res = metadata.getMetadata(type)
        return res ?: ""
    }

    fun getReachableByTaxiTypes(): List<TaxiType>? {
        return reachableByTaxiTypes
    }

    fun addMetadata(type: MetadataType, value: String?) {
        metadata.addMetadata(type.toInt(), value)
    }

    private fun addMetadata(type: Int, value: String?) {
        metadata.addMetadata(type, value)
    }

    fun addMetadata(types: IntArray, values: Array<String?>) {
        for (i in types.indices) addMetadata(types[i], values[i])
    }

    fun hasPhoneNumber(): Boolean {
        return !TextUtils.isEmpty(getMetadata(MetadataType.FMD_PHONE_NUMBER))
    }

    fun shouldShowUGC(): Boolean {
        return shouldShowUGC
    }

    fun canBeRated(): Boolean {
        return canBeRated
    }

    fun canBeReviewed(): Boolean {
        return canBeReviewed
    }

    companion object {
        const val POI = 0
        const val API_POINT = 1
        const val BOOKMARK = 2
        const val MY_POSITION = 3
        const val SEARCH = 4
        const val OPENING_MODE_PREVIEW = 0
        const val OPENING_MODE_PREVIEW_PLUS = 1
        const val OPENING_MODE_DETAILS = 2
        const val OPENING_MODE_FULL = 3
        @JvmStatic
        fun createMapObject(
            featureId: FeatureId, @MapObjectType mapObjectType: Int,
            title: String, subtitle: String, lat: Double, lon: Double
        ): MapObject {
            return MapObject(
                featureId, mapObjectType, title,
                "", subtitle, "", lat, lon, "", null,
                null, "", null, null, OPENING_MODE_PREVIEW,
                false /* shouldShowUGC */, false /* canBeRated */, false /* canBeReviewed */,
                null /* ratings */, null /* this.hotelType */,
                PriceFilterView.UNDEFINED, Popularity.defaultInstance(), "",
                RoadWarningMarkType.UNKNOWN.ordinal, emptyArray()
            )
        }

        private fun readBanners(source: Parcel): List<Banner>? {
            return null
        }

        private fun readRatings(source: Parcel): ArrayList<UGC.Rating>? {
            val ratings = ArrayList<UGC.Rating>()
            source.readTypedList(ratings, UGC.Rating.CREATOR)
            return if (ratings.isEmpty()) null else ratings
        }

        private fun readTaxiTypes(source: Parcel): List<TaxiType?> {
            val types: List<TaxiType?> = ArrayList()
            source.readList(types, TaxiType::class.java.classLoader)
            return types
        }

        private fun readRawTypes(source: Parcel): List<String> {
            val types: List<String> = ArrayList()
            source.readStringList(types)
            return types
        }

        @JvmStatic
        fun same(one: MapObject?, another: MapObject?): Boolean {
            return if (one == null && another == null) true else one != null && one.sameAs(another)
        }

        @JvmStatic
        fun isOfType(@MapObjectType type: Int, `object`: MapObject?): Boolean {
            return `object` != null && `object`.mapObjectType == type
        }
    }

    init {
        if (taxiTypes != null) {
            reachableByTaxiTypes = ArrayList()
            for (type in taxiTypes) (reachableByTaxiTypes as ArrayList<TaxiType>).add(TaxiType.values()[type])
        }
        if (ratings != null) defaultRatings =
            ArrayList<UGC.Rating>(Arrays.asList(*ratings))
        this.hotelType = hotelType
        this.priceRate = priceRate
        roadWarningMarkType = RoadWarningMarkType.values()[roadWarningType]
    }

    override fun getPopularity(): Popularity = mPopularity



    open fun setLat(value: Double) {
        this.mLat = value
    }

    open fun setLon(value: Double) {
        this.mLon = value
    }

    @MapObjectType
    open val mapObjectType: Int
        get() = mMapObjectType

    override val scale: Double
        get() = 0.0

    override val address: String
        get() = mAddress

    override val name: String
        get() = title

    override val lat: Double
        get() = mLat

    override val lon: Double
        get() = mLon

}