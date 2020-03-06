package com.mapswithme.maps.ugc.routes

import android.content.Intent
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkManager

class EditCategoryNameFragment : BaseEditUserBookmarkCategoryFragment() {
    protected override val hintText: Int
        protected get() = R.string.name_placeholder

    protected override val topSummaryText: CharSequence
        protected get() = getString(R.string.name_comment1)

    protected override val bottomSummaryText: CharSequence
        protected get() = getString(R.string.name_comment2)

    override fun onDoneOptionItemClicked() {
        BookmarkManager.INSTANCE.setCategoryName(
            category!!.id,
            editText?.text.toString().trim { it <= ' ' }
        )
        openNextScreen()
    }

    private fun openNextScreen() {
        val intent = Intent(context, EditCategoryDescriptionActivity::class.java)
        intent.putExtra(
            BaseEditUserBookmarkCategoryFragment.Companion.BUNDLE_BOOKMARK_CATEGORY,
            category
        )
        startActivityForResult(intent, REQ_CODE_EDIT_DESCRIPTION)
    }

    override val editableText: CharSequence
        get() = category?.name.orEmpty()

    companion object {
        const val REQ_CODE_EDIT_DESCRIPTION = 75
    }
}