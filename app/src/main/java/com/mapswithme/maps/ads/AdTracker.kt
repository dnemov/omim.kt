package com.mapswithme.maps.ads

/**
 * Represents a interface to track an ad visibility on the screen.
 * As result, object of this class can conclude whether a tracked ad has a good impression indicator,
 * i.e. has been shown enough time for user, or not.
 */
interface AdTracker {
    fun onViewShown(provider: String, bannerId: String)
    fun onViewHidden(provider: String, bannerId: String)
    fun onContentObtained(provider: String, bannerId: String)
    fun isImpressionGood(provider: String, bannerId: String): Boolean
}