package com.mapswithme.maps.widget.placepage

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.maps.bookmarks.data.Bookmark
import com.mapswithme.util.StringUtils
import com.mapswithme.util.UiUtils
import java.lang.ref.WeakReference

class EditDescriptionFragment : BaseMwmDialogFragment() {
    private var mEtDescription: EditText? = null
    private var mBookmark: Bookmark? = null

    interface OnDescriptionSavedListener {
        fun onSaved(bookmark: Bookmark?)
    }

    private var mListener: WeakReference<OnDescriptionSavedListener>? = null
    override val customTheme: Int
        get() = fullscreenTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_description, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        mBookmark = arguments!!.getParcelable(EXTRA_BOOKMARK)
        var description: String? = null
        if (mBookmark != null) description = mBookmark!!.bookmarkDescription
        if (description != null && StringUtils.nativeIsHtml(description)) {
            val descriptionNoSimpleTags =
                StringUtils.removeEditTextHtmlTags(description)
            if (!StringUtils.nativeIsHtml(descriptionNoSimpleTags)) description =
                Html.fromHtml(description).toString()
        }
        mEtDescription = view.findViewById<View>(R.id.et__description) as EditText
        mEtDescription!!.setText(description)
        initToolbar(view)
        mEtDescription!!.requestFocus()
        dialog!!.window
            ?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    fun setSaveDescriptionListener(listener: OnDescriptionSavedListener) {
        mListener = WeakReference(listener)
    }

    private fun initToolbar(view: View) {
        val toolbar =
            view.findViewById<View>(R.id.toolbar) as Toolbar
        UiUtils.extendViewWithStatusBar(toolbar)
        val textView = toolbar.findViewById<View>(R.id.tv__save) as TextView
        textView.setOnClickListener { saveDescription() }
        UiUtils.showHomeUpButton(toolbar)
        toolbar.setTitle(R.string.description)
        toolbar.setNavigationOnClickListener { dismiss() }
    }

    private fun saveDescription() {
        mBookmark!!.setParams(mBookmark!!.title, null, mEtDescription!!.text.toString())
        if (mListener != null) {
            val listener = mListener!!.get()
            listener?.onSaved(mBookmark)
        }
        dismiss()
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    companion object {
        const val EXTRA_BOOKMARK = "bookmark"
    }
}