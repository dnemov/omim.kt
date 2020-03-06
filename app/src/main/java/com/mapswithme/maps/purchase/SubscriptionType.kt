package com.mapswithme.maps.purchase

import com.mapswithme.maps.PrivateVariables
import com.mapswithme.util.Utils

enum class SubscriptionType {
    ADS_REMOVAL {

        override val serverId: String
            get() = PrivateVariables.adsRemovalServerId()
        override val vendor: String
            get() = PrivateVariables.adsRemovalVendor()
        override val productIds: Array<String>
            get() = Utils.concatArrays(
                PrivateVariables.adsRemovalNotUsedList(),
                PrivateVariables.adsRemovalYearlyProductId(),
                PrivateVariables.adsRemovalMonthlyProductId(),
                PrivateVariables.adsRemovalWeeklyProductId()
            )
        override val yearlyProductId: String
            get() = PrivateVariables.adsRemovalYearlyProductId()
        override val monthlyProductId: String
            get() = PrivateVariables.adsRemovalMonthlyProductId()
    },
    BOOKMARKS_ALL {

        override val serverId: String
            get() = PrivateVariables.bookmarksSubscriptionServerId()
        override val vendor: String
            get() = PrivateVariables.bookmarksSubscriptionVendor()
        override val productIds: Array<String>
            get() = Utils.concatArrays(
                PrivateVariables.bookmarksSubscriptionNotUsedList(),
                PrivateVariables.bookmarksSubscriptionYearlyProductId(),
                PrivateVariables.bookmarksSubscriptionMonthlyProductId()
            )
        override val yearlyProductId: String
            get() = PrivateVariables.bookmarksSubscriptionYearlyProductId()
        override val monthlyProductId: String
            get() = PrivateVariables.bookmarksSubscriptionMonthlyProductId()
    },
    BOOKMARKS_SIGHTS {

        override val serverId: String
            get() = PrivateVariables.bookmarksSubscriptionSightsServerId()
        override val vendor: String
            get() = PrivateVariables.bookmarksSubscriptionVendor()
        override val productIds: Array<String>
            get() = Utils.concatArrays(
                PrivateVariables.bookmarksSubscriptionSightsNotUsedList(),
                PrivateVariables.bookmarksSubscriptionSightsYearlyProductId(),
                PrivateVariables.bookmarksSubscriptionSightsMonthlyProductId()
            )
        override val yearlyProductId: String
            get() = PrivateVariables.bookmarksSubscriptionSightsYearlyProductId()
        override val monthlyProductId: String
            get() = PrivateVariables.bookmarksSubscriptionSightsMonthlyProductId()
    };

    abstract val serverId: String
    abstract val vendor: String
    abstract val productIds: Array<String>
    abstract val yearlyProductId: String
    abstract val monthlyProductId: String

    companion object {
        fun getTypeByBookmarksGroup(group: String): SubscriptionType {
            for (type in values()) {
                if (type.serverId == group) return type
            }
            return BOOKMARKS_ALL
        }
    }
}