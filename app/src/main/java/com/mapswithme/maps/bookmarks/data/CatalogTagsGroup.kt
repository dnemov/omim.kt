package com.mapswithme.maps.bookmarks.data

import java.util.*

class CatalogTagsGroup(val localizedName: String, tags: Array<CatalogTag>) {
    val tags: List<CatalogTag> = tags.asList()

    override fun toString(): String {
        val sb = StringBuilder("CatalogTagsGroup{")
        sb.append("mLocalizedName='").append(localizedName).append('\'')
        sb.append(", mTags=").append(tags)
        sb.append('}')
        return sb.toString()
    }
}