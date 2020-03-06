package com.mapswithme.maps.metrics


import com.mapswithme.maps.discovery.DiscoveryUserEvent
import com.mapswithme.maps.tips.Tutorial
import com.mapswithme.maps.tips.TutorialAction

object UserActionsLogger {
    @kotlin.jvm.JvmStatic
    fun logTipClickedEvent(tutorial: Tutorial, action: TutorialAction) {
        nativeTipClicked(tutorial.ordinal, action.ordinal)
    }

    @kotlin.jvm.JvmStatic
    fun logBookingFilterUsedEvent() {
        nativeBookingFilterUsed()
    }

    fun logDiscoveryShownEvent() {
        nativeDiscoveryShown()
    }

    fun logBookmarksCatalogShownEvent() {
        nativeBookmarksCatalogShown()
    }

    fun logDiscoveryItemClickedEvent(event: DiscoveryUserEvent) {
        nativeDiscoveryItemClicked(event.ordinal)
    }

    fun logAddToBookmarkEvent() {
        nativeAddToBookmark()
    }

    @kotlin.jvm.JvmStatic
    fun logUgcEditorOpened() {
        nativeUgcEditorOpened()
    }

    @kotlin.jvm.JvmStatic
    fun logUgcSaved() {
        nativeUgcSaved()
    }

    @kotlin.jvm.JvmStatic
    fun logBookingBookClicked() {
        nativeBookingBookClicked()
    }

    @kotlin.jvm.JvmStatic
    fun logBookingMoreClicked() {
        nativeBookingMoreClicked()
    }

    @kotlin.jvm.JvmStatic
    fun logBookingReviewsClicked() {
        nativeBookingReviewsClicked()
    }

    @kotlin.jvm.JvmStatic
    fun logBookingDetailsClicked() {
        nativeBookingDetailsClicked()
    }

    @kotlin.jvm.JvmStatic
    fun logPromoAfterBookingShown(id: String) {
        nativePromoAfterBookingShown(id)
    }

    @JvmStatic private external fun nativeTipClicked(type: Int, event: Int)
    @JvmStatic private external fun nativeBookingFilterUsed()
    @JvmStatic private external fun nativeBookmarksCatalogShown()
    @JvmStatic private external fun nativeDiscoveryShown()
    @JvmStatic private external fun nativeDiscoveryItemClicked(event: Int)
    @JvmStatic private external fun nativeAddToBookmark()
    @JvmStatic private external fun nativeUgcEditorOpened()
    @JvmStatic private external fun nativeUgcSaved()
    @JvmStatic private external fun nativeBookingBookClicked()
    @JvmStatic private external fun nativeBookingMoreClicked()
    @JvmStatic private external fun nativeBookingReviewsClicked()
    @JvmStatic private external fun nativeBookingDetailsClicked()
    @JvmStatic private external fun nativePromoAfterBookingShown(id: String)
}