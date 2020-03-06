package com.mapswithme.maps.bookmarks.data

import java.util.*

abstract class AbstractCategoriesSnapshot internal constructor(items: List<BookmarkCategory>) {

    protected open val items: List<BookmarkCategory> = Collections.unmodifiableList(items)

    class Default(
        items: List<BookmarkCategory>,
        private val mStrategy: FilterStrategy
    ) : AbstractCategoriesSnapshot(items) {

        public override val items: List<BookmarkCategory>
            get() = mStrategy.filter(super.items)

        fun indexOfOrThrow(category: BookmarkCategory): Int {
            return indexOfThrowInternal(
                items,
                category
            )
        }

        fun refresh(category: BookmarkCategory): BookmarkCategory {
            val items = items
            val index =
                indexOfThrowInternal(
                    items,
                    category
                )
            return items[index]
        }

        companion object {
            private fun indexOfThrowInternal(
                categories: List<BookmarkCategory>,
                category: BookmarkCategory
            ): Int {
                val indexOf = categories.indexOf(category)
                if (indexOf < 0) {
                    throw UnsupportedOperationException(
                        StringBuilder(
                            "This category absent in " +
                                    "current snapshot "
                        )
                            .append(category)
                            .append("all items : ")
                            .append(Arrays.toString(categories.toTypedArray()))
                            .toString()
                    )
                }
                return indexOf
            }
        }

    }

}