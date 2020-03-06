package com.mapswithme.maps.onboarding

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mapswithme.maps.R

import com.mapswithme.maps.bookmarks.BookmarkCategoriesActivity
import com.mapswithme.maps.bookmarks.BookmarksCatalogActivity.Companion.start
import com.mapswithme.maps.bookmarks.BookmarksPageFactory
import com.mapswithme.util.statistics.StatisticValueConverter

enum class IntroductionScreenFactory : StatisticValueConverter<String?> {
    FREE_GUIDE {
        override fun toStatisticValue(): String {
            return "catalogue"
        }

        override val title: Int
            get() = R.string.onboarding_guide_direct_download_title
        override val subtitle: Int
            get() = R.string.onboarding_guide_direct_download_subtitle
        override val action: Int
            get() = R.string.onboarding_guide_direct_download_button
        override val image: Int
            get() = R.drawable.img_onboarding_guide

        override fun createButtonClickListener(): OnIntroductionButtonClickListener {
            return object : OnIntroductionButtonClickListener {
                override fun onIntroductionButtonClick(
                    activity: Activity,
                    deeplink: String
                ) {
                    BookmarkCategoriesActivity.startForResult(
                        activity,
                        BookmarksPageFactory.DOWNLOADED.ordinal,
                        deeplink
                    )
                }
            }
        }
    },
    GUIDES_PAGE {
        override fun toStatisticValue(): String {
            return "guides_page"
        }

        override val title: Int
            get() = R.string.onboarding_bydeeplink_guide_title
        override val subtitle: Int
            get() = R.string.onboarding_bydeeplink_guide_subtitle
        override val action: Int
            get() = R.string.onboarding_guide_direct_download_button
        override val image: Int
            get() = R.drawable.img_onboarding_guide

        override fun createButtonClickListener(): OnIntroductionButtonClickListener {
            return object : OnIntroductionButtonClickListener {
                override fun onIntroductionButtonClick(
                    activity: Activity,
                    deeplink: String
                ) {
                    start(activity, deeplink)
                }
            }
        }
    };

    @get:StringRes
    abstract val title: Int

    @get:StringRes
    abstract val subtitle: Int

    @get:StringRes
    abstract val action: Int

    @get:DrawableRes
    abstract val image: Int

    abstract fun createButtonClickListener(): OnIntroductionButtonClickListener
}