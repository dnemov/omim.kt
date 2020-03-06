package com.mapswithme.maps.intent

import android.content.Intent

interface IntentProcessor {
    fun isSupported(intent: Intent): Boolean
    fun process(intent: Intent): MapTask?
}