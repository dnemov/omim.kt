package com.mapswithme.maps.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.maps.base.BaseMwmToolbarFragment
import com.mapswithme.maps.widget.ToolbarController
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName

open class AuthFragment : BaseMwmToolbarFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_auth_editor, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        toolbarController.setTitle(R.string.thank_you)
        val osmAuthDelegate: OsmAuthFragmentDelegate = object : OsmAuthFragmentDelegate(this) {
            override fun loginOsm() {
                (activity as BaseMwmFragmentActivity?)!!.replaceFragment(
                    OsmAuthFragment::class.java,
                    null,
                    null
                )
            }
        }
        osmAuthDelegate.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateToolbarController(root: View): ToolbarController {
        return object : ToolbarController(root, activity!!) {
            override fun onUpClick() {
                Statistics.INSTANCE.trackEvent(EventName.EDITOR_AUTH_DECLINED)
                super.onUpClick()
            }
        }
    }
}