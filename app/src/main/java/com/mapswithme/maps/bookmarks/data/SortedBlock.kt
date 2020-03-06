package com.mapswithme.maps.bookmarks.data

import java.util.*

class SortedBlock(
    val name: String, bookmarkIds: Array<Long?>,
    trackIds: Array<Long?>
) {
    val bookmarkIds: MutableList<Long>
    val trackIds: MutableList<Long>
    val isBookmarksBlock: Boolean
        get() = !bookmarkIds.isEmpty()

    val isTracksBlock: Boolean
        get() = !trackIds.isEmpty()

    init {
        this.bookmarkIds =
            ArrayList(Arrays.asList<Long>(*bookmarkIds))
        this.trackIds =
            ArrayList(Arrays.asList<Long>(*trackIds))
    }
}