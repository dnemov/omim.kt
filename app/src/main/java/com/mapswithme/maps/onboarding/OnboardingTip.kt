package com.mapswithme.maps.onboarding

import android.os.Parcelable
import androidx.annotation.IntDef
import kotlinx.android.parcel.Parcelize
import kotlin.annotation.Retention

@Parcelize
class OnboardingTip internal constructor(
    @ScreenType val type: Int, val url: String
) : Parcelable {
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        DISCOVER_CATALOG,
        DOWNLOAD_SAMPLES,
        BUY_SUBSCRIPTION
    )
    internal annotation class ScreenType

    companion object {
        // The order is important, must corresponds to
        // OnboardingTip::Type enum at map/onboarding.hpp.
        const val DISCOVER_CATALOG = 0
        const val DOWNLOAD_SAMPLES = 1
        const val BUY_SUBSCRIPTION = 2
        @JvmStatic
        fun get(): OnboardingTip? {
            return nativeGetTip()
        }

        @JvmStatic
        private external fun nativeGetTip(): OnboardingTip?
    }

}