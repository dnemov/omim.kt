package com.mapswithme.maps.api

/**
 * Represents url_scheme::SearchRequest from core.
 */
class ParsedSearchRequest(
    val mQuery: String,
    val mLocale: String?,
    val mLat: Double,
    val mLon: Double,
    val mIsSearchOnMap: Boolean
)