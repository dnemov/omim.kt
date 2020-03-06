package com.mapswithme.maps.downloader

import android.app.Application
import com.mapswithme.util.Utils.Proc

internal class ExpandRetryConfirmationListener(
    app: Application,
    private val mDialogClickListener: Proc<Boolean>?
) : RetryFailedDownloadConfirmationListener(app) {
    override fun run() {
        super.run()
        if (mDialogClickListener == null) return
        mDialogClickListener.invoke(true)
    }

}