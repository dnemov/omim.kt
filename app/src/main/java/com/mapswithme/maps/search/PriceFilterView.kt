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
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import com.mapswithme.maps.R
import com.mapswithme.maps.search.HotelsFilter
import com.mapswithme.maps.search.HotelsFilter.PriceRateFilter
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.*

class PriceFilterView : LinearLayout, View.OnClickListener {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        UNDEFINED,
        LOW,
        MEDIUM,
        HIGH
    )
    annotation class PriceDef

    private class Item(val mFrame: View, val mTitle: TextView) {
        var mSelected = false
        fun select(select: Boolean) {
            mSelected = select
            update()
        }

        fun update() {
            @DrawableRes val background =
                UiUtils.getStyledResourceId(mFrame.context, R.attr.filterPropertyBackground)
            @ColorRes val titleColor = if (mSelected) UiUtils.getStyledResourceId(
                mFrame.context,
                R.attr.accentButtonTextColor
            ) else UiUtils.getStyledResourceId(mFrame.context, android.R.attr.textColorPrimary)
            if (!mSelected) mFrame.setBackgroundResource(background) else mFrame.setBackgroundColor(
                ContextCompat.getColor(
                    mFrame.context,
                    UiUtils.getStyledResourceId(mFrame.context, R.attr.colorAccent)
                )
            )
            mTitle.setTextColor(ContextCompat.getColor(mFrame.context, titleColor))
        }

    }

    var filter: HotelsFilter? = null
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
        LayoutInflater.from(context).inflate(R.layout.price_filter, this, true)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val low = findViewById<View>(R.id.low)
        low.setOnClickListener(this)
        mItems.append(
            R.id.low,
            Item(
                low,
                findViewById<View>(R.id.low_title) as TextView
            )
        )
        val medium = findViewById<View>(R.id.medium)
        medium.setOnClickListener(this)
        mItems.append(
            R.id.medium,
            Item(
                medium,
                findViewById<View>(R.id.medium_title) as TextView
            )
        )
        val high = findViewById<View>(R.id.high)
        high.setOnClickListener(this)
        mItems.append(
            R.id.high,
            Item(
                high,
                findViewById<View>(R.id.high_title) as TextView
            )
        )
    }

    fun update(filter: HotelsFilter?) {
        this.filter = filter
        deselectAll()
        if (this.filter != null) updateRecursive(this.filter!!)
    }

    private fun updateRecursive(filter: HotelsFilter) {
        if (filter is PriceRateFilter) {
            selectByValue(filter.mValue)
        } else if (filter is HotelsFilter.Or) {
            val or =
                filter
            updateRecursive(or.mLhs)
            updateRecursive(or.mRhs)
        } else {
            throw AssertionError("Wrong hotels filter type")
        }
    }

    private fun deselectAll() {
        for (i in 0 until mItems.size()) {
            val item = mItems.valueAt(i)
            item.select(false)
        }
    }

    private fun selectByValue(@PriceDef value: Int) {
        when (value) {
            LOW -> select(R.id.low, true)
            MEDIUM -> select(R.id.medium, true)
            HIGH -> select(R.id.high, true)
        }
    }

    private fun select(@IdRes id: Int, force: Boolean) {
        for (i in 0 until mItems.size()) {
            val key = mItems.keyAt(i)
            val item = mItems.valueAt(i)
            if (key == id) {
                item.select(force || !item.mSelected)
                return
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.low -> Statistics.INSTANCE.trackFilterClick(
                Statistics.EventParam.HOTEL,
                Pair(
                    Statistics.EventParam.PRICE_CATEGORY,
                    LOW.toString()
                )
            )
            R.id.medium -> Statistics.INSTANCE.trackFilterClick(
                Statistics.EventParam.HOTEL,
                Pair(
                    Statistics.EventParam.PRICE_CATEGORY,
                    MEDIUM.toString()
                )
            )
            R.id.high -> Statistics.INSTANCE.trackFilterClick(
                Statistics.EventParam.HOTEL,
                Pair(
                    Statistics.EventParam.PRICE_CATEGORY,
                    HIGH.toString()
                )
            )
        }
        select(v.id, false)
    }

    fun updateFilter() {
        val filters: MutableList<PriceRateFilter> =
            ArrayList()
        for (i in 0 until mItems.size()) {
            val key = mItems.keyAt(i)
            val item = mItems.valueAt(i)
            if (item.mSelected) {
                @PriceDef var value = LOW
                when (key) {
                    R.id.low -> value = LOW
                    R.id.medium -> value = MEDIUM
                    R.id.high -> value = HIGH
                }
                filters.add(
                    PriceRateFilter(
                        HotelsFilter.Op.OP_EQ,
                        value
                    )
                )
            }
        }
        if (filters.size > 3) throw AssertionError("Wrong filters count")
        filter = null
        for (filter in filters) {
            if (this.filter == null) this.filter = filter else this.filter =
                HotelsFilter.Or(this.filter!!, filter)
        }
    }

    companion object {
        const val UNDEFINED = -1
        const val LOW = 1
        const val MEDIUM = 2
        const val HIGH = 3
    }
}