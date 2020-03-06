package com.mapswithme.maps.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.ListFragment
import com.mapswithme.maps.R
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import org.alohalytics.Statistics

@Deprecated("")
abstract class BaseMwmListFragment : ListFragment() {
    var toolbar: Toolbar? = null
        private set

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Utils.detachFragmentIfCoreNotInitialized(context, this)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        toolbar =
            view.findViewById<View>(R.id.toolbar) as Toolbar
        if (toolbar != null) {
            UiUtils.showHomeUpButton(toolbar!!)
            toolbar!!.setNavigationOnClickListener {
                Utils.navigateToParent(
                    activity
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Statistics.logEvent(
            "\$onResume", javaClass.simpleName + ":" +
                    UiUtils.deviceOrientationAsString(activity!!)
        )
    }

    override fun onPause() {
        super.onPause()
        Statistics.logEvent("\$onPause", javaClass.simpleName)
    }
}