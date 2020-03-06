package com.mapswithme.maps.editor

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.util.Constants
import com.mapswithme.util.Graphics
import com.mapswithme.util.InputUtils
import com.mapswithme.util.UiUtils

class AdvancedTimetableFragment : BaseMwmFragment(),
    View.OnClickListener, TimetableProvider {
    private var mIsExampleShown = false
    private var mInput: EditText? = null
    private var mExample: WebView? = null
    private var mExamplesTitle: TextView? = null
    private var mInitTimetables: String? = null
    var mListener: TimetableChangedListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timetable_advanced, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        refreshTimetables()
        showExample(false)
    }

    override fun onResume() {
        super.onResume()
        refreshTimetables()
    }

    private fun initViews(view: View) {
        view.findViewById<View>(R.id.examples).setOnClickListener(this)
        mInput = view.findViewById<View>(R.id.et__timetable) as EditText
        mExample = view.findViewById<View>(R.id.wv__examples) as WebView
        mExample!!.loadUrl(Constants.Url.OPENING_HOURS_MANUAL)
        mExamplesTitle = view.findViewById<View>(R.id.tv__examples_title) as TextView
        setExampleDrawables(R.drawable.ic_type_text, R.drawable.ic_expand_more)
        setTextChangedListener(mInput, mListener)
    }

    private fun showExample(show: Boolean) {
        mIsExampleShown = show
        if (mIsExampleShown) {
            UiUtils.show(mExample)
            setExampleDrawables(R.drawable.ic_type_text, R.drawable.ic_expand_less)
        } else {
            UiUtils.hide(mExample!!)
            setExampleDrawables(R.drawable.ic_type_text, R.drawable.ic_expand_more)
        }
    }

    private fun setExampleDrawables(@DrawableRes left: Int, @DrawableRes right: Int) {
        activity?.let {
            mExamplesTitle!!.setCompoundDrawablesWithIntrinsicBounds(
                Graphics.tint(it, left, R.attr.colorAccent), null,
                Graphics.tint(it, right, R.attr.colorAccent), null
            )
        }

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.examples -> showExample(!mIsExampleShown)
        }
    }

    override var timetables: String?
        get() = mInput!!.text.toString()
        set(timetables) {
            mInitTimetables = timetables
            refreshTimetables()
        }

    private fun refreshTimetables() {
        if (mInput == null || mInitTimetables == null) return
        mInput!!.setText(mInitTimetables)
        mInput!!.requestFocus()
        InputUtils.showKeyboard(mInput)
    }

    fun setTimetableChangedListener(listener: TimetableChangedListener) {
        mListener = listener
        setTextChangedListener(mInput, mListener)
    }

    companion object {
        private fun setTextChangedListener(
            input: EditText?,
            listener: TimetableChangedListener?
        ) {
            if (input == null || listener == null) return
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                }

                override fun afterTextChanged(s: Editable) {
                    listener.onTimetableChanged(s.toString())
                }
            })
        }
    }
}