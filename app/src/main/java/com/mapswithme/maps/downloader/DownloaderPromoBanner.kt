package com.mapswithme.maps.downloader

import androidx.annotation.IntDef
import com.mapswithme.util.statistics.StatisticValueConverter
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Promo banner for on-map downloader. Created by native code.
 */
class DownloaderPromoBanner(
    @field:DownloaderPromoType @get:DownloaderPromoType
    @param:DownloaderPromoType val type: Int, val url: String
) :
    StatisticValueConverter<String?> {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        DOWNLOADER_PROMO_TYPE_NO_PROMO,
        DOWNLOADER_PROMO_TYPE_BOOKMARK_CATALOG,
        DOWNLOADER_PROMO_TYPE_MEGAFON
    )
    annotation class DownloaderPromoType

    override fun toStatisticValue(): String {
        return DownloaderPromoBannerStats.values()[type]
            .value
    }

    companion object {
        // Must be corresponded to DownloaderPromoType in downloader_promo.hpp
        const val DOWNLOADER_PROMO_TYPE_NO_PROMO = 0
        const val DOWNLOADER_PROMO_TYPE_BOOKMARK_CATALOG = 1
        const val DOWNLOADER_PROMO_TYPE_MEGAFON = 2
    }

}