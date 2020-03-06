package com.mapswithme.util

import androidx.annotation.IntDef
import com.mapswithme.maps.Framework
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

object PowerManagment {
    // It should consider to power_managment::Scheme from
// map/power_management/power_management_schemas.hpp
    const val NONE = 0
    const val NORMAL = 1
    const val MEDIUM = 2
    const val HIGH = 3
    const val AUTO = 4
    @SchemeType
    fun getScheme(): Int {
        return Framework.nativeGetPowerManagerScheme()
    }

    fun setScheme(@SchemeType value: Int) {
        Framework.nativeSetPowerManagerScheme(value)
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        NONE,
        NORMAL,
        MEDIUM,
        HIGH,
        AUTO
    )
    annotation class SchemeType
}