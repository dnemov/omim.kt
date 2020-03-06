package com.mapswithme.maps.ads

import android.os.Parcelable
import androidx.annotation.IntDef
import kotlinx.android.parcel.Parcelize
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Parcelize
class LocalAdInfo private constructor(@Status val mStatus: Int, val url: String?): Parcelable {
    @IntDef(
        STATUS_NOT_AVAILABLE,
        STATUS_CANDIDATE,
        STATUS_CUSTOMER,
        STATUS_HIDDEN
    )
    annotation class Status

    val isAvailable: Boolean
        get() = mStatus == STATUS_NOT_AVAILABLE

    val isCustomer: Boolean
        get() = mStatus == STATUS_CUSTOMER

    val isHidden: Boolean
        get() = mStatus == STATUS_HIDDEN

    companion object {
        private const val STATUS_NOT_AVAILABLE = 0
        private const val STATUS_CANDIDATE = 1
        private const val STATUS_CUSTOMER = 2
        private const val STATUS_HIDDEN = 3
    }

}