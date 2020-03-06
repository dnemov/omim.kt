package com.mapswithme.maps.bookmarks

import android.app.Activity
import android.text.TextUtils
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.dialog.DialogUtils
import com.mapswithme.maps.dialog.EditTextDialogFragment

internal class CategoryValidator : EditTextDialogFragment.Validator {
    override fun validate(activity: Activity, text: String?): Boolean {
        if (TextUtils.isEmpty(text)) {
            DialogUtils.showAlertDialog(
                activity, R.string.bookmarks_error_title_empty_list_name,
                R.string.bookmarks_error_message_empty_list_name
            )
            return false
        }
        if (BookmarkManager.INSTANCE.isUsedCategoryName(text!!)) {
            DialogUtils.showAlertDialog(
                activity, R.string.bookmarks_error_title_list_name_already_taken,
                R.string.bookmarks_error_message_list_name_already_taken
            )
            return false
        }
        return true
    }
}