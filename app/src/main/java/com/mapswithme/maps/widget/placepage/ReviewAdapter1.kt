package com.mapswithme.maps.widget.placepage

import android.text.TextUtils
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.review.Review
import com.mapswithme.util.UiUtils
import java.util.*

internal class ReviewAdapter :
    RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {
    private var mItems =
        ArrayList<Review>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_comment, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(mItems[position], position > 0)
    }

    override fun getItemCount(): Int {
        return Math.min(
            mItems.size,
            MAX_COUNT
        )
    }

    var items: ArrayList<Review>
        get() = mItems
        set(items) {
            mItems = items
            notifyDataSetChanged()
        }

    internal class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mDivider: View
        val mUserName: TextView
        val mCommentDate: TextView
        val mRating: TextView
        val mPositiveReview: View
        val mTvPositiveReview: TextView
        val mNegativeReview: View
        val mTvNegativeReview: TextView
        fun bind(
            item: Review,
            isShowDivider: Boolean
        ) {
            UiUtils.showIf(isShowDivider, mDivider)
            mUserName.text = item.author
            val date = Date(item.date)
            mCommentDate.text = DateFormat.getMediumDateFormat(mCommentDate.context).format(
                date
            )
            mRating.text = String.format(
                Locale.getDefault(),
                "%.1f",
                item.rating
            )
            if (TextUtils.isEmpty(item.pros)) {
                UiUtils.hide(mPositiveReview)
            } else {
                UiUtils.show(mPositiveReview)
                mTvPositiveReview.text = item.pros
            }
            if (TextUtils.isEmpty(item.cons)) {
                UiUtils.hide(mNegativeReview)
            } else {
                UiUtils.show(mNegativeReview)
                mTvNegativeReview.text = item.cons
            }
        }

        init {
            mDivider = view.findViewById(R.id.v__divider)
            mUserName = view.findViewById<View>(R.id.tv__user_name) as TextView
            mCommentDate = view.findViewById<View>(R.id.tv__comment_date) as TextView
            mRating = view.findViewById<View>(R.id.tv__user_rating) as TextView
            mPositiveReview = view.findViewById(R.id.ll__positive_review)
            mTvPositiveReview =
                view.findViewById<View>(R.id.tv__positive_review) as TextView
            mNegativeReview = view.findViewById(R.id.ll__negative_review)
            mTvNegativeReview =
                view.findViewById<View>(R.id.tv__negative_review) as TextView
        }
    }

    companion object {
        private const val MAX_COUNT = 3
    }
}