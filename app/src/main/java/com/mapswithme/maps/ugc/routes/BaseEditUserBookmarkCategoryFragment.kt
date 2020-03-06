package com.mapswithme.maps.ugc.routes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmToolbarFragment
import com.mapswithme.maps.bookmarks.data.BookmarkCategory
import java.util.*

abstract class BaseEditUserBookmarkCategoryFragment : BaseMwmToolbarFragment() {
    protected var editText: EditText? = null
    private lateinit var mCharactersAmountText: TextView
    private var mTextLimit = 0
    protected var category: BookmarkCategory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.category =
                it.getParcelable(BUNDLE_BOOKMARK_CATEGORY)
            mTextLimit = it.getInt(
                TEXT_LENGTH_LIMIT,
                defaultTextLengthLimit
            )
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(
            R.layout.fragment_bookmark_category_restriction, container,
            false
        )
        setHasOptionsMenu(true)
        editText = root.findViewById(R.id.edit_text_field)
        val inputFilters = arrayOf<InputFilter>(LengthFilter(mTextLimit))
        val editTextContainer: TextInputLayout = root.findViewById(R.id.edit_text_container)
        val hint = getString(hintText)
        editTextContainer.hint = hint
        editText?.filters = inputFilters
        editText?.setText(editableText)
        editText?.addTextChangedListener(TextRestrictionWatcher())
        mCharactersAmountText = root.findViewById(R.id.characters_amount)
        mCharactersAmountText.text = makeFormattedCharsAmount(
            editableText,
            mTextLimit
        )
        val summaryView = root.findViewById<TextView>(R.id.summary)
        summaryView.text = topSummaryText
        summaryView.append(DOUBLE_BREAK_LINE_CHAR)
        summaryView.append(bottomSummaryText)
        return root
    }

    protected abstract val topSummaryText: CharSequence
    protected abstract val bottomSummaryText: CharSequence
    @get:StringRes
    protected abstract val hintText: Int

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        inflater.inflate(R.menu.menu_bookmark_category_restriction, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val item = menu.findItem(R.id.done)
        item.isVisible = editText?.editableText?.length!! > 0
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            activity!!.setResult(Activity.RESULT_OK, data)
            activity!!.finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.done) {
            onDoneOptionItemClicked()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    protected abstract fun onDoneOptionItemClicked()
    protected abstract val editableText: CharSequence

    private inner class TextRestrictionWatcher : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence,
            start: Int,
            count: Int,
            after: Int
        ) { /* Do nothing by default. */
        }

        override fun onTextChanged(
            s: CharSequence,
            start: Int,
            before: Int,
            count: Int
        ) {
            if (s.length == 0 || s.length == 1) activity!!.invalidateOptionsMenu()
        }

        override fun afterTextChanged(s: Editable) {
            val src =
                makeFormattedCharsAmount(
                    s,
                    mTextLimit
                )
            mCharactersAmountText.text = src
        }
    }

    companion object {
        const val BUNDLE_BOOKMARK_CATEGORY = "category"
        private const val FORMAT_TEMPLATE = "%d / %d"
        private const val TEXT_LENGTH_LIMIT = "text_length_limit"
        protected const val defaultTextLengthLimit = 42
        private const val DOUBLE_BREAK_LINE_CHAR = "\n\n"
        private fun makeFormattedCharsAmount(s: CharSequence?, limit: Int): String {
            return String.format(
                Locale.US,
                FORMAT_TEMPLATE,
                if (s == null) 0 else Math.min(s.length, limit),
                limit
            )
        }
    }
}