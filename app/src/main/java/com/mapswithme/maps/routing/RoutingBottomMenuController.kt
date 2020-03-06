package com.mapswithme.maps.routing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.mapswithme.maps.Framework
import com.mapswithme.maps.Framework.RouteAltitudeLimits
import com.mapswithme.maps.R
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.routing.RoutePointInfo.RouteMarkType
import com.mapswithme.maps.taxi.TaxiAdapter
import com.mapswithme.maps.taxi.TaxiInfo
import com.mapswithme.maps.taxi.TaxiManager
import com.mapswithme.maps.widget.DotPager
import com.mapswithme.maps.widget.recycler.DotDividerItemDecoration
import com.mapswithme.maps.widget.recycler.MultilineLayoutManager
import com.mapswithme.util.Graphics
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import java.util.*

internal class RoutingBottomMenuController private constructor(
    private val mContext: Activity,
    private val mAltitudeChartFrame: View,
    private val mTransitFrame: View,
    private val mTaxiFrame: View,
    private val mError: TextView,
    private val mStart: Button,
    private val mAltitudeChart: ImageView,
    private val mAltitudeDifference: TextView,
    private val mNumbersFrame: View,
    private val mActionFrame: View,
    listener: RoutingBottomMenuListener?
) : View.OnClickListener {
    private val mActionMessage: TextView
    private val mActionButton: View
    private val mActionIcon: ImageView
    private val mTransitViewDecorator: DotDividerItemDecoration
    private var mTaxiInfo: TaxiInfo? = null
    private var mTaxiProduct: TaxiInfo.Product? = null
    private val mListener: RoutingBottomMenuListener?
    fun showAltitudeChartAndRoutingDetails() {
        UiUtils.hide(mError, mTaxiFrame, mActionFrame, mTransitFrame)
        showRouteAltitudeChart()
        showRoutingDetails()
        UiUtils.show(mAltitudeChartFrame)
    }

    fun hideAltitudeChartAndRoutingDetails() {
        UiUtils.hide(mAltitudeChartFrame, mTransitFrame)
    }

    fun showTaxiInfo(info: TaxiInfo) {
        UiUtils.hide(mError, mAltitudeChartFrame, mActionFrame, mTransitFrame)
        val logo =
            mTaxiFrame.findViewById<ImageView>(R.id.iv__logo)
        logo.setImageResource(info.type.icon)
        val products =
            info.products
        mTaxiInfo = info
        mTaxiProduct = products[0]
        val adapter: PagerAdapter = TaxiAdapter(mContext, mTaxiInfo!!.type, products)
        val pager = DotPager.Builder(
            mContext, (mTaxiFrame.findViewById<View>(R.id.pager) as ViewPager),
            adapter
        )
            .setIndicatorContainer((mTaxiFrame.findViewById<View>(R.id.indicator) as ViewGroup))
            .setPageChangedListener(object : DotPager.OnPageChangedListener{
                override fun onPageChanged(position: Int) {
                    mTaxiProduct = products[position]
                }

            }).build()
        pager.show()
        setStartButton()
        UiUtils.show(mTaxiFrame)
    }

    @SuppressLint("SetTextI18n")
    fun showTransitInfo(info: TransitRouteInfo) {
        UiUtils.hide(mError, mAltitudeChartFrame, mActionFrame, mAltitudeChartFrame, mTaxiFrame)
        showStartButton(false)
        UiUtils.show(mTransitFrame)
        val rv =
            mTransitFrame.findViewById<View>(R.id.transit_recycler_view) as RecyclerView
        val adapter = TransitStepAdapter()
        rv.layoutManager = MultilineLayoutManager()
        rv.isNestedScrollingEnabled = false
        rv.removeItemDecoration(mTransitViewDecorator)
        rv.addItemDecoration(mTransitViewDecorator)
        rv.adapter = adapter
        adapter.setItems(info.transitSteps)
        val totalTimeView =
            mTransitFrame.findViewById<View>(R.id.total_time) as TextView
        totalTimeView.text = RoutingController.formatRoutingTime(
            mContext, info.totalTime,
            R.dimen.text_size_routing_number
        )
        val dotView = mTransitFrame.findViewById<View>(R.id.dot)
        val pedestrianIcon =
            mTransitFrame.findViewById<View>(R.id.pedestrian_icon)
        val distanceView =
            mTransitFrame.findViewById<View>(R.id.total_distance) as TextView
        UiUtils.showIf(
            info.totalPedestrianTimeInSec > 0,
            dotView,
            pedestrianIcon,
            distanceView
        )
        distanceView.text = info.totalPedestrianDistance + " " + info.totalPedestrianDistanceUnits
    }

    fun showAddStartFrame() {
        UiUtils.hide(mTaxiFrame, mError, mTransitFrame)
        UiUtils.show(mActionFrame)
        mActionMessage.setText(R.string.routing_add_start_point)
        mActionMessage.tag = RoutePointInfo.ROUTE_MARK_START
        if (LocationHelper.INSTANCE.myPosition != null) {
            UiUtils.show(mActionButton)
            val icon =
                ContextCompat.getDrawable(mContext, R.drawable.ic_my_location)
            val colorAccent = ContextCompat.getColor(
                mContext,
                UiUtils.getStyledResourceId(mContext, R.attr.colorAccent)
            )
            mActionIcon.setImageDrawable(Graphics.tint(icon, colorAccent))
        } else {
            UiUtils.hide(mActionButton)
        }
    }

    fun showAddFinishFrame() {
        UiUtils.hide(mTaxiFrame, mError, mTransitFrame)
        UiUtils.show(mActionFrame)
        mActionMessage.setText(R.string.routing_add_finish_point)
        mActionMessage.tag = RoutePointInfo.ROUTE_MARK_FINISH
        UiUtils.hide(mActionButton)
    }

    fun hideActionFrame() {
        UiUtils.hide(mActionFrame)
    }

    fun setStartButton() {
        if (RoutingController.get().isTaxiRouterType && mTaxiInfo != null) {
            mStart.setText(
                if (Utils.isAppInstalled(
                        mContext,
                        mTaxiInfo!!.type.packageName
                    )
                ) R.string.taxi_order else R.string.install_app
            )
            mStart.setOnClickListener { v: View? -> handleTaxiClick() }
        } else {
            mStart.text = mContext.getText(R.string.p2p_start)
            mStart.setOnClickListener { v: View? -> mListener?.onRoutingStart() }
        }
        showStartButton(true)
    }

    private fun handleTaxiClick() {
        if (mTaxiProduct == null || mTaxiInfo == null) return
        val startPoint = RoutingController.get().startPoint
        val endPoint = RoutingController.get().endPoint
        val links = TaxiManager.getTaxiLink(
            mTaxiProduct!!.productId, mTaxiInfo!!.type,
            startPoint, endPoint
        )
        if (links != null) TaxiManager.launchTaxiApp(mContext, links, mTaxiInfo!!.type)
    }

    fun showError(@StringRes message: Int) {
        showError(mError.context.getString(message))
    }

    private fun showError(message: String) {
        UiUtils.hide(mTaxiFrame, mAltitudeChartFrame, mActionFrame, mTransitFrame)
        mError.text = message
        mError.visibility = View.VISIBLE
        showStartButton(false)
    }

    fun showStartButton(show: Boolean) {
        val result = show && (RoutingController.get().isBuilt
                || RoutingController.get().isTaxiRouterType)
        UiUtils.showIf(result, mStart)
    }

    fun saveRoutingPanelState(outState: Bundle) {
        outState.putBoolean(
            STATE_ALTITUDE_CHART_SHOWN,
            UiUtils.isVisible(mAltitudeChartFrame)
        )
        outState.putParcelable(STATE_TAXI_INFO, mTaxiInfo)
        if (UiUtils.isVisible(mError)) outState.putString(
            STATE_ERROR,
            mError.text.toString()
        )
    }

    fun restoreRoutingPanelState(state: Bundle) {
        if (state.getBoolean(STATE_ALTITUDE_CHART_SHOWN)) showAltitudeChartAndRoutingDetails()
        val info: TaxiInfo? =
            state.getParcelable(STATE_TAXI_INFO)
        info?.let { showTaxiInfo(it) }
        val error =
            state.getString(STATE_ERROR)
        if (!TextUtils.isEmpty(error)) showError(error!!)
    }

    private fun showRouteAltitudeChart() {
        if (RoutingController.get().isVehicleRouterType) {
            UiUtils.hide(mAltitudeChart, mAltitudeDifference)
            return
        }
        val chartWidth = UiUtils.dimen(mContext, R.dimen.altitude_chart_image_width)
        val chartHeight = UiUtils.dimen(mContext, R.dimen.altitude_chart_image_height)
        val limits = RouteAltitudeLimits()
        val bm = Framework.generateRouteAltitudeChart(chartWidth, chartHeight, limits)
        if (bm != null) {
            mAltitudeChart.setImageBitmap(bm)
            UiUtils.show(mAltitudeChart)
            val meter = mAltitudeDifference.resources.getString(R.string.meter)
            val foot = mAltitudeDifference.resources.getString(R.string.foot)
            mAltitudeDifference.text = String.format(
                Locale.getDefault(), "%d %s",
                limits.maxRouteAltitude - limits.minRouteAltitude,
                if (limits.isMetricUnits) meter else foot
            )
            val icon = ContextCompat.getDrawable(
                mContext,
                R.drawable.ic_altitude_difference
            )
            val colorAccent = ContextCompat.getColor(
                mContext,
                UiUtils.getStyledResourceId(mContext, R.attr.colorAccent)
            )
            mAltitudeDifference.setCompoundDrawablesWithIntrinsicBounds(
                Graphics.tint(icon, colorAccent),
                null, null, null
            )
            UiUtils.show(mAltitudeDifference)
        }
    }

    private fun showRoutingDetails() {
        val rinfo = RoutingController.get().cachedRoutingInfo
        if (rinfo == null) {
            UiUtils.hide(mNumbersFrame)
            return
        }
        val spanned =
            makeSpannedRoutingDetails(mContext, rinfo)
        val numbersTime =
            mNumbersFrame.findViewById<View>(R.id.time) as TextView
        numbersTime.text = spanned
        val numbersArrival =
            mNumbersFrame.findViewById<View>(R.id.arrival) as TextView?
        if (numbersArrival != null) {
            val arrivalTime =
                RoutingController.formatArrivalTime(rinfo.totalTimeInSeconds)
            numbersArrival.text = arrivalTime
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn__my_position_use -> mListener?.onUseMyPositionAsStart()
            R.id.btn__search_point -> if (mListener != null) {
                @RouteMarkType val pointType = mActionMessage.tag as Int
                mListener.onSearchRoutePoint(pointType)
            }
        }
    }

    companion object {
        private const val STATE_ALTITUDE_CHART_SHOWN = "altitude_chart_shown"
        private const val STATE_TAXI_INFO = "taxi_info"
        private const val STATE_ERROR = "error"
        @kotlin.jvm.JvmStatic
        fun newInstance(
            activity: Activity, frame: View,
            listener: RoutingBottomMenuListener?
        ): RoutingBottomMenuController {
            val altitudeChartFrame =
                getViewById(
                    activity,
                    frame,
                    R.id.altitude_chart_panel
                )
            val transitFrame = getViewById(
                activity,
                frame,
                R.id.transit_panel
            )
            val taxiFrame =
                getViewById(activity, frame, R.id.taxi_panel)
            val error = getViewById(
                activity,
                frame,
                R.id.error
            ) as TextView
            val start = getViewById(
                activity,
                frame,
                R.id.start
            ) as Button
            val altitudeChart =
                getViewById(
                    activity,
                    frame,
                    R.id.altitude_chart
                ) as ImageView
            val altitudeDifference = getViewById(
                activity,
                frame,
                R.id.altitude_difference
            ) as TextView
            val numbersFrame =
                getViewById(activity, frame, R.id.numbers)
            val actionFrame = getViewById(
                activity,
                frame,
                R.id.routing_action_frame
            )
            return RoutingBottomMenuController(
                activity, altitudeChartFrame, transitFrame, taxiFrame,
                error, start, altitudeChart, altitudeDifference,
                numbersFrame, actionFrame, listener
            )
        }

        private fun getViewById(
            activity: Activity, frame: View,
            @IdRes resourceId: Int
        ): View {
            val view = frame.findViewById<View>(resourceId)
            return view ?: activity.findViewById(resourceId)
        }

        private fun makeSpannedRoutingDetails(
            context: Context,
            routingInfo: RoutingInfo
        ): Spanned {
            val time = RoutingController.formatRoutingTime(
                context,
                routingInfo.totalTimeInSeconds,
                R.dimen.text_size_routing_number
            )
            val builder = SpannableStringBuilder()
            initTimeBuilderSequence(context, time, builder)
            val dot = " • "
            initDotBuilderSequence(context, dot, builder)
            val dist = routingInfo.distToTarget + " " + routingInfo.targetUnits
            initDistanceBuilderSequence(
                context,
                dist,
                builder
            )
            return builder
        }

        private fun initTimeBuilderSequence(
            context: Context, time: CharSequence,
            builder: SpannableStringBuilder
        ) {
            builder.append(time)
            builder.setSpan(
                TypefaceSpan(context.resources.getString(R.string.robotoMedium)),
                0,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                AbsoluteSizeSpan(
                    context.resources
                        .getDimensionPixelSize(R.dimen.text_size_routing_number)
                ),
                0,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                ForegroundColorSpan(
                    ThemeUtils.getColor(
                        context,
                        android.R.attr.textColorPrimary
                    )
                ),
                0,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        private fun initDotBuilderSequence(
            context: Context, dot: String,
            builder: SpannableStringBuilder
        ) {
            builder.append(dot)
            builder.setSpan(
                TypefaceSpan(context.resources.getString(R.string.robotoMedium)),
                builder.length - dot.length,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                AbsoluteSizeSpan(
                    context.resources
                        .getDimensionPixelSize(R.dimen.text_size_routing_number)
                ),
                builder.length - dot.length,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                ForegroundColorSpan(
                    ThemeUtils.getColor(
                        context,
                        R.attr.secondary
                    )
                ),
                builder.length - dot.length,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        private fun initDistanceBuilderSequence(
            context: Context, arrivalTime: String,
            builder: SpannableStringBuilder
        ) {
            builder.append(arrivalTime)
            builder.setSpan(
                TypefaceSpan(context.resources.getString(R.string.robotoMedium)),
                builder.length - arrivalTime.length,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                AbsoluteSizeSpan(
                    context.resources
                        .getDimensionPixelSize(R.dimen.text_size_routing_number)
                ),
                builder.length - arrivalTime.length,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                StyleSpan(Typeface.NORMAL),
                builder.length - arrivalTime.length,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                ForegroundColorSpan(
                    ThemeUtils.getColor(
                        context,
                        android.R.attr.textColorPrimary
                    )
                ),
                builder.length - arrivalTime.length,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    init {
        mActionMessage = mActionFrame.findViewById<View>(R.id.tv__message) as TextView
        mActionButton = mActionFrame.findViewById(R.id.btn__my_position_use)
        mActionButton.setOnClickListener(this)
        val actionSearchButton =
            mActionFrame.findViewById<View>(R.id.btn__search_point)
        actionSearchButton.setOnClickListener(this)
        mActionIcon =
            mActionButton.findViewById<View>(R.id.iv__icon) as ImageView
        UiUtils.hide(mAltitudeChartFrame, mTaxiFrame, mActionFrame)
        mListener = listener
        val dividerRes =
            ThemeUtils.getResource(mContext, R.attr.transitStepDivider)
        val dividerDrawable =
            ContextCompat.getDrawable(mContext, dividerRes)
        val res = mContext.resources
        mTransitViewDecorator = DotDividerItemDecoration(
            dividerDrawable!!, res.getDimensionPixelSize(R.dimen.margin_base),
            res.getDimensionPixelSize(R.dimen.margin_half)
        )
    }
}