package com.mapswithme.maps.search

/**
 * Native search will return results via this interface.
 */
interface NativeBookmarkSearchListener {
    /**
     * @param bookmarkIds Founded bookmark ids.
     * @param timestamp Timestamp of search request.
     */
    fun onBookmarkSearchResultsUpdate(
        bookmarkIds: LongArray?,
        timestamp: Long
    )

    /**
     * @param bookmarkIds Founded bookmark ids.
     * @param timestamp Timestamp of search request.
     */
    fun onBookmarkSearchResultsEnd(
        bookmarkIds: LongArray?,
        timestamp: Long
    )
}