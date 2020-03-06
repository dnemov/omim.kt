package com.mapswithme.maps.background

import androidx.annotation.IntDef
import java.io.Serializable

open class NotificationCandidate private constructor(@field:NotificationType @param:NotificationType val type: Int) :
    Serializable {
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(
        TYPE_UGC_AUTH,
        TYPE_UGC_REVIEW
    )
    internal annotation class NotificationType

    class UgcReview internal constructor(
        val mercatorPosX: Double,
        val mercatorPosY: Double,
        val readableName: String,
        val defaultName: String,
        val featureBestType: String,
        val address: String
    ) : NotificationCandidate(TYPE_UGC_REVIEW) {

        companion object {
            private const val serialVersionUID = 5469867251355445859L
        }

    }

    companion object {
        private const val serialVersionUID = -7020549752940235436L
        // This constants should be compatible with notifications::NotificationCandidate::Type enum
// from c++ side.
        const val TYPE_UGC_AUTH = 0
        const val TYPE_UGC_REVIEW = 1
    }

}