package com.mapswithme.util.log

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import com.mapswithme.maps.MwmApplication

object MemLogging {
    val memoryInfo: String
        get() {
            val debugMI = Debug.MemoryInfo()
            Debug.getMemoryInfo(debugMI)
            val mi = ActivityManager.MemoryInfo()
            val activityManager = MwmApplication.get().applicationContext
                    .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(mi)
            val log = StringBuilder("Memory info: ")
            log.append(" Debug.getNativeHeapSize() = ")
                .append(Debug.getNativeHeapSize() / 1024)
                .append("KB; Debug.getNativeHeapAllocatedSize() = ")
                .append(Debug.getNativeHeapAllocatedSize() / 1024)
                .append("KB; Debug.getNativeHeapFreeSize() = ")
                .append(Debug.getNativeHeapFreeSize() / 1024)
                .append("KB; debugMI.getTotalPrivateDirty() = ").append(debugMI.totalPrivateDirty)
                .append("KB; debugMI.getTotalPss() = ").append(debugMI.totalPss)
                .append("KB; mi.availMem = ").append(mi.availMem / 1024)
                .append("KB; mi.threshold = ").append(mi.threshold / 1024)
                .append("KB; mi.lowMemory = ").append(mi.lowMemory)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                log.append(" mi.totalMem = ").append(mi.totalMem / 1024).append("KB;")
            }
            return log.toString()
        }
}