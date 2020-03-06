package com.mapswithme.maps.downloader

import com.mapswithme.util.statistics.Statistics

enum class DownloaderPromoBannerStats {
    NO_PROMO {
        override val value: String
            get() = throw UnsupportedOperationException("Unsupported here")
    },
    CATALOG {

        override val value: String
            get() = Statistics.ParamValue.MAPSME_GUIDES
    },
    MEGAFON {

        override val value: String
            get() = Statistics.ParamValue.MEGAFON
    };

    abstract val value: String
}