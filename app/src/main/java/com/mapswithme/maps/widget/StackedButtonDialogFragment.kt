package com.mapswithme.maps.widget

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.StackedButtonsDialog
import com.mapswithme.util.Config
import com.mapswithme.util.NetworkPolicy
import com.mapswithme.util.NetworkPolicy.NetworkPolicyListener
import com.mapswithme.util.statistics.Statistics

class StackedButtonDialogFragment : DialogFragment() {
    private var mListener: NetworkPolicyListener? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return StackedButtonsDialog.Builder(context!!)
            .setTitle(R.string.mobile_data_dialog)
            .setMessage(R.string.mobile_data_description)
            .setCancelable(false)
            .setPositiveButton(
                R.string.mobile_data_option_always,
                DialogInterface.OnClickListener { dialog, which ->
                    onDialogBtnClicked(
                        NetworkPolicy.Type.ALWAYS,
                        true
                    )
                }
            )
            .setNegativeButton(
                R.string.mobile_data_option_not_today,
                DialogInterface.OnClickListener{ dialog: DialogInterface?, which: Int ->
                    onMobileDataImpactBtnClicked(
                        NetworkPolicy.Type.NOT_TODAY,
                        false
                    )
                }
            )
            .setNeutralButton(
                R.string.mobile_data_option_today,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    onMobileDataImpactBtnClicked(
                        NetworkPolicy.Type.TODAY,
                        true
                    )
                }
            )
            .build()
    }

    private fun onMobileDataImpactBtnClicked(
        today: NetworkPolicy.Type,
        canUse: Boolean
    ) {
        Config.setMobileDataTimeStamp(System.currentTimeMillis())
        onDialogBtnClicked(today, canUse)
    }

    private fun onDialogBtnClicked(type: NetworkPolicy.Type, canUse: Boolean) {
        Statistics.INSTANCE.trackNetworkUsageAlert(
            Statistics.EventName.MOBILE_INTERNET_ALERT,
            type.toStatisticValue()
        )
        Config.setUseMobileDataSettings(type)
        if (mListener != null) mListener!!.onResult(NetworkPolicy.newInstance(canUse))
    }

    override fun show(
        manager: FragmentManager,
        tag: String?
    ) {
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }

    fun setListener(listener: NetworkPolicyListener?) {
        mListener = listener
    }
}