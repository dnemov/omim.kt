package com.mapswithme.maps.editor

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.sharing.SharingHelper
import com.mapswithme.util.statistics.Statistics
import com.mapswithme.util.statistics.Statistics.EventName
import java.util.*

class ViralFragment : BaseMwmDialogFragment() {
    private var mViralText: String? = null
    private val viralChangesMsg =
        MwmApplication.get().getString(R.string.editor_done_dialog_1)
    private val viralRatingMsg = MwmApplication.get()
        .getString(R.string.editor_done_dialog_2, userEditorRank)
    private var mDismissListener: Runnable? = null
    override val style: Int
        protected get() = STYLE_NO_TITLE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        MwmApplication.prefs()?.edit()?.putBoolean(EXTRA_CONGRATS_SHOWN, true)
            ?.apply()
        @SuppressLint("InflateParams") val root =
            inflater.inflate(R.layout.fragment_editor_viral, null)
        val viralText = root.findViewById<View>(R.id.viral) as TextView
        initViralText()
        viralText.text = mViralText
        root.findViewById<View>(R.id.tell_friend)
            .setOnClickListener {
                share()
                dismiss()
                if (mDismissListener != null) mDismissListener!!.run()
                Statistics.INSTANCE.trackEvent(EventName.EDITOR_SHARE_CLICK)
            }
        root.findViewById<View>(R.id.close)
            .setOnClickListener {
                dismiss()
                if (mDismissListener != null) mDismissListener!!.run()
            }
        Statistics.INSTANCE.trackEvent(
            EventName.EDITOR_SHARE_SHOW,
            Statistics.params().add(
                "showed",
                if (mViralText == viralChangesMsg) "change" else "rating"
            )
        )
        return root
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (mDismissListener != null) mDismissListener!!.run()
    }

    fun onDismissListener(onDismissListener: Runnable?) {
        mDismissListener = onDismissListener
    }

    private fun share() {
        SharingHelper.shareViralEditor(
            activity,
            R.drawable.img_sharing_editor,
            R.string.editor_sharing_title,
            R.string.whatsnew_editor_message_1
        )
    }

    private fun initViralText() {
        mViralText = if (Random().nextBoolean()) viralChangesMsg else viralRatingMsg
    }

    companion object {
        private const val EXTRA_CONGRATS_SHOWN = "CongratsShown"
        @kotlin.jvm.JvmStatic
        fun shouldDisplay(): Boolean {
            return !(MwmApplication.prefs()?.contains(EXTRA_CONGRATS_SHOWN) ?: false) && Editor.nativeGetStats()[0] == 2L &&
                    ConnectionState.isConnected
        }

        // Counts fake rank in the rating of editors.
        private val userEditorRank: Int
            private get() = 1000 + Random().nextInt(1000)
    }
}