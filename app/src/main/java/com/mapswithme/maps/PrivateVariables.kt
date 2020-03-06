package com.mapswithme.maps

/**
 * Interface to re-use some important variables from C++.
 */
object PrivateVariables {
    @JvmStatic
    external fun alohalyticsUrl(): String
    @JvmStatic
    external fun alohalyticsRealtimeUrl(): String
    external fun flurryKey(): String
    @JvmStatic
    external fun appsFlyerKey(): String
    external fun myTrackerKey(): String
    @JvmStatic
    external fun myTargetSlot(): Int
    external fun myTargetRbSlot(): Int
    @JvmStatic
    external fun myTargetCheckUrl(): String
    external fun googleWebClientId(): String
    external fun adsRemovalServerId(): String
    external fun adsRemovalVendor(): String
    external fun adsRemovalYearlyProductId(): String
    external fun adsRemovalMonthlyProductId(): String
    external fun adsRemovalWeeklyProductId(): String
    external fun adsRemovalNotUsedList(): Array<String>
    external fun bookmarksVendor(): String
    external fun bookmarkInAppIds(): Array<String>
    external fun bookmarksSubscriptionServerId(): String
    external fun bookmarksSubscriptionVendor(): String
    external fun bookmarksSubscriptionYearlyProductId(): String
    external fun bookmarksSubscriptionMonthlyProductId(): String
    external fun bookmarksSubscriptionNotUsedList(): Array<String>
    external fun bookmarksSubscriptionSightsServerId(): String
    external fun bookmarksSubscriptionSightsYearlyProductId(): String
    external fun bookmarksSubscriptionSightsMonthlyProductId(): String
    external fun bookmarksSubscriptionSightsNotUsedList(): Array<String>
    /**
     * @return interval in seconds
     */
    @JvmStatic
    external fun myTargetCheckInterval(): Long
}