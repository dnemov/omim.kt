package com.mapswithme.maps.base

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import org.alohalytics.Statistics

open class BaseMwmFragment : Fragment(), OnBackPressListener {
    override fun onAttach(context: Context) {
        super.onAttach(context)
        Utils.detachFragmentIfCoreNotInitialized(context, this)
    }

    override fun onResume() {
        super.onResume()
        Statistics.logEvent(
            "\$onResume", this.javaClass.simpleName
                    + ":" + UiUtils.deviceOrientationAsString(activity!!)
        )
    }

    override fun onPause() {
        super.onPause()
        Statistics.logEvent("\$onPause", this.javaClass.simpleName)
    }

    val mwmActivity: BaseMwmFragmentActivity
        get() = Utils.castTo(activity!!)

    override fun onBackPressed(): Boolean {
        return false
    }

    val viewOrThrow: View
        get() = this.view
            ?: throw IllegalStateException("Before call this method make sure that fragment exists")
}