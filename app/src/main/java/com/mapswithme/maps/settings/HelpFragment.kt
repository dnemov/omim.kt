package com.mapswithme.maps.settings

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mapswithme.maps.R
import com.mapswithme.maps.WebContainerDelegate
import com.mapswithme.util.Constants
import com.mapswithme.util.Utils
import com.mapswithme.util.statistics.AlohaHelper
import com.mapswithme.util.statistics.Statistics

class HelpFragment : BaseSettingsFragment() {
    private val mDialogClickListener: DialogInterface.OnClickListener =
        object : DialogInterface.OnClickListener {
            private fun sendGeneralFeedback() {
                Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.FEEDBACK_GENERAL)
                AlohaHelper.logClick(AlohaHelper.Settings.FEEDBACK_GENERAL)
                Utils.sendFeedback(requireActivity())
            }

            private fun reportBug() {
                Statistics.INSTANCE.trackEvent(Statistics.EventName.Settings.REPORT_BUG)
                AlohaHelper.logClick(AlohaHelper.Settings.REPORT_BUG)
                Utils.sendBugReport(requireActivity(), "Bugreport from user")
            }

            override fun onClick(dialog: DialogInterface, which: Int) {
                when (which) {
                    0 -> sendGeneralFeedback()
                    1 -> reportBug()
                }
            }
        }

    protected override val layoutRes: Int
        protected get() = R.layout.fragment_prefs_help

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        object : WebContainerDelegate(root!!, Constants.Url.FAQ) {
            override fun doStartActivity(intent: Intent?) {
                startActivity(intent)
            }
        }
        val feedback = root.findViewById<TextView>(R.id.feedback)
        feedback.setOnClickListener { v: View? ->
            AlertDialog.Builder(activity)
                .setTitle(R.string.feedback)
                .setNegativeButton(R.string.cancel, null)
                .setItems(
                    arrayOf<CharSequence>(
                        getString(R.string.feedback_general),
                        getString(R.string.report_a_bug)
                    ),
                    mDialogClickListener
                ).show()
        }
        return root
    }
}