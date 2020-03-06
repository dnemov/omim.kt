package com.mapswithme.maps.bookmarks.data

import com.mapswithme.util.Predicate
import java.util.*

interface FilterStrategy {
    fun filter(items: List<BookmarkCategory>): List<BookmarkCategory>
    class All : FilterStrategy {
        override fun filter(items: List<BookmarkCategory>): List<BookmarkCategory> {
            return items
        }
    }

    class Private : PredicativeStrategy<Boolean?>(
        Predicate.Equals<Boolean?, BookmarkCategory?>(
            BookmarkCategory.Downloaded(),
            false
        )
    )

    class Downloaded internal constructor() : PredicativeStrategy<Boolean?>(
        Predicate.Equals<Boolean?, BookmarkCategory?>(
            BookmarkCategory.Downloaded(),
            true
        )
    )

    open class PredicativeStrategy<T> constructor(private val mPredicate: Predicate<T?, BookmarkCategory?>) :
        FilterStrategy {
        override fun filter(items: List<BookmarkCategory>): List<BookmarkCategory> {
            val result: MutableList<BookmarkCategory> =
                ArrayList()
            for (each in items) {
                if (mPredicate.apply(each)) result.add(each)
            }
            return Collections.unmodifiableList(result)
        }

        companion object {
            fun makeDownloadedInstance(): FilterStrategy {
                return Downloaded()
            }

            fun makePrivateInstance(): FilterStrategy {
                return Private()
            }
        }

    }
}