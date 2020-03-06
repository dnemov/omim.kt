package com.mapswithme.util.statistics

enum class GalleryType {
    LOCAL_EXPERTS {

        override val provider: String
            get() = Statistics.ParamValue.LOCALS_EXPERTS
    },
    SEARCH_RESTAURANTS {
        override val provider: String
            get() = Statistics.ParamValue.SEARCH_RESTAURANTS
    },
    SEARCH_ATTRACTIONS {
        override val provider: String
            get() = Statistics.ParamValue.SEARCH_ATTRACTIONS
    },
    SEARCH_HOTELS {
        override val provider: String
            get() = Statistics.ParamValue.BOOKING_COM
    },
    PROMO {
        override val provider: String
            get() = Statistics.ParamValue.MAPSME_GUIDES
    };

    abstract val provider: String
}