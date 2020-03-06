package com.mapswithme.maps.bookmarks.data

import android.content.Context
import android.os.Parcelable
import android.text.TextUtils
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.BookmarksPageFactory
import com.mapswithme.util.TypeConverter
import kotlinx.android.parcel.Parcelize

@Parcelize
class BookmarkCategory(
    val id: Long, val name: String, val authorId: String,
    val authorName: String, val annotation: String,
    val description: String, val tracksCount: Int, val bookmarksCount: Int,
    val fromCatalog: Boolean, val isMyCategory: Boolean, val isVisible: Boolean,
    val accessRulesIndex: Int, val serverId: String
): Parcelable {
    val author: Author?
    private val mTypeIndex: Int

    override fun hashCode(): Int {
        return (id xor (id ushr 32)).toInt()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as BookmarkCategory
        return id == that.id
    }

    val type: Type
        get() = Type.values()[mTypeIndex]

    val accessRules: AccessRules

    val isFromCatalog: Boolean
        get() = Type.values()[mTypeIndex] == Type.DOWNLOADED

    fun size(): Int {
        return bookmarksCount + tracksCount
    }

    val pluralsCountTemplate: CountAndPlurals
        get() {
            if (size() == 0) return CountAndPlurals(0, R.plurals.objects)
            if (bookmarksCount == 0) return CountAndPlurals(tracksCount, R.plurals.tracks)
            return if (tracksCount == 0) CountAndPlurals(
                bookmarksCount,
                R.plurals.places
            ) else CountAndPlurals(size(), R.plurals.objects)
        }

    val isSharingOptionsAllowed: Boolean
        get() {
            val rules = accessRules
            return rules != AccessRules.ACCESS_RULES_PAID && rules != AccessRules.ACCESS_RULES_P2P && size() > 0
        }

    val isExportAllowed: Boolean
        get() {
            val rules = accessRules
            val isLocal = rules == AccessRules.ACCESS_RULES_LOCAL
            return isLocal && size() > 0
        }

    class CountAndPlurals(val count: Int, @field:PluralsRes val plurals: Int)

    @Parcelize
    class Author(
        val id: String,
        val name: String
    ) : Parcelable {

        companion object {

            @kotlin.jvm.JvmStatic
            fun getRepresentation(
                context: Context,
                author: Author
            ): String? {
                val res = context.resources
                return String.format(
                    res.getString(R.string.author_name_by_prefix),
                    author.name
                )
            }
        }

    }

    class Downloaded :
        TypeConverter<BookmarkCategory?, Boolean?> {

        override fun convert(data: BookmarkCategory?): Boolean? {
            return data?.type == Type.DOWNLOADED
        }
    }

    enum class Type(
        val factory: BookmarksPageFactory,
        val filterStrategy: FilterStrategy
    ) {
        PRIVATE(
            BookmarksPageFactory.PRIVATE,
            FilterStrategy.PredicativeStrategy.makePrivateInstance()
        ),
        DOWNLOADED(
            BookmarksPageFactory.DOWNLOADED,
            FilterStrategy.PredicativeStrategy.makeDownloadedInstance()
        );

    }

    enum class AccessRules(@get:StringRes val nameResId: Int, @get:DrawableRes val drawableResId: Int) {
        ACCESS_RULES_LOCAL(
            R.string.not_shared,
            R.drawable.ic_lock
        ),
        ACCESS_RULES_PUBLIC(
            R.string.public_access,
            R.drawable.ic_public_inline
        ),
        ACCESS_RULES_DIRECT_LINK(
            R.string.limited_access,
            R.drawable.ic_link_inline
        ),
        ACCESS_RULES_P2P(
            R.string.access_rules_p_to_p,
            R.drawable.ic_public_inline
        ),
        ACCESS_RULES_PAID(
            R.string.access_rules_paid,
            R.drawable.ic_public_inline
        ),
        ACCESS_RULES_AUTHOR_ONLY(R.string.access_rules_author_only, R.drawable.ic_lock);

    }

    init {
        mTypeIndex =
            if (fromCatalog && !isMyCategory) Type.DOWNLOADED.ordinal else Type.PRIVATE.ordinal
        author =
            if (TextUtils.isEmpty(authorId) || TextUtils.isEmpty(authorName)) null else Author(
                authorId,
                authorName
            )

        accessRules = AccessRules.values()[accessRulesIndex]
    }
}