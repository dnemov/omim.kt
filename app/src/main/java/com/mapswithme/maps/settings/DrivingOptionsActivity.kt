package com.mapswithme.maps.settings

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.mapswithme.maps.MwmActivity
import com.mapswithme.maps.base.BaseMwmFragmentActivity

class DrivingOptionsActivity : BaseMwmFragmentActivity() {
    override val fragmentClass: Class<out Fragment>
        get() = DrivingOptionsFragment::class.java

    companion object {
        @kotlin.jvm.JvmStatic
        fun start(activity: Activity) {
            val intent = Intent(activity, DrivingOptionsActivity::class.java)
            activity.startActivityForResult(intent, MwmActivity.REQ_CODE_DRIVING_OPTIONS)
        }
    }
}