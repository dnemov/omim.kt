package com.mapswithme.util

import android.content.Context
import android.content.SharedPreferences
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory

object SecureStorage {
    private val LOGGER: Logger = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
    private val TAG = SecureStorage::class.java.simpleName
    private val mPrefs: SharedPreferences =
        MwmApplication.get().getSharedPreferences("secure", Context.MODE_PRIVATE)

    @JvmStatic
    fun save(key: String, value: String) {
        LOGGER.d(
            TAG,
            "save: key = $key"
        )
        mPrefs.edit().putString(key, value).apply()
    }

    @JvmStatic
    fun load(key: String): String? {
        LOGGER.d(
            TAG,
            "load: key = $key"
        )
        return mPrefs.getString(key, null)
    }

    @JvmStatic
    fun remove(key: String) {
        LOGGER.d(
            TAG,
            "remove: key = $key"
        )
        mPrefs.edit().remove(key).apply()
    }
}