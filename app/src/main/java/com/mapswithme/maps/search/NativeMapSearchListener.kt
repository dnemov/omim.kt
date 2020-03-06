package com.mapswithme.maps.search

interface NativeMapSearchListener {
    class Result(val countryId: String, val matchedString: String)

    fun onMapSearchResults(
        results: Array<Result>?,
        timestamp: Long,
        isLast: Boolean
    )
}