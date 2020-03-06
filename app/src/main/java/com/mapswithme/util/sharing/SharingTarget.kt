package com.mapswithme.util.sharing

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.text.TextUtils
import com.google.gson.annotations.SerializedName
import com.mapswithme.util.Gsonable

class SharingTarget : Gsonable, Comparable<SharingTarget> {
    @SerializedName("package_name")
    var packageName: String? = null
    @SerializedName("usage_count")
    var usageCount = 0
    @Transient
    var name: String? = null
    @Transient
    var activityName: String? = null
    @Transient
    var drawableIcon: Drawable? = null

    constructor() {}
    constructor(packageName: String?) {
        this.packageName = packageName
    }

    override operator fun compareTo(other: SharingTarget): Int {
        return other.usageCount - usageCount
    }

    fun setupComponentName(intent: Intent) {
        if (TextUtils.isEmpty(activityName)) intent.setPackage(packageName) else intent.component =
            ComponentName(packageName as String, activityName as String)
    }
}