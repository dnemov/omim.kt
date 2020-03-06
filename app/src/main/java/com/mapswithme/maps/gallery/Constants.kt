package com.mapswithme.maps.gallery

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

object Constants {
    const val TYPE_PRODUCT = 0
    const val TYPE_MORE = 1

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        TYPE_PRODUCT,
        TYPE_MORE
    )
    internal annotation class ViewType
}