package com.mapswithme.maps.taxi

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mapswithme.maps.R
import com.mapswithme.util.Utils.PartnerAppOpenMode

enum class TaxiType(
    @field:StringRes @get:StringRes
    @param:StringRes val waitingTemplateResId: Int, val formatPriceStrategy: FormatPriceStrategy,
    val isPriceApproximated: Boolean
) {
    UBER {

        override val packageName: String
            get() = "com.ubercab"
        override val openMode: PartnerAppOpenMode
            get() = PartnerAppOpenMode.Direct
        override val icon: Int
            get() = R.drawable.ic_logo_uber
        override val title: Int
            get() = R.string.uber
        override val providerName: String
            get() = "Uber"
    },
    YANDEX(LocaleDependentFormatPriceStrategy(), true) {
        override val packageName: String
            get() = "ru.yandex.taxi"
        override val openMode: PartnerAppOpenMode
            get() = PartnerAppOpenMode.Indirect
        override val icon: Int
            get() = R.drawable.ic_logo_yandex_taxi
        override val title: Int
            get() = R.string.yandex_taxi_title
        override val providerName: String
            get() = "Yandex"
    },
    MAXIM(true) {

        override val packageName: String
            get() = "com.taxsee.taxsee"
        override val openMode: PartnerAppOpenMode
            get() = PartnerAppOpenMode.Direct
        override val icon: Int
            get() = R.drawable.ic_taxi_logo_maksim
        override val title: Int
            get() = R.string.maxim_taxi_title
        override val providerName: String
            get() = "Maxim"
    },
    TAXI_VEZET(R.string.place_page_starting_from, LocaleDependentFormatPriceStrategy(), true) {

        override val packageName: String
            get() = "ru.rutaxi.vezet"
        override val openMode: PartnerAppOpenMode
            get() = PartnerAppOpenMode.Direct
        override val icon: Int
            get() = R.drawable.ic_taxi_logo_vezet
        override val title: Int
            get() = R.string.vezet_taxi
        override val providerName: String
            get() = "Vezet"
    };

    constructor(
        strategy: FormatPriceStrategy,
        priceApproximated: Boolean
    ) : this(R.string.taxi_wait, strategy, priceApproximated) {
    }

    constructor(priceApproximated: Boolean = false) : this(
        DefaultFormatPriceStrategy(),
        priceApproximated
    ) {
    }

    abstract val packageName: String
    abstract val openMode: PartnerAppOpenMode
    @get:DrawableRes
    abstract val icon: Int

    @get:StringRes
    abstract val title: Int

    abstract val providerName: String

}