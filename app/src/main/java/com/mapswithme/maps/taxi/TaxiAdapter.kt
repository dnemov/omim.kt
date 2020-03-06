package com.mapswithme.maps.taxi

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import com.mapswithme.maps.R
import com.mapswithme.maps.routing.RoutingController.Companion.formatRoutingTime
import com.mapswithme.util.UiUtils

class TaxiAdapter(
    private val mContext: Context, private val mType: TaxiType,
    private val mProducts: List<TaxiInfo.Product>
) : PagerAdapter() {
    override fun getCount(): Int {
        return mProducts.size
    }

    override fun isViewFromObject(
        view: View,
        `object`: Any
    ): Boolean {
        return view === `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val product = mProducts[position]
        val v =
            LayoutInflater.from(mContext).inflate(R.layout.taxi_pager_item, container, false)
        val name = v.findViewById<View>(R.id.product_name) as TextView
        val isApproxPrice = mType.isPriceApproximated
        name.text = if (isApproxPrice) mContext.getString(mType.title) else product.name
        val separator =
            UiUtils.PHRASE_SEPARATOR + if (isApproxPrice) UiUtils.APPROXIMATE_SYMBOL else ""
        val timeAndPriceView =
            v.findViewById<View>(R.id.arrival_time_price) as TextView
        val time = product.time.toInt()
        val waitTime = formatRoutingTime(
            mContext, time,
            R.dimen.text_size_body_3
        )
        val formattedPrice = mType.formatPriceStrategy.format(product)
        val timeAndPriceValue = waitTime.toString() + separator + formattedPrice
        val timeAndPrice =
            mContext.getString(mType.waitingTemplateResId, timeAndPriceValue)
        timeAndPriceView.text = timeAndPrice
        container.addView(v, 0)
        return v
    }

    override fun destroyItem(
        container: ViewGroup,
        position: Int,
        `object`: Any
    ) {
        container.removeView(`object` as View)
    }

}