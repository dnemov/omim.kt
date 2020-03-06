package com.mapswithme.util.sharing

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkInfo
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.widget.placepage.Sponsored
import com.mapswithme.util.Utils.isIntentSupported

abstract class ShareOption internal constructor(
    @field:StringRes private val mNameResId: Int, private val mBaseIntent: Intent
) {
    fun isSupported(context: Context?): Boolean {
        return isIntentSupported(context!!, mBaseIntent)
    }

    fun shareMapObject(
        activity: Activity?,
        mapObject: MapObject,
        sponsored: Sponsored?
    ) {
        val mapObjectShareable =
            MapObjectShareable(activity!!, mapObject, sponsored)
        shareObjectInternal(mapObjectShareable)
    }

    fun shareBookmarkObject(
        activity: Activity?, mapObject: BookmarkInfo,
        sponsored: Sponsored?
    ) {
        val shareable: BookmarkInfoShareable<BookmarkInfo> =
            BookmarkInfoShareable(activity!!, mapObject, sponsored)
        shareObjectInternal(shareable)
    }

    private fun shareObjectInternal(shareable: BaseShareable) {
        SharingHelper.shareOutside(
            shareable
                .setBaseIntent(Intent(mBaseIntent)), mNameResId
        )
    }

    class EmailShareOption protected constructor() : ShareOption(
        R.string.share_by_email,
        Intent(Intent.ACTION_SEND).setType(TargetUtils.TYPE_MESSAGE_RFC822)
    )

    class AnyShareOption internal constructor() : ShareOption(
        R.string.share,
        Intent(Intent.ACTION_SEND).setType(TargetUtils.TYPE_TEXT_PLAIN)
    ) {
        fun share(activity: Activity?, body: String?) {
            SharingHelper.shareOutside(TextShareable(activity, body))
        }

        fun share(
            activity: Activity?,
            body: String?, @StringRes titleRes: Int
        ) {
            SharingHelper.shareOutside(TextShareable(activity, body), titleRes)
        }

        companion object {
            val ANY =
                AnyShareOption()
        }
    }

}