package com.mapswithme.maps.editor

import android.app.Activity
import android.content.Intent
import android.graphics.LightingColorFilter
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.maps.base.BaseMwmToolbarFragment
import com.mapswithme.maps.editor.OsmOAuth.AuthType
import com.mapswithme.maps.widget.ToolbarController
import com.mapswithme.util.*
import com.mapswithme.util.concurrency.ThreadPool
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName

class OsmAuthFragment : BaseMwmToolbarFragment(), View.OnClickListener {
    private var mDelegate: OsmAuthFragmentDelegate? = null
    private var mProgress: ProgressBar? = null
    private var mTvLogin: TextView? = null
    private var mTvLostPassword: View? = null

    private class AuthToolbarController internal constructor(
        root: View?,
        activity: Activity?
    ) : ToolbarController(root!!, activity!!) {
        init {
            toolbar.navigationIcon = Graphics.tint(
                activity!!,
                activity!!.resources.getDrawable(R.drawable.ic_cancel)
            )
            toolbar.setTitleTextColor(
                ThemeUtils.getColor(
                    activity,
                    android.R.attr.textColorPrimary
                )
            )
        }
    }

    private var mEtLogin: EditText? = null
    private var mEtPassword: EditText? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_osm_login, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        mDelegate = object : OsmAuthFragmentDelegate(this) {
            override fun loginOsm() {
                (activity as BaseMwmFragmentActivity?)!!.replaceFragment(
                    OsmAuthFragment::class.java,
                    null,
                    null
                )
            }
        }
        mDelegate?.onViewCreated(view, savedInstanceState)
        toolbarController.setTitle(R.string.login)
        mEtLogin = view.findViewById<View>(R.id.osm_username) as EditText
        mEtPassword = view.findViewById<View>(R.id.osm_password) as EditText
        mTvLogin = view.findViewById<View>(R.id.login) as TextView
        mTvLogin!!.setOnClickListener(this)
        mTvLostPassword = view.findViewById(R.id.lost_password)
        mTvLostPassword?.setOnClickListener(this)
        mProgress =
            view.findViewById<View>(R.id.osm_login_progress) as ProgressBar
        mProgress!!.indeterminateDrawable.colorFilter = LightingColorFilter(-0x1000000, 0xFFFFFF)
        UiUtils.hide(mProgress!!)
    }

    override fun onCreateToolbarController(root: View): ToolbarController {
        return AuthToolbarController(root, activity)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.login -> login()
            R.id.lost_password -> recoverPassword()
        }
    }

    private fun login() {
        InputUtils.hideKeyboard(mEtLogin)
        val username = mEtLogin!!.text.toString()
        val password = mEtPassword!!.text.toString()
        enableInput(false)
        UiUtils.show(mProgress)
        mTvLogin!!.text = ""
        ThreadPool.worker.execute(Runnable {
            val auth =
                OsmOAuth.nativeAuthWithPassword(username, password)
            val username =
                if (auth == null) null else OsmOAuth.nativeGetOsmUsername(
                    auth[0],
                    auth[1]
                )
            UiThread.run(Runnable {
                if (!isAdded) return@Runnable
                enableInput(true)
                UiUtils.hide(mProgress!!)
                mTvLogin!!.setText(R.string.login)
                mDelegate!!.processAuth(auth, AuthType.OSM, username)
            })
        })
    }

    private fun enableInput(enable: Boolean) {
        mEtPassword!!.isEnabled = enable
        mEtLogin!!.isEnabled = enable
        mTvLogin!!.isEnabled = enable
        mTvLostPassword!!.isEnabled = enable
    }

    private fun recoverPassword() {
        Statistics.INSTANCE.trackEvent(EventName.EDITOR_LOST_PASSWORD)
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(Constants.Url.OSM_RECOVER_PASSWORD)
            )
        )
    }
}