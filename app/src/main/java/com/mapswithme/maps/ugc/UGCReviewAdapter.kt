package com.mapswithme.maps.ugc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.RatingView
import com.mapswithme.util.DateUtils
import java.util.*

internal class UGCReviewAdapter :
    RecyclerView.Adapter<UGCReviewAdapter.ViewHolder>() {
    private val mItems =
        ArrayList<UGC.Review>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ugc_comment, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(mItems[position])
    }

    override fun getItemCount(): Int {
        return Math.min(mItems.size, MAX_COUNT)
    }

    fun setItems(items: List<UGC.Review>) {
        mItems.clear()
        mItems.addAll(items)
        notifyDataSetChanged()
    }

    internal class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val mAuthor: TextView
        val mCommentDate: TextView
        val mReview: TextView
        val mRating: RatingView
        fun bind(review: UGC.Review) {
            mAuthor.text = review.author
            mCommentDate.text = DateUtils.getMediumDateFormatter().format(
                Date(
                    review.time
                )
            )
            mReview.text = review.text
            mRating.setRating(
                Impress.values()[review.impress],
                review.rating.toString()
            )
        }

        init {
            mAuthor = itemView.findViewById<View>(R.id.name) as TextView
            mCommentDate = itemView.findViewById<View>(R.id.date) as TextView
            mReview = itemView.findViewById<View>(R.id.review) as TextView
            mRating = itemView.findViewById<View>(R.id.rating) as RatingView
            // TODO: remove "gone" visibility when review rating behaviour is fixed on the server.
            mRating.visibility = View.GONE
        }
    }

    companion object {
        const val MAX_COUNT = 3
    }
}