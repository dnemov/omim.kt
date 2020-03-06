package com.mapswithme.maps.bookmarks.data

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntRange
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
class Metadata : Parcelable {
    // Values must correspond to definitions from feature_meta.hpp.
    enum class MetadataType(private val mMetaType: Int) {
        FMD_CUISINE(1), FMD_OPEN_HOURS(2), FMD_PHONE_NUMBER(3), FMD_FAX_NUMBER(4), FMD_STARS(5), FMD_OPERATOR(
            6
        ),
        FMD_URL(7), FMD_WEBSITE(8), FMD_INTERNET(9), FMD_ELE(10), FMD_TURN_LANES(11), FMD_TURN_LANES_FORWARD(
            12
        ),
        FMD_TURN_LANES_BACKWARD(13), FMD_EMAIL(14), FMD_POSTCODE(15),  // TODO: It is hacked in jni and returns full Wikipedia url. Should use separate getter instead.
        FMD_WIKIPEDIA(16),  // FMD_MAXSPEED(17),
        FMD_FLATS(18), FMD_HEIGHT(19), FMD_MIN_HEIGHT(20), FMD_DENOMINATION(21), FMD_BUILDING_LEVELS(
            22
        ),
        FWD_TEST_ID(23), FMD_SPONSORED_ID(24), FMD_PRICE_RATE(25), FMD_RATING(26), FMD_BANNER_URL(27), FMD_LEVEL(
            28
        ),
        FMD_AIRPORT_IATA(29), FMD_BRAND(30), FMD_DURATION(31);

        fun toInt(): Int {
            return mMetaType
        }

        companion object {
            fun fromInt(
                @IntRange(
                    from = 1,
                    to = 28
                ) metaType: Int
            ): MetadataType {
                for (type in values()) if (type.mMetaType == metaType) return type
                throw IllegalArgumentException("Illegal metaType arg!")
            }
        }

    }

    @IgnoredOnParcel
    private val mMetadataMap: MutableMap<MetadataType, String?> =
        EnumMap(MetadataType::class.java)

    /**
     * Adds metadata with type code and value. Returns false if metaType is wrong or unknown
     *
     * @return true, if metadata was added, false otherwise
     */
    fun addMetadata(metaType: Int, metaValue: String?): Boolean {
        val type = MetadataType.fromInt(metaType)
        mMetadataMap[type] = metaValue
        return true
    }

    fun getMetadata(type: MetadataType?): String? {
        return mMetadataMap[type]
    }
}