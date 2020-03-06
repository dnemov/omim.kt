package com.mapswithme.maps.ugc.routes

import android.content.Intent
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkManager

class EditCategoryDescriptionFragment : BaseEditUserBookmarkCategoryFragment() {

    protected override val topSummaryText: CharSequence
        protected get() = getString(R.string.description_comment1)

    protected override val bottomSummaryText: CharSequence
        protected get() = ""

    protected override val hintText: Int
        protected get() = R.string.description_placeholder

    override fun onDoneOptionItemClicked() {
        BookmarkManager.INSTANCE.setCategoryDescription(
            category!!.id,
            editText?.text.toString().trim { it <= ' ' }
        )
        val intent = Intent(context, UgcRoutePropertiesActivity::class.java)
        startActivityForResult(
            intent,
            REQUEST_CODE_CUSTOM_PROPS
        )
    }

    override val editableText: CharSequence
        get() = category?.description.orEmpty()

    companion object {
        const val REQUEST_CODE_CUSTOM_PROPS = 100
        protected const val defaultTextLengthLimit = 500
    }
}