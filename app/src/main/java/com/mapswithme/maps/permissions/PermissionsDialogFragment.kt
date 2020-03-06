package com.mapswithme.maps.permissions

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.mapswithme.maps.R

import com.mapswithme.maps.news.OnboardingStep
import com.mapswithme.util.statistics.Statistics

class PermissionsDialogFragment : BasePermissionsDialogFragment() {
    @get:DrawableRes
    protected override val imageRes: Int
        protected get() = R.drawable.img_welcome

    @get:StringRes
    protected override val titleRes: Int
        protected get() = R.string.onboarding_permissions_title

    @get:StringRes
    protected override val subtitleRes: Int
        protected get() = R.string.onboarding_permissions_message

    @get:LayoutRes
    protected override val layoutRes: Int
        protected get() = R.layout.fragment_permissions

    @get:IdRes
    protected override val firstActionButton: Int
        protected get() = R.id.decline_btn

    override fun onFirstActionClick() {
        PermissionsDetailDialogFragment.Companion.show(activity!!, requestCode)
        sendStatistics(Statistics.EventName.ONBOARDING_SCREEN_DECLINE)
    }

    @get:IdRes
    protected override val continueActionButton: Int
        protected get() = R.id.accept_btn

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (savedInstanceState == null) sendStatistics(Statistics.EventName.ONBOARDING_SCREEN_SHOW)
        return dialog
    }

    override fun dismiss() {
        val dialog: DialogFragment? =
            PermissionsDetailDialogFragment.Companion.find(activity!!)
        dialog?.dismiss()
        super.dismiss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        activity!!.finish()
    }

    override fun onContinueBtnClicked(v: View?) {
        super.onContinueBtnClicked(v)
        sendStatistics(Statistics.EventName.ONBOARDING_SCREEN_ACCEPT)
    }

    private fun sendStatistics(event: String) {
        val value = OnboardingStep.PERMISSION_EXPLANATION.toStatisticValue()
        val builder = Statistics.params()
            .add(Statistics.EventParam.TYPE, value)
        Statistics.INSTANCE.trackEvent(event, builder)
    }

    companion object {
        @kotlin.jvm.JvmStatic
        fun show(
            activity: FragmentActivity,
            requestCode: Int
        ): DialogFragment? {
            return BasePermissionsDialogFragment.Companion.show(
                activity,
                requestCode,
                PermissionsDialogFragment::class.java
            )
        }

        @kotlin.jvm.JvmStatic
        fun find(activity: FragmentActivity): DialogFragment? {
            val fm = activity.supportFragmentManager
            if (fm.isDestroyed) return null
            val f = fm.findFragmentByTag(
                PermissionsDialogFragment::class.java.name
            )
            return f as DialogFragment?
        }
    }
}