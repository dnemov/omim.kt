package com.mapswithme.maps.search

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Pair
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.mapswithme.maps.R
import com.mapswithme.maps.search.HotelsFilter
import com.mapswithme.maps.search.HotelsFilter.RatingFilter
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics

class RatingFilterView : LinearLayout, View.OnClickListener {
    private class Item(
        val mFrame: View,
        val mTitle: TextView,
        val mSubtitle: TextView?
    ) {
        fun select(select: Boolean) {
            @DrawableRes val background =
                UiUtils.getStyledResourceId(mFrame.context, R.attr.filterPropertyBackground)
            @ColorRes val titleColor = if (select) UiUtils.getStyledResourceId(
                mFrame.context,
                R.attr.accentButtonTextColor
            ) else UiUtils.getStyledResourceId(mFrame.context, android.R.attr.textColorPrimary)
            @ColorRes val subtitleColor = if (select) UiUtils.getStyledResourceId(
                mFrame.context,
                android.R.attr.textColorSecondaryInverse
            ) else UiUtils.getStyledResourceId(
                mFrame.context,
                android.R.attr.textColorSecondary
            )
            if (!select) mFrame.setBackgroundResource(background) else mFrame.setBackgroundColor(
                ContextCompat.getColor(
                    mFrame.context,
                    UiUtils.getStyledResourceId(mFrame.context, R.attr.colorAccent)
                )
            )
            mTitle.setTextColor(ContextCompat.getColor(mFrame.context, titleColor))
            mSubtitle?.setTextColor(
                ContextCompat.getColor(
                    mFrame.context,
                    subtitleColor
                )
            )
        }

    }

    var filter: RatingFilter? = null
        private set
    private val mItems =
        SparseArray<Item>()

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }

    private fun init(context: Context) {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.rating_filter, this, true)
    }

    override fun onFinishInflate() {
        val any = findViewById<View>(R.id.any)
        any.setOnClickListener(this)
        mItems.append(
            R.id.any,
            Item(
                any,
                findViewById(R.id.any_title),
                null
            )
        )
        val good = findViewById<View>(R.id.good)
        good.setOnClickListener(this)
        mItems.append(
            R.id.good, Item(
                good, findViewById(R.id.good_title),
                findViewById(R.id.good_subtitle)
            )
        )
        val veryGood = findViewById<View>(R.id.very_good)
        veryGood.setOnClickListener(this)
        mItems.append(
            R.id.very_good, Item(
                veryGood, findViewById(R.id.very_good_title),
                findViewById(R.id.very_good_subtitle)
            )
        )
        val excellent = findViewById<View>(R.id.excellent)
        excellent.setOnClickListener(this)
        mItems.append(
            R.id.excellent, Item(
                excellent, findViewById(R.id.excellent_title),
                findViewById(R.id.excellent_subtitle)
            )
        )
    }

    fun update(filter: RatingFilter?) {
        this.filter = filter
        if (this.filter == null) select(R.id.any) else if (filter!!.mValue == GOOD) select(
            R.id.good
        ) else if (filter.mValue == VERY_GOOD) select(R.id.very_good) else if (filter.mValue == EXCELLENT) select(
            R.id.excellent
        )
    }

    private fun select(id: Int) {
        for (i in 0 until mItems.size()) {
            val key = mItems.keyAt(i)
            val item = mItems.valueAt(i)
            item.select(key == id)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.any -> {
                Statistics.INSTANCE.trackFilterClick(
                    Statistics.EventParam.HOTEL,
                    Pair(
                        Statistics.EventParam.RATING,
                        Statistics.ParamValue.ANY
                    )
                )
                update(null)
            }
            R.id.good -> {
                Statistics.INSTANCE.trackFilterClick(
                    Statistics.EventParam.HOTEL,
                    Pair(
                        Statistics.EventParam.RATING,
                        GOOD.toString()
                    )
                )
                update(
                    RatingFilter(
                        HotelsFilter.Op.OP_GE,
                        GOOD
                    )
                )
            }
            R.id.very_good -> {
                Statistics.INSTANCE.trackFilterClick(
                    Statistics.EventParam.HOTEL,
                    Pair(
                        Statistics.EventParam.RATING,
                        VERY_GOOD.toString()
                    )
                )
                update(
                    RatingFilter(
                        HotelsFilter.Op.OP_GE,
                        VERY_GOOD
                    )
                )
            }
            R.id.excellent -> {
                Statistics.INSTANCE.trackFilterClick(
                    Statistics.EventParam.HOTEL,
                    Pair(
                        Statistics.EventParam.RATING,
                        EXCELLENT.toString()
                    )
                )
                update(
                    RatingFilter(
                        HotelsFilter.Op.OP_GE,
                        EXCELLENT
                    )
                )
            }
        }
    }

    companion object {
        const val GOOD = 7.0f
        const val VERY_GOOD = 8.0f
        const val EXCELLENT = 9.0f
    }
}