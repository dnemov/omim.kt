package com.mapswithme.maps.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.util.InputUtils

class EditTextDialogFragment : BaseMwmDialogFragment() {
    private var mTitle: String? = null
    private var mInitialText: String? = null
    private var mHint: String? = null
    private var mEtInput: EditText? = null

    interface EditTextDialogInterface {
        val saveTextListener: OnTextSaveListener
        val validator: Validator
    }

    interface OnTextSaveListener {
        fun onSaveText(text: String)
    }

    interface Validator {
        fun validate(activity: Activity, text: String?): Boolean
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments
        var positiveButtonText: String? = getString(R.string.ok)
        var negativeButtonText: String? = getString(R.string.cancel)
        if (args != null) {
            mTitle = args.getString(ARG_TITLE)
            mInitialText = args.getString(ARG_INITIAL)
            mHint = args.getString(ARG_HINT)
            positiveButtonText =
                args.getString(ARG_POSITIVE_BUTTON)
            negativeButtonText =
                args.getString(ARG_NEGATIVE_BUTTON)
        }
        return AlertDialog.Builder(activity!!)
            .setView(buildView())
            .setNegativeButton(negativeButtonText, null)
            .setPositiveButton(
                positiveButtonText
            ) { dialog: DialogInterface?, which: Int ->
                val parentFragment = parentFragment
                val result = mEtInput!!.text.toString()
                if (parentFragment is EditTextDialogInterface) {
                    dismiss()
                    processInput(parentFragment as EditTextDialogInterface, result)
                    return@setPositiveButton
                }
                val activity: Activity? = activity
                if (activity is EditTextDialogInterface) {
                    processInput(activity as EditTextDialogInterface, result)
                }
            }.create()
    }

    private fun processInput(
        editInterface: EditTextDialogInterface,
        text: String?
    ) {
        val validator =
            editInterface.validator
        if (!validator.validate(activity!!, text)) return
        if (TextUtils.isEmpty(text)) throw AssertionError("Input must be non-empty!")
        editInterface.saveTextListener.onSaveText(text!!)
    }

    private fun buildView(): View {
        @SuppressLint("InflateParams") val root =
            activity!!.layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val inputLayout: TextInputLayout = root.findViewById(R.id.input)
        inputLayout.hint = if (TextUtils.isEmpty(mHint)) getString(R.string.name) else mHint
        mEtInput = inputLayout.findViewById(R.id.et__input)
        val maxLength =
            arguments!!.getInt(ARG_TEXT_LENGTH_LIMIT)
        if (maxLength != NO_LIMITED_TEXT_LENGTH) {
            val f = arrayOf<InputFilter>(LengthFilter(maxLength))
            mEtInput?.setFilters(f)
        }
        if (!TextUtils.isEmpty(mInitialText)) {
            mEtInput?.setText(mInitialText)
            mEtInput?.selectAll()
        }
        InputUtils.showKeyboard(mEtInput)
        (root.findViewById<View>(R.id.tv__title) as TextView).text = mTitle
        return root
    }

    companion object {
        const val ARG_TITLE = "arg_dialog_title"
        const val ARG_INITIAL = "arg_initial"
        const val ARG_POSITIVE_BUTTON = "arg_positive_button"
        const val ARG_NEGATIVE_BUTTON = "arg_negative_button"
        const val ARG_HINT = "arg_hint"
        const val ARG_TEXT_LENGTH_LIMIT = "arg_text_length_limit"
        private const val NO_LIMITED_TEXT_LENGTH = -1
        fun show(
            title: String?, initialText: String?,
            positiveBtn: String?, negativeBtn: String?,
            parent: Fragment
        ) {
            show(
                title,
                initialText,
                "",
                positiveBtn,
                negativeBtn,
                NO_LIMITED_TEXT_LENGTH,
                parent
            )
        }

        fun show(
            title: String?, initialText: String?,
            positiveBtn: String?, negativeBtn: String?,
            textLimit: Int, parent: Fragment
        ) {
            show(
                title,
                initialText,
                "",
                positiveBtn,
                negativeBtn,
                textLimit,
                parent
            )
        }

        fun show(
            title: String?, initialText: String?, hint: String?,
            positiveBtn: String?, negativeBtn: String?,
            parent: Fragment
        ) {
            show(
                title,
                initialText,
                hint,
                positiveBtn,
                negativeBtn,
                NO_LIMITED_TEXT_LENGTH,
                parent
            )
        }

        fun show(
            title: String?, initialText: String?, hint: String?,
            positiveBtn: String?, negativeBtn: String?, textLimit: Int,
            parent: Fragment
        ) {
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_INITIAL, initialText)
            args.putString(
                ARG_POSITIVE_BUTTON,
                positiveBtn?.toUpperCase()
            )
            args.putString(
                ARG_NEGATIVE_BUTTON,
                negativeBtn?.toUpperCase()
            )
            args.putString(ARG_HINT, hint)
            args.putInt(ARG_TEXT_LENGTH_LIMIT, textLimit)
            val fragment = Fragment.instantiate(
                parent.activity!!,
                EditTextDialogFragment::class.java.name
            ) as EditTextDialogFragment
            fragment.arguments = args
            fragment.show(
                parent.childFragmentManager,
                EditTextDialogFragment::class.java.name
            )
        }
    }
}