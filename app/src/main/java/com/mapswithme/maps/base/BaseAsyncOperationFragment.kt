package com.mapswithme.maps.base

import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.mapswithme.maps.R
import com.mapswithme.maps.dialog.ProgressDialogFragment
import java.util.*

abstract class BaseAsyncOperationFragment : BaseMwmFragment() {
    protected fun showProgress() {
        val resId = progressMessageId
        val title = getString(resId)
        val dialog = ProgressDialogFragment.newInstance(title)
        fragmentManager?.let {
            it.beginTransaction()
            .add(dialog, PROGRESS_DIALOG_TAG)
            .commitAllowingStateLoss()
        }
    }


    protected open val progressMessageId: Int
        @StringRes get() = R.string.downloading

    protected fun hideProgress() {
        val fm = fragmentManager
        val frag = fm?.findFragmentByTag(PROGRESS_DIALOG_TAG) as DialogFragment?
        frag?.dismissAllowingStateLoss()
    }

    companion object {
        private const val PROGRESS_DIALOG_TAG = "base_progress_dialog"
    }
}