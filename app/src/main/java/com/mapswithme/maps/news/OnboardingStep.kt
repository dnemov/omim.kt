package com.mapswithme.maps.news

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.StatisticValueConverter

enum class OnboardingStep private constructor(
    @param:StringRes @field:StringRes
    @get:StringRes
    val acceptButtonResId: Int, @param:StringRes @field:StringRes
    private val mDeclineButtonResId: Int,
    @param:StringRes @field:StringRes
    @get:StringRes
    val title: Int, @param:StringRes @field:StringRes
    @get:StringRes
    val subtitle: Int, @param:DrawableRes @field:DrawableRes
    @get:DrawableRes
    val image: Int,
    private val mDeclinedButton: Boolean = true
) : StatisticValueConverter<String> {
    CHECK_OUT_SIGHTS(
        R.string.new_onboarding_step5_3_button,
        R.string.later,
        R.string.new_onboarding_step5_1_header,
        R.string.new_onboarding_step5_3_message,
        R.drawable.img_check_sights_out
    ) {
        override fun toStatisticValue(): String {
            return "sample_discovery"
        }
    },
    SUBSCRIBE_TO_CATALOG(
        R.string.new_onboarding_step5_2_button,
        R.string.later,
        R.string.new_onboarding_step5_1_header,
        R.string.new_onboarding_step5_2_message,
        R.drawable.img_discover_guides
    ) {
        override fun toStatisticValue(): String {
            return "buy_subscription"
        }
    },
    DISCOVER_GUIDES(
        R.string.new_onboarding_step5_1_button,
        R.string.later,
        R.string.new_onboarding_step5_1_header,
        R.string.new_onboarding_step5_1_message,
        R.drawable.img_discover_guides
    ) {
        override fun toStatisticValue(): String {
            return "catalog_discovery"
        }
    },
    SHARE_EMOTIONS(
        R.string.new_onboarding_button_2,
        UiUtils.NO_ID,
        R.string.new_onboarding_step4_header,
        R.string.new_onboarding_step4_message,
        R.drawable.img_share_emptions, false
    ) {
        override fun toStatisticValue(): String {
            return "share_emotions"
        }
    },
    EXPERIENCE(
        R.string.new_onboarding_button,
        UiUtils.NO_ID,
        R.string.new_onboarding_step3_header,
        R.string.new_onboarding_step3_message,
        R.drawable.img_experience, false
    ) {
        override fun toStatisticValue(): String {
            return "experience"
        }
    },
    DREAM_AND_PLAN(
        R.string.new_onboarding_button,
        UiUtils.NO_ID,
        R.string.new_onboarding_step2_header,
        R.string.new_onboarding_step2_message,
        R.drawable.img_dream_and_plan, false
    ) {
        override fun toStatisticValue(): String {
            return "dream_and_plan"
        }
    },
    PERMISSION_EXPLANATION(
        R.string.new_onboarding_button,
        R.string.learn_more,
        R.string.onboarding_permissions_title,
        R.string.onboarding_permissions_message,
        R.drawable.img_welcome
    ) {
        override fun toStatisticValue(): String {
            return "permissions"
        }
    };

    val declinedButtonResId: Int
        @StringRes
        get() {
            if (!hasDeclinedButton())
                throw UnsupportedOperationException("Value : $name")

            return mDeclineButtonResId
        }

    fun hasDeclinedButton(): Boolean {
        return mDeclinedButton
    }
}
