package com.mapswithme.maps.review

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.maps.widget.recycler.RecyclerClickListener
import java.util.*

class ReviewFragment : BaseMwmFragment(), RecyclerClickListener {
    private var mItems: ArrayList<Review>? = null
    private var mRating: String? = null
    private var mRatingBase = 0
    private var mUrl: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_review, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        readArguments()
        if (mItems != null && mRating != null) {
            val rvGallery =
                view.findViewById<View>(R.id.rv__review) as RecyclerView
            rvGallery.layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.VERTICAL,
                false
            )
            rvGallery.adapter = ReviewAdapter(
                mItems!!,
                this,
                mRating!!,
                mRatingBase
            )
        }
    }

    private fun readArguments() {
        val arguments = arguments ?: return
        mItems = arguments.getParcelableArrayList(ReviewActivity.Companion.EXTRA_REVIEWS)
        mRating = arguments.getString(ReviewActivity.Companion.EXTRA_RATING)
        mRatingBase = arguments.getInt(ReviewActivity.Companion.EXTRA_RATING_BASE)
        mUrl = arguments.getString(ReviewActivity.Companion.EXTRA_RATING_URL)
    }

    override fun onItemClick(v: View?, position: Int) {
        if (mUrl == null) return
        val intent = Intent(Intent.ACTION_VIEW)
        var url: String = mUrl!!
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
        intent.data = Uri.parse(url)
        context!!.startActivity(intent)
    }
}