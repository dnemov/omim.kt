package com.mapswithme.maps.permissions

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.R
import com.mapswithme.maps.SplashActivity
import com.mapswithme.maps.base.BaseMwmDialogFragment


class StoragePermissionsDialogFragment : BaseMwmDialogFragment(),
    View.OnClickListener {
    // We can't read actual theme, because permissions are not granted yet.
    override val customTheme: Int
        protected get() =// We can't read actual theme, because permissions are not granted yet.
            R.style.MwmTheme_DialogFragment_Fullscreen

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val res = super.onCreateDialog(savedInstanceState)
        res.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val content =
            View.inflate(activity, R.layout.fragment_storage_permissions, null)
        res.setContentView(content)
        val acceptBtn = content.findViewById<TextView>(R.id.accept_btn)
        acceptBtn.setOnClickListener(this)
        acceptBtn.setText(R.string.settings)
        val declineBtn = content.findViewById<TextView>(R.id.decline_btn)
        declineBtn.setOnClickListener(this)
        declineBtn.setText(R.string.back)
        val image =
            content.findViewById<View>(R.id.iv__image) as ImageView
        image.setImageResource(R.drawable.img_no_storage_permission)
        val title = content.findViewById<View>(R.id.tv__title) as TextView
        title.setText(R.string.onboarding_storage_permissions_title)
        val subtitle =
            content.findViewById<View>(R.id.tv__subtitle1) as TextView
        subtitle.setText(R.string.onboarding_storage_permissions_message)
        return res
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.decline_btn -> activity!!.finish()
            R.id.accept_btn -> {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri =
                    Uri.fromParts("package", context!!.packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        activity!!.finish()
    }

    companion object {
        @kotlin.jvm.JvmStatic
        fun show(activity: FragmentActivity): DialogFragment? {
            val fm = activity.supportFragmentManager
            if (fm.isDestroyed) return null
            val f = fm.findFragmentByTag(
                StoragePermissionsDialogFragment::class.java.name
            )
            if (f != null) return f as DialogFragment?
            val dialog = StoragePermissionsDialogFragment()
            dialog.show(fm, StoragePermissionsDialogFragment::class.java.name)
            return dialog
        }

        @kotlin.jvm.JvmStatic
        fun find(activity: SplashActivity): DialogFragment? {
            val fm = activity.supportFragmentManager
            if (fm.isDestroyed) return null
            val f = fm.findFragmentByTag(
                StoragePermissionsDialogFragment::class.java.name
            )
            return f as DialogFragment?
        }
    }
}