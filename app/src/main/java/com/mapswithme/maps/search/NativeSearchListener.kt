package com.mapswithme.maps.search

/**
 * Native search will return results via this interface.
 */
interface NativeSearchListener {
    /**
     * @param results Search results.
     * @param timestamp Timestamp of search request.
     */
    fun onResultsUpdate(
        results: Array<SearchResult>?,
        timestamp: Long,
        isHotel: Boolean
    )

    /**
     * @param timestamp Timestamp of search request.
     */
    fun onResultsEnd(timestamp: Long)
}