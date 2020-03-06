package com.mapswithme.maps.search

import android.text.TextUtils
import android.util.Pair
import com.mapswithme.util.Language
import java.util.*

object SearchRecents {
    private val sRecents: MutableList<String> =
        ArrayList()

    fun refresh() {
        val pairs: List<Pair<String, String>> =
            ArrayList()
        nativeGetList(pairs)
        sRecents.clear()
        for (pair in pairs) sRecents.add(
            pair.second
        )
    }

    val size: Int
        get() = sRecents.size

    operator fun get(position: Int): String {
        return sRecents[position]
    }

    fun add(query: String): Boolean {
        if (TextUtils.isEmpty(query) || sRecents.contains(query)) return false
        nativeAdd(Language.keyboardLocale, query)
        refresh()
        return true
    }

    fun clear() {
        nativeClear()
        sRecents.clear()
    }

    @JvmStatic private external fun nativeGetList(result: List<Pair<String, String>>)
    @JvmStatic private external fun nativeAdd(locale: String, query: String)
    @JvmStatic private external fun nativeClear()
}