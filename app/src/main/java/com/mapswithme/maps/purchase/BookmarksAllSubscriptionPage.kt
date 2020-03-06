package com.mapswithme.maps.purchase

import androidx.annotation.StringRes
import com.mapswithme.maps.R

enum class BookmarksAllSubscriptionPage(
    @field:StringRes @get:StringRes
    @param:StringRes val titleId: Int, @field:StringRes @get:StringRes
    @param:StringRes val descriptionId: Int
) {
    FIRST(
        R.string.all_pass_subscription_message_title,
        R.string.all_pass_subscription_message_subtitle
    ),
    SECOND(
        R.string.all_pass_subscription_message_title_2,
        R.string.all_pass_subscription_message_subtitle_2
    ),
    THIRD(
        R.string.all_pass_subscription_message_title_3,
        R.string.all_pass_subscription_message_subtitle_3
    );

}