package com.mapswithme.maps.search

internal object DisplayedCategories {
    val keys: Array<String>
        get() = nativeGetKeys()

    @JvmStatic private external fun nativeGetKeys(): Array<String>
}