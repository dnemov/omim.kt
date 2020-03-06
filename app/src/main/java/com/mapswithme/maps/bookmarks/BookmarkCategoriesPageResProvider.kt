package com.mapswithme.maps.bookmarks

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mapswithme.maps.R

interface BookmarkCategoriesPageResProvider {
    val headerText: Int
        @StringRes get

    val footerText: Int
        @StringRes get

    val footerImage: Int
        @DrawableRes get

    val headerBtn: Button

    open class Default @JvmOverloads constructor(private val mBtn: Button = Button()) :
        BookmarkCategoriesPageResProvider {

        override val headerText: Int
            get() = R.string.bookmarks_groups
        override val footerText: Int
            get() = R.string.bookmarks_create_new_group
        override val footerImage: Int
            get() = R.drawable.ic_checkbox_add
        override val headerBtn: Button
            get() = mBtn

    }

    class Catalog :
        Default() {

        override val headerText: Int
            get() = R.string.guides_groups_cached
        override val footerImage: Int
            get() = R.drawable.ic_download
        override val footerText: Int
            get() = R.string.download_guides
    }

    class Button {

        val selectModeText: Int
            @StringRes get() = R.string.bookmarks_groups_show_all

        val unSelectModeText: Int
            @StringRes get() = R.string.bookmarks_groups_hide_all
    }
}