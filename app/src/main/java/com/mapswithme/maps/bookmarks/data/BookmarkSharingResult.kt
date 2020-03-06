package com.mapswithme.maps.bookmarks.data

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

class BookmarkSharingResult private constructor(
    val categoryId: Long, @field:Code @param:Code val code: Int,
    val sharingPath: String,
    val errorString: String
) {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        SUCCESS,
        EMPTY_CATEGORY,
        ARCHIVE_ERROR,
        FILE_ERROR
    )
    annotation class Code

    companion object {
        const val SUCCESS = 0
        const val EMPTY_CATEGORY = 1
        const val ARCHIVE_ERROR = 2
        const val FILE_ERROR = 3
    }

}