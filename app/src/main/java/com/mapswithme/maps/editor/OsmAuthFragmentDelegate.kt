package com.mapswithme.maps.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.Size
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.maps.editor.OsmOAuth.AuthType
import com.mapswithme.util.Constants
import com.mapswithme.util.Utils
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName

abstract class OsmAuthFragmentDelegate(private val mFragment: Fragment) :
    View.OnClickListener {
    protected abstract fun loginOsm()
    fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        for (@IdRes childId in intArrayOf(R.id.login_osm, R.id.register)) {
            val v = view.findViewById<View>(childId)
            v?.setOnClickListener(this)
        }
    }

    override fun onClick(v: View) { // TODO show/hide spinners
        when (v.id) {
            R.id.login_osm -> {
                Statistics.INSTANCE.trackAuthRequest(AuthType.OSM)
                loginOsm()
            }
            R.id.register -> {
                Statistics.INSTANCE.trackEvent(EventName.EDITOR_REG_REQUEST)
                register()
            }
        }
    }

    fun processAuth(
        @Size(2) auth: Array<String>?, type: AuthType,
        username: String?
    ) {
        if (auth == null) {
            if (mFragment.isAdded) {
                AlertDialog.Builder(mFragment.activity!!)
                    .setTitle(R.string.editor_login_error_dialog)
                    .setPositiveButton(android.R.string.ok, null).show()
                Statistics.INSTANCE.trackEvent(
                    EventName.EDITOR_AUTH_REQUEST_RESULT,
                    Statistics.params().add(
                        Statistics.EventParam.IS_SUCCESS,
                        false
                    ).add(Statistics.EventParam.TYPE, type.name)
                )
            }
            return
        }
        OsmOAuth.setAuthorization(auth[0], auth[1], username)
        if (mFragment.isAdded) Utils.navigateToParent(mFragment.activity)
        Statistics.INSTANCE.trackEvent(
            EventName.EDITOR_AUTH_REQUEST_RESULT,
            Statistics.params().add(
                Statistics.EventParam.IS_SUCCESS,
                true
            ).add(Statistics.EventParam.TYPE, type.name)
        )
    }

    protected fun register() {
        mFragment.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(Constants.Url.OSM_REGISTER)
            )
        )
    }

}