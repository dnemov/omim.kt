package com.mapswithme.maps.review

import android.text.TextUtils
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.recycler.RecyclerClickListener
import com.mapswithme.util.UiUtils
import java.util.*

internal class ReviewAdapter(
    private val mItems: ArrayList<Review>,
    private val mListener: RecyclerClickListener?,
    private val mRating: String,
    private val mRatingBase: Int
) : RecyclerView.Adapter<ReviewAdapter.BaseViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder {
        if (viewType == VIEW_TYPE_REVIEW) return ReviewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_comment, parent, false), mListener
        )
        return if (viewType == VIEW_TYPE_MORE) MoreHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_more_button, parent, false), mListener
        ) else RatingHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_rating, parent, false), mListener
        )
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int
    ) {
        val positionNoHeader = position - 1
        if (position == 0) (holder as RatingHolder).bind(
            mRating,
            mRatingBase
        ) else if (positionNoHeader < mItems.size) holder.bind(
            mItems[positionNoHeader],
            positionNoHeader
        ) else holder.bind(null, positionNoHeader)
    }

    override fun getItemCount(): Int {
        return if (mItems.size > MAX_COUNT) MAX_COUNT + 2 else mItems.size + 2
        // 1 overall rating item + count of user reviews + 1 "more reviews" item
    }

    override fun getItemViewType(position: Int): Int {
        val positionNoHeader = position - 1
        if (position == 0) return VIEW_TYPE_RATING
        return if (positionNoHeader == mItems.size) VIEW_TYPE_MORE else VIEW_TYPE_REVIEW
    }

    internal abstract class BaseViewHolder(
        itemView: View?,
        private val mListener: RecyclerClickListener?
    ) : RecyclerView.ViewHolder(itemView!!), View.OnClickListener {
        private var mPosition = 0
        override fun onClick(v: View) {
            mListener?.onItemClick(v, mPosition)
        }

        @CallSuper
        open fun bind(item: Review?, position: Int) {
            mPosition = position
        }

    }

    private class ReviewHolder internal constructor(
        itemView: View,
        listener: RecyclerClickListener?
    ) : BaseViewHolder(itemView, listener) {
        val mDivider: View
        val mUserName: TextView
        val mCommentDate: TextView
        val mRating: TextView
        val mPositiveReview: View
        val mTvPositiveReview: TextView
        val mNegativeReview: View
        val mTvNegativeReview: TextView
        override fun bind(
            item: Review?,
            position: Int
        ) {
            super.bind(item, position)
            UiUtils.showIf(position > 0, mDivider)
            mUserName.text = item!!.author
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
            mDivider = itemView.findViewById(R.id.v__divider)
            mUserName = itemView.findViewById<View>(R.id.tv__user_name) as TextView
            mCommentDate =
                itemView.findViewById<View>(R.id.tv__comment_date) as TextView
            mRating = itemView.findViewById<View>(R.id.tv__user_rating) as TextView
            mPositiveReview = itemView.findViewById(R.id.ll__positive_review)
            mTvPositiveReview =
                itemView.findViewById<View>(R.id.tv__positive_review) as TextView
            mNegativeReview = itemView.findViewById(R.id.ll__negative_review)
            mTvNegativeReview =
                itemView.findViewById<View>(R.id.tv__negative_review) as TextView
        }
    }

    private class MoreHolder internal constructor(
        itemView: View,
        listener: RecyclerClickListener?
    ) : BaseViewHolder(itemView, listener) {
        init {
            itemView.setOnClickListener(this)
        }
    }

    private class RatingHolder internal constructor(
        itemView: View,
        listener: RecyclerClickListener?
    ) : BaseViewHolder(itemView, listener) {
        val mHotelRating: TextView
        val mHotelRatingBase: TextView
        fun bind(rating: String?, ratingBase: Int) {
            mHotelRating.text = rating
            mHotelRatingBase.text = mHotelRatingBase.context.resources
                .getQuantityString(
                    R.plurals.place_page_booking_rating_base,
                    ratingBase, ratingBase
                )
        }

        init {
            mHotelRating =
                itemView.findViewById<View>(R.id.tv__place_hotel_rating) as TextView
            mHotelRatingBase =
                itemView.findViewById<View>(R.id.tv__place_hotel_rating_base) as TextView
        }
    }

    companion object {
        private const val MAX_COUNT = 15
        private const val VIEW_TYPE_REVIEW = 0
        private const val VIEW_TYPE_MORE = 1
        private const val VIEW_TYPE_RATING = 2
    }

}