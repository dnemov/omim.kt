package com.mapswithme.maps.ugc

import android.app.Activity
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.ugc.UGC.Companion.setReceiveListener
import com.mapswithme.maps.ugc.UGC.ReceiveUGCListener
import com.mapswithme.maps.widget.RatingView
import com.mapswithme.maps.widget.placepage.PlacePageView
import com.mapswithme.maps.widget.recycler.ItemDecoratorFactory
import com.mapswithme.util.DateUtils
import com.mapswithme.util.UiUtils
import java.util.*

class UGCController(private val mPlacePage: PlacePageView) : View.OnClickListener,
    ReceiveUGCListener {
    private val mUgcRootView: View
    private val mUgcAddRatingView: View
    private val mUgcMoreReviews: View
    private val mUGCReviewAdapter = UGCReviewAdapter()
    private val mUGCRatingRecordsAdapter = UGCRatingRecordsAdapter()
    private val mUGCUserRatingRecordsAdapter = UGCRatingRecordsAdapter()
    private val mReviewCount: TextView
    private val mLeaveReviewButton: Button
    private val mPreviewUgcInfoView: View
    private val mUserRatingRecordsContainer: View
    private val mUserReviewView: View
    private val mUserReviewDivider: View
    private val mReviewListDivider: View
    private val mSummaryRootView: View
    private val mSummaryReviewCount: TextView
    private val mLeaveReviewClickListener =
        View.OnClickListener {
            if (mMapObject == null) return@OnClickListener
            val builder: EditParams.Builder =
                EditParams.Builder.Companion.fromMapObject(mMapObject!!)
                    .setDefaultRating(UGC.RATING_NONE)
                    .setFromPP(true)
            UGCEditorActivity.Companion.start(
                mPlacePage.context as Activity,
                builder.build()
            )
        }
    private val mMoreReviewsClickListener =
        View.OnClickListener {
            // TODO: coming soon.
        }
    private var mMapObject: MapObject? = null
    fun clearViewsFor(mapObject: MapObject) {
        UiUtils.hide(mUgcRootView, mLeaveReviewButton, mPreviewUgcInfoView)
        mUGCReviewAdapter.setItems(emptyList())
        mUGCRatingRecordsAdapter.setItems(emptyList())
        mUGCUserRatingRecordsAdapter.setItems(emptyList())
        mReviewCount.text = ""
        mSummaryReviewCount.text = ""
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ll__horrible -> onAggRatingTapped(UGC.RATING_HORRIBLE)
            R.id.ll__bad -> onAggRatingTapped(UGC.RATING_BAD)
            R.id.ll__normal -> onAggRatingTapped(UGC.RATING_NORMAL)
            R.id.ll__good -> onAggRatingTapped(UGC.RATING_GOOD)
            R.id.ll__excellent -> onAggRatingTapped(UGC.RATING_EXCELLENT)
            else -> throw AssertionError("Unknown rating view:")
        }
    }

    fun getUGC(mapObject: MapObject) {
        if (!mapObject.shouldShowUGC()) return
        mMapObject = mapObject
        mMapObject?.let {
            UGC.nativeRequestUGC(it.featureId);
        }
    }

    override fun onUGCReceived(
        ugc: UGC?, ugcUpdate: UGCUpdate?, @UGC.Impress impress: Int,
        rating: String
    ) {
        UiUtils.show(mUgcRootView)
        UiUtils.showIf(
            ugc != null || canUserRate(ugcUpdate) || ugcUpdate != null,
            mPreviewUgcInfoView
        )
        UiUtils.showIf(canUserRate(ugcUpdate), mLeaveReviewButton, mUgcAddRatingView)
        UiUtils.showIf(ugc != null, mUgcMoreReviews)
        UiUtils.showIf(ugc != null && impress != UGC.RATING_NONE, mSummaryRootView)
        val ratingView: RatingView = mPreviewUgcInfoView.findViewById(R.id.rating_view)
        if (ugc == null) {
            mReviewCount.setText(if (ugcUpdate != null) R.string.placepage_reviewed else R.string.placepage_no_reviews)
            ratingView.setRating(
                if (ugcUpdate == null) Impress.NONE else Impress.COMING_SOON,
                rating
            )
            setUserReviewAndRatingsView(ugcUpdate)
            return
        }
        val context = mPlacePage.context
        val reviewsCount = ugc.basedOnCount
        if (impress != UGC.RATING_NONE) {
            mReviewCount.text = context.resources.getQuantityString(
                R.plurals.placepage_summary_rating_description, reviewsCount, reviewsCount
            )
            setSummaryViews(ugc, impress, rating)
        }
        ratingView.setRating(Impress.values()[impress], rating)
        setUserReviewAndRatingsView(ugcUpdate)
        val reviews = ugc.reviews
        if (reviews != null) mUGCReviewAdapter.setItems(ugc.reviews!!)
        UiUtils.showIf(reviews != null, mReviewListDivider)
        // TODO: don't show "more reviews" button while reviews screen activity is not ready.
        UiUtils.showIf(
            false /* reviews != null && reviews.size() > UGCReviewAdapter.MAX_COUNT */,
            mUgcMoreReviews
        )
    }

    private fun setSummaryViews(
        ugc: UGC, @UGC.Impress impress: Int,
        rating: String
    ) {
        val ratingView =
            mSummaryRootView.findViewById<View>(R.id.rv__summary_rating) as RatingView
        ratingView.setRating(Impress.values()[impress], rating)
        val context = mPlacePage.context
        val reviewsCount = ugc.basedOnCount
        mSummaryReviewCount.text = context.resources.getQuantityString(
            R.plurals.placepage_summary_rating_description, reviewsCount, reviewsCount
        )
        mUGCRatingRecordsAdapter.setItems(ugc.ratings)
    }

    private fun canUserRate(ugcUpdate: UGCUpdate?): Boolean {
        return mMapObject != null && mMapObject!!.canBeRated() && ugcUpdate == null
    }

    private fun onAggRatingTapped(@UGC.Impress rating: Int) {
        if (mMapObject == null) return
        val builder: EditParams.Builder =
            EditParams.Builder.Companion.fromMapObject(mMapObject!!)
                .setDefaultRating(rating)
        UGCEditorActivity.Companion.start(mPlacePage.context as Activity, builder.build())
    }

    private fun setUserReviewAndRatingsView(update: UGCUpdate?) {
        UiUtils.showIf(
            update != null, mUserReviewView, mUserReviewDivider,
            mUserRatingRecordsContainer
        )
        if (update == null) return
        val name = mUserReviewView.findViewById<TextView>(R.id.name)
        val date = mUserReviewView.findViewById<TextView>(R.id.date)
        name.setText(R.string.placepage_reviews_your_comment)
        val formatter = DateUtils.getMediumDateFormatter()
        date.text = formatter.format(Date(update.mTimeMillis))
        val review = mUserReviewView.findViewById<TextView>(R.id.review)
        UiUtils.showIf(!TextUtils.isEmpty(update.mText), review)
        review.text = update.mText
        mUGCUserRatingRecordsAdapter.setItems(update.ratings)
    }

    init {
        val context = mPlacePage.context
        mUgcRootView = mPlacePage.findViewById(R.id.ll__pp_ugc)
        mPreviewUgcInfoView = mPlacePage.findViewById(R.id.preview_rating_info)
        mUgcMoreReviews = mPlacePage.findViewById(R.id.tv__pp_ugc_reviews_more)
        mUgcMoreReviews.setOnClickListener(mMoreReviewsClickListener)
        mUgcAddRatingView = mPlacePage.findViewById(R.id.ll__pp_ugc_add_rating)
        mUgcAddRatingView.findViewById<View>(R.id.ll__horrible)
            .setOnClickListener(this)
        mUgcAddRatingView.findViewById<View>(R.id.ll__bad).setOnClickListener(this)
        mUgcAddRatingView.findViewById<View>(R.id.ll__normal).setOnClickListener(this)
        mUgcAddRatingView.findViewById<View>(R.id.ll__good).setOnClickListener(this)
        mUgcAddRatingView.findViewById<View>(R.id.ll__excellent)
            .setOnClickListener(this)
        mReviewCount = mPlacePage.findViewById<View>(R.id.tv__review_count) as TextView
        mLeaveReviewButton =
            mPlacePage.findViewById<View>(R.id.leaveReview) as Button
        mLeaveReviewButton.setOnClickListener(mLeaveReviewClickListener)
        val rvUGCReviews =
            mPlacePage.findViewById<View>(R.id.rv__pp_ugc_reviews) as RecyclerView
        rvUGCReviews.layoutManager = LinearLayoutManager(context)
        rvUGCReviews.layoutManager!!.isAutoMeasureEnabled = true
        rvUGCReviews.addItemDecoration(
            ItemDecoratorFactory.createDefaultDecorator(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        rvUGCReviews.isNestedScrollingEnabled = false
        rvUGCReviews.setHasFixedSize(false)
        rvUGCReviews.adapter = mUGCReviewAdapter
        mSummaryRootView = mPlacePage.findViewById(R.id.ll__summary_container)
        val summaryRatingContainer =
            mSummaryRootView.findViewById<View>(R.id.summary_rating_records)
        val rvRatingRecords =
            summaryRatingContainer.findViewById<View>(R.id.rv__summary_rating_records) as RecyclerView
        rvRatingRecords.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        rvRatingRecords.layoutManager!!.isAutoMeasureEnabled = true
        rvRatingRecords.isNestedScrollingEnabled = false
        rvRatingRecords.setHasFixedSize(false)
        rvRatingRecords.addItemDecoration(
            ItemDecoratorFactory.createRatingRecordDecorator(
                context,
                LinearLayoutManager.HORIZONTAL
            )
        )
        rvRatingRecords.adapter = mUGCRatingRecordsAdapter
        mSummaryReviewCount =
            mSummaryRootView.findViewById<View>(R.id.tv__review_count) as TextView
        mUserRatingRecordsContainer =
            mPlacePage.findViewById(R.id.user_rating_records)
        val userRatingRecords =
            mUserRatingRecordsContainer.findViewById<View>(R.id.rv__summary_rating_records) as RecyclerView
        userRatingRecords.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        userRatingRecords.layoutManager!!.isAutoMeasureEnabled = true
        userRatingRecords.isNestedScrollingEnabled = false
        userRatingRecords.setHasFixedSize(false)
        userRatingRecords.addItemDecoration(
            ItemDecoratorFactory.createRatingRecordDecorator(
                context,
                LinearLayoutManager.HORIZONTAL
            )
        )
        userRatingRecords.adapter = mUGCUserRatingRecordsAdapter
        mUserReviewView = mPlacePage.findViewById(R.id.rl_user_review)
        mUserReviewView.findViewById<View>(R.id.rating).visibility = View.GONE
        mReviewListDivider =
            mPlacePage.findViewById(R.id.ugc_review_list_divider)
        mUserReviewDivider = mPlacePage.findViewById(R.id.user_review_divider)
        setReceiveListener(this)
    }
}