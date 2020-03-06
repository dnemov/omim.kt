package com.mapswithme.maps.permissions

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R


class PermissionsDetailDialogFragment : BasePermissionsDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val res = super.onCreateDialog(savedInstanceState)
        val permissions =
            res.findViewById<View>(R.id.rv__permissions) as RecyclerView
        permissions.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL, false
        )
        permissions.adapter = PermissionsAdapter()
        val acceptBtn = res.findViewById<TextView>(R.id.accept_btn)
        acceptBtn.setText(R.string.continue_download)
        val declineBtn = res.findViewById<TextView>(R.id.decline_btn)
        declineBtn.setText(R.string.back)
        return res
    }

    @get:LayoutRes
    protected override val layoutRes: Int
        protected get() = R.layout.fragment_detail_permissions

    @get:IdRes
    protected override val firstActionButton: Int
        protected get() = R.id.decline_btn

    override fun onFirstActionClick() {
        dismiss()
    }

    @get:IdRes
    protected override val continueActionButton: Int
        protected get() = R.id.accept_btn

    companion object {
        fun show(
            activity: FragmentActivity,
            requestCode: Int
        ): DialogFragment? {
            val dialog: DialogFragment? =
                BasePermissionsDialogFragment.show(
                    activity, requestCode,
                    PermissionsDetailDialogFragment::class.java
                )
            if (dialog != null) dialog.isCancelable = true
            return dialog
        }

        fun find(activity: FragmentActivity): DialogFragment? {
            val fm = activity.supportFragmentManager
            if (fm.isDestroyed) return null
            val f = fm.findFragmentByTag(
                PermissionsDetailDialogFragment::class.java.name
            )
            return f as DialogFragment?
        }
    }
}