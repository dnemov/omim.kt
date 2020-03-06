package com.mapswithme.maps.widget.placepage

import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.mapswithme.maps.base.Initializable
import com.mapswithme.maps.base.Savable
import com.mapswithme.maps.bookmarks.data.MapObject

interface PlacePageController : Initializable, Savable<Bundle?>, ActivityLifecycleCallbacks {
    fun openFor(`object`: MapObject)
    fun close()
    val isClosed: Boolean

    interface SlideListener {
        fun onPlacePageSlide(top: Int)
    }
}