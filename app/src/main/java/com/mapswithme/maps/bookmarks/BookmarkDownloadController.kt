package com.mapswithme.maps.bookmarks

import android.os.Bundle
import com.mapswithme.maps.base.Detachable
import com.mapswithme.maps.base.Savable

interface BookmarkDownloadController : Detachable<BookmarkDownloadCallback?>,
    Savable<Bundle?> {
    fun downloadBookmark(url: String): Boolean
    fun retryDownloadBookmark()
}