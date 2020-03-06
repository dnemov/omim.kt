package com.mapswithme.maps.review

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseMwmExtraTitleActivity
import java.util.*

class ReviewActivity : BaseMwmExtraTitleActivity() {
    override val fragmentClass: Class<out Fragment>
        protected get() = ReviewFragment::class.java

    companion object {
        const val EXTRA_REVIEWS = "review_items"
        const val EXTRA_RATING = "review_rating"
        const val EXTRA_RATING_BASE = "review_rating_base"
        const val EXTRA_RATING_URL = "review_rating_url"
        fun start(
            context: Context,
            items: ArrayList<Review?>,
            title: String,
            rating: String,
            ratingBase: Int,
            url: String
        ) {
            val i = Intent(context, ReviewActivity::class.java)
            i.putParcelableArrayListExtra(EXTRA_REVIEWS, items)
            i.putExtra(EXTRA_TITLE, title)
            i.putExtra(EXTRA_RATING, rating)
            i.putExtra(EXTRA_RATING_BASE, ratingBase)
            i.putExtra(EXTRA_RATING_URL, url)
            context.startActivity(i)
        }
    }
}