package com.mapswithme.maps.widget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.SearchToolbarController
import com.mapswithme.util.InputUtils
import com.mapswithme.util.StringUtils.SimpleTextWatcher
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.AlohaHelper

open class SearchToolbarController(root: View, activity: Activity?) :
    ToolbarController(root, activity),
    View.OnClickListener {
    private val mContainer: View
    private val mQuery: EditText
    protected val mProgress: View
    private val mClear: View
    private val mVoiceInput: View
    private val mVoiceInputSupported =
        InputUtils.isVoiceInputSupported(activity)
    private val mTextWatcher: TextWatcher = object : SimpleTextWatcher() {
        override fun onTextChanged(
            s: CharSequence,
            start: Int,
            before: Int,
            count: Int
        ) {
            updateButtons(TextUtils.isEmpty(s))
            this@SearchToolbarController.onTextChanged(s.toString())
        }
    }

    interface Container {
        val controller: SearchToolbarController?
    }

    private fun updateButtons(queryEmpty: Boolean) {
        UiUtils.showIf(supportsVoiceSearch() && queryEmpty && mVoiceInputSupported, mVoiceInput)
        UiUtils.showIf(alwaysShowClearButton() || !queryEmpty, mClear)
    }

    protected open fun onQueryClick(query: String) {}
    protected open fun onTextChanged(query: String) {}
    protected open fun onStartSearchClick(): Boolean {
        return true
    }

    protected open fun onClearClick() {
        clear()
    }

    protected open fun startVoiceRecognition(intent: Intent?, code: Int) {
        throw RuntimeException("To be used startVoiceRecognition() must be implemented by descendant class")
    }

    /**
     * Return true to display & activate voice search. Turned OFF by default.
     */
    protected open fun supportsVoiceSearch(): Boolean {
        return false
    }

    protected open fun alwaysShowClearButton(): Boolean {
        return false
    }

    private fun onVoiceInputClick() {
        try {
            startVoiceRecognition(
                InputUtils.createIntentForVoiceRecognition(
                    activity?.getString(
                        voiceInputPrompt
                    )
                ), REQUEST_VOICE_RECOGNITION
            )
        } catch (e: ActivityNotFoundException) {
            AlohaHelper.logException(e)
        }
    }

    @get:StringRes
    protected open val voiceInputPrompt: Int
        protected get() = R.string.search

    var query: String
        get() = if (UiUtils.isVisible(mContainer)) mQuery.text.toString() else ""
        set(query) {
            mQuery.setText(query)
            if (!TextUtils.isEmpty(query)) mQuery.setSelection(query.length)
        }

    open fun clear() {
        query = ""
    }

    fun hasQuery(): Boolean {
        return !query.isEmpty()
    }

    fun activate() {
        mQuery.requestFocus()
        InputUtils.showKeyboard(mQuery)
    }

    fun deactivate() {
        InputUtils.hideKeyboard(mQuery)
        InputUtils.removeFocusEditTextHack(mQuery)
    }

    fun showProgress(show: Boolean) {
        UiUtils.showIf(show, mProgress)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.query -> onQueryClick(query)
            R.id.clear -> onClearClick()
            R.id.voice_input -> onVoiceInputClick()
        }
    }

    fun showControls(show: Boolean) {
        UiUtils.showIf(show, mContainer)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_VOICE_RECOGNITION && resultCode == Activity.RESULT_OK) {
            val result = InputUtils.getBestRecognitionResult(data!!)
            if (!TextUtils.isEmpty(result)) query = result ?: ""
        }
    }

    fun setHint(@StringRes hint: Int) {
        mQuery.setHint(hint)
    }

    companion object {
        private const val REQUEST_VOICE_RECOGNITION = 0xCA11
    }

    init {
        mContainer = toolbar.findViewById(R.id.frame)
        mQuery = mContainer.findViewById(R.id.query)
        mQuery.setOnClickListener(this)
        mQuery.addTextChangedListener(mTextWatcher)
        mQuery.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            val isSearchDown =
                event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_SEARCH
            val isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH
            (isSearchDown || isSearchAction) && onStartSearchClick()
        }
        mProgress = mContainer.findViewById(R.id.progress)
        mVoiceInput = mContainer.findViewById(R.id.voice_input)
        mVoiceInput.setOnClickListener(this)
        mClear = mContainer.findViewById(R.id.clear)
        mClear.setOnClickListener(this)
        showProgress(false)
        updateButtons(true)
    }
}