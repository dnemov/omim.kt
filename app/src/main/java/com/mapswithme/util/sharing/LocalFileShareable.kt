package com.mapswithme.util.sharing

import android.app.Activity
import android.content.Intent
import com.mapswithme.util.StorageUtils.getUriForFilePath

class LocalFileShareable internal constructor(
    context: Activity,
    private val mFileName: String,
    override val mimeType: String
) : BaseShareable(context) {
    override fun modifyIntent(intent: Intent?, target: SharingTarget?) {
        super.modifyIntent(intent, target)
        val fileUri = getUriForFilePath(activity, mFileName)
        intent?.putExtra(Intent.EXTRA_STREAM, fileUri)
    }

}