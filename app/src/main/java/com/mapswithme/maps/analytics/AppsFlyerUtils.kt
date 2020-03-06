package com.mapswithme.maps.analytics

internal object AppsFlyerUtils {
    @JvmStatic
    fun isFirstLaunch(conversionData: Map<String, String>): Boolean {
        val isFirstLaunch = conversionData["is_first_launch"]
        return java.lang.Boolean.parseBoolean(isFirstLaunch)
    }

    @JvmStatic
    fun getDeepLink(conversionData: Map<String, String>): String? {
        return conversionData["af_dp"]
    }
}