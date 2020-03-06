package com.mapswithme.maps.editor

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseMwmFragmentActivity

class EditorActivity : BaseMwmFragmentActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = EditorHostFragment::class.java

    companion object {
        const val EXTRA_NEW_OBJECT = "ExtraNewMapObject"
        @kotlin.jvm.JvmStatic
        fun start(activity: Activity) {
            val intent = Intent(activity, EditorActivity::class.java)
            activity.startActivity(intent)
        }
    }
}