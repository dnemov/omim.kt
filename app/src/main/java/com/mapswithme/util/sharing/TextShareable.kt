package com.mapswithme.util.sharing

import android.app.Activity

class TextShareable(context: Activity?, text: String?) : BaseShareable(context!!) {
    override val mimeType: String
        get() = TargetUtils.TYPE_TEXT_PLAIN

    init {
        setText(text)
    }
}