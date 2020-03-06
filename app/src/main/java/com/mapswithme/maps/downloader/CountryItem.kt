package com.mapswithme.maps.downloader

import android.text.TextUtils

/**
 * Class representing a single item in countries hierarchy.
 * Fields are filled by native code.
 */
class CountryItem(val id: String?) : Comparable<CountryItem> {
    var directParentId: String? = null
    var topmostParentId: String? = null
    @kotlin.jvm.JvmField
    var name: String? = null
    var directParentName: String? = null
    var topmostParentName: String? = null
    var description: String? = null
    var size: Long = 0
    var enqueuedSize: Long = 0
    @kotlin.jvm.JvmField
    var totalSize: Long = 0
    var childCount = 0
    @kotlin.jvm.JvmField
    var totalChildCount = 0
    var category = 0
    @kotlin.jvm.JvmField
    var status = 0
    var errorCode = 0
    var present = false
    // Progress
    var progress = 0
    var downloadedBytes: Long = 0
    var bytesToDownload: Long = 0
    // Internal ID for grouping under headers in the list
    var headerId = 0
    // Internal field to store search result name
    var searchResultName: String? = null

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other == null || javaClass != other.javaClass) false else id == (other as CountryItem).id
    }

    override fun compareTo(another: CountryItem): Int {
        val catDiff = category - another.category
        return if (catDiff != 0) catDiff else name!!.compareTo(another.name!!)
    }

    fun update() {
        MapManager.nativeGetAttributes(this)
        ensureRootIdKnown()
        if (TextUtils.equals(sRootId, directParentId)) directParentId = ""
    }

    val isExpandable: Boolean
        get() = totalChildCount > 1

    override fun toString(): String {
        return "{ id: \"" + id +
                "\", directParentId: \"" + directParentId +
                "\", topmostParentId: \"" + topmostParentId +
                "\", category: \"" + category +
                "\", name: \"" + name +
                "\", directParentName: \"" + directParentName +
                "\", topmostParentName: \"" + topmostParentName +
                "\", present: " + present +
                ", status: " + status +
                ", errorCode: " + errorCode +
                ", headerId: " + headerId +
                ", size: " + size +
                ", enqueuedSize: " + enqueuedSize +
                ", totalSize: " + totalSize +
                ", childCount: " + childCount +
                ", totalChildCount: " + totalChildCount +
                ", progress: " + progress +
                "% }"
    }

    companion object {
        private var sRootId: String? = null
        // Must correspond to ItemCategory in MapManager.cpp
        const val CATEGORY_NEAR_ME = 0
        const val CATEGORY_DOWNLOADED = 1
        const val CATEGORY_AVAILABLE = 2
        const val CATEGORY__LAST = CATEGORY_AVAILABLE
        // Must correspond to NodeStatus in storage_defines.hpp
        const val STATUS_UNKNOWN = 0
        const val STATUS_PROGRESS = 1 // Downloading a new mwm or updating an old one.
        const val STATUS_APPLYING = 2 // Applying downloaded diff for an old mwm.
        const val STATUS_ENQUEUED = 3 // An mwm is waiting for downloading in the queue.
        const val STATUS_FAILED = 4 // An error happened while downloading
        const val STATUS_UPDATABLE =
            5 // An update for a downloaded mwm is ready according to counties.txt.
        const val STATUS_DONE = 6 // Downloaded mwm(s) is up to date. No need to update it.
        const val STATUS_DOWNLOADABLE = 7 // An mwm can be downloaded but not downloaded yet.
        const val STATUS_PARTLY =
            8 // Leafs of group node has a mix of STATUS_DONE and STATUS_DOWNLOADABLE.
        // Must correspond to NodeErrorCode in storage_defines.hpp
        const val ERROR_NONE = 0
        const val ERROR_UNKNOWN = 1
        const val ERROR_OOM = 2
        const val ERROR_NO_INTERNET = 3
        private fun ensureRootIdKnown() {
            if (sRootId == null) sRootId =
                MapManager.nativeGetRoot()
        }

        @kotlin.jvm.JvmStatic
        fun fill(countryId: String?): CountryItem {
            val res = CountryItem(countryId)
            res.update()
            return res
        }

        fun isRoot(id: String?): Boolean {
            ensureRootIdKnown()
            return sRootId == id
        }

        val rootId: String?
            get() {
                ensureRootIdKnown()
                return sRootId
            }
    }

}