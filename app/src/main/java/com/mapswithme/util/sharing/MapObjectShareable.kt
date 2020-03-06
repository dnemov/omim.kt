package com.mapswithme.util.sharing

import android.app.Activity
import com.mapswithme.maps.Framework
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.widget.placepage.Sponsored
import java.util.*

internal class MapObjectShareable(
    context: Activity,
    mapObject: MapObject,
    sponsored: Sponsored?
) : BookmarkInfoShareable<MapObject>(context, mapObject, sponsored) {
    private fun makeMyPositionEmailBodyContent(): String {
        return activity.getString(
            R.string.my_position_share_email,
            Framework.nativeGetAddress(
                super.provider.lat,
                super.provider.lon
            ),
            super.geoUrl, super.httpUrl
        )
    }


    override val emailBodyContent: Iterable<String>
        get() = Arrays.asList(
            super.provider.name, super.provider.subtitle, super.provider.address,
            super.geoUrl, super.httpUrl
        )

    init {
        if (MapObject.isOfType(MapObject.MY_POSITION, mapObject)) {
            setSubject(R.string.my_position_share_email_subject)
            val text = makeMyPositionEmailBodyContent()
            setText(text)
        }
    }
}