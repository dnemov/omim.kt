package com.mapswithme.maps.bookmarks.data

class Track internal constructor(
    val trackId: Long,
    val categoryId: Long,
    val name: String,
    val lengthString: String,
    val color: Int
)