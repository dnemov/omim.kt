package com.mapswithme.maps.dialog

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.mapswithme.maps.R
import java.util.*

class ProgressDialogFragment : DialogFragment() {
    protected fun setCancelResult() {
        val targetFragment = targetFragment
        targetFragment?.onActivityResult(
            targetRequestCode,
            Activity.RESULT_CANCELED,
            null
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            retainInstance = it.getBoolean(
                ARG_RETAIN_INSTANCE,
                true
            )
        }
        isCancelable = arguments?.getBoolean(ARG_CANCELABLE, false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.indeterminated_progress_dialog, container, false)

        val messageView = view.findViewById<TextView>(R.id.message)
        messageView.text = arguments?.getString(ARG_MESSAGE)
        return view
    }

    override fun onCancel(dialog: DialogInterface) {
        setCancelResult()
    }

    val isShowing: Boolean
        get() {
            val dialog = dialog
            return dialog != null && dialog.isShowing
        }

    override fun onDestroyView() {
        if (dialog != null && retainInstance) dialog!!.setDismissMessage(null)
        super.onDestroyView()
    }

    companion object {
        private const val ARG_MESSAGE = "title"
        private const val ARG_CANCELABLE = "cancelable"
        private const val ARG_RETAIN_INSTANCE = "retain_instance"
        @JvmOverloads
        fun newInstance(
            message: String, cancelable: Boolean = false,
            retainInstance: Boolean = true
        ): ProgressDialogFragment {
            val fr = ProgressDialogFragment()
            fr.arguments = getArgs(
                message,
                cancelable,
                retainInstance
            )
            return fr
        }

        private fun getArgs(
            title: String,
            cancelable: Boolean,
            retainInstance: Boolean
        ): Bundle {
            val args = Bundle()
            args.putString(ARG_MESSAGE, title)
            args.putBoolean(ARG_CANCELABLE, cancelable)
            args.putBoolean(ARG_RETAIN_INSTANCE, retainInstance)
            return args
        }
    }
}