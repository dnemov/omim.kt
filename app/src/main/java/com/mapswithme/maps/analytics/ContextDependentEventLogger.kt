package com.mapswithme.maps.analytics

import android.app.Application

internal abstract class ContextDependentEventLogger(val application: Application) :
    EventLogger