package com.mapswithme.maps.search

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.activity.CustomNavigateUpListener
import com.mapswithme.maps.base.BaseMwmToolbarFragment
import com.mapswithme.maps.metrics.UserActionsLogger.logBookingFilterUsedEvent
import com.mapswithme.maps.search.HotelsFilter.HotelType
import com.mapswithme.maps.search.HotelsFilter.OneOf
import com.mapswithme.maps.search.HotelsTypeAdapter.OnTypeSelectedListener
import com.mapswithme.maps.widget.ToolbarController
import com.mapswithme.maps.widget.recycler.TagItemDecoration
import com.mapswithme.maps.widget.recycler.TagLayoutManager
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.DateUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics
import java.util.*
import java.util.concurrent.TimeUnit

class FilterFragment : BaseMwmToolbarFragment(), OnTypeSelectedListener {
    private val mDateFormatter =
        DateUtils.getMediumDateFormatter()
    private var mNavigateUpListener: CustomNavigateUpListener? = null
    private var mListener: Listener? = null
    private lateinit var mRating: RatingFilterView
    private lateinit var mPrice: PriceFilterView
    private lateinit var mCheckIn: TextView
    private lateinit var mCheckOut: TextView
    private lateinit var mCheckInTitle: TextView
    private lateinit var mCheckOutTitle: TextView
    private lateinit var mOfflineWarning: TextView
    private val mTagsDecorator =
        ContextCompat.getDrawable(MwmApplication.get(), R.drawable.divider_transparent_half)!!
    private val mHotelTypes: MutableSet<HotelType> =
        HashSet()
    private var mTypeAdapter: HotelsTypeAdapter? = null
    private var mCheckinDate = Calendar.getInstance()
    private var mCheckoutDate =
        getDayAfter(mCheckinDate)
    private val mCheckinListener =
        OnDateSetListener { view: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
            val chosenDate = Calendar.getInstance()
            chosenDate[year, monthOfYear] = dayOfMonth
            mCheckinDate = chosenDate
            if (mCheckinDate.after(mCheckoutDate)) {
                mCheckoutDate = getDayAfter(mCheckinDate)
                mCheckOut.text = mDateFormatter.format(mCheckoutDate.time)
            } else {
                val difference =
                    mCheckoutDate.timeInMillis - mCheckinDate.timeInMillis
                val days = TimeUnit.MILLISECONDS.toDays(difference).toInt()
                if (days > MAX_STAYING_DAYS) {
                    mCheckoutDate = getMaxDateForCheckout(mCheckinDate)
                    mCheckOut.text = mDateFormatter.format(mCheckoutDate.time)
                }
            }
            mCheckIn.text = mDateFormatter.format(chosenDate.time)
        }
    private val mCheckoutListener =
        OnDateSetListener { view: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
            val chosenDate = Calendar.getInstance()
            chosenDate[year, monthOfYear] = dayOfMonth
            mCheckoutDate = chosenDate
            mCheckOut.text = mDateFormatter.format(mCheckoutDate.time)
        }
    private val mNetworkStateReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                enableDateViewsIfConnected()
            }
        }
    private lateinit var mFilterParamsFactory: BookingFilterParams.Factory
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CustomNavigateUpListener) mNavigateUpListener =
            context
        if (context is Listener) mListener =
            context
        mFilterParamsFactory = BookingFilterParams.Factory()
    }

    override fun onDetach() {
        super.onDetach()
        mNavigateUpListener = null
        mListener = null
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        activity!!.registerReceiver(mNetworkStateReceiver, filter)
    }

    override fun onStop() {
        activity!!.unregisterReceiver(mNetworkStateReceiver)
        super.onStop()
    }

    override fun onCreateToolbarController(root: View): ToolbarController {
        return object : ToolbarController(root, activity!!) {
            override fun onUpClick() {
                if (mNavigateUpListener == null) return
                mNavigateUpListener!!.customOnNavigateUp()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_search_filter, container, false)
        initDateViews(root)
        mRating = root.findViewById(R.id.rating)
        // Explicit casting is needed in this case, otherwise the crash is obtained in runtime,
// seems like a bug in current compiler (Java 8)
        mPrice = root.findViewById(R.id.price)
        val content = root.findViewById<View>(R.id.content)
        val type: RecyclerView = content.findViewById(R.id.type)
        type.layoutManager = TagLayoutManager()
        type.isNestedScrollingEnabled = false
        type.addItemDecoration(TagItemDecoration(mTagsDecorator))
        mTypeAdapter = HotelsTypeAdapter(this)
        type.adapter = mTypeAdapter
        root.findViewById<View>(R.id.done)
            .setOnClickListener { v: View? -> onFilterClicked() }
        val args = arguments
        var filter: HotelsFilter? = null
        var params: BookingFilterParams? = null
        if (args != null) {
            filter = args.getParcelable(ARG_FILTER)
            params = args.getParcelable(ARG_FILTER_PARAMS)
        }
        updateViews(filter, params)
        return root
    }

    private fun onFilterClicked() {
        if (mListener == null) return
        val filter = populateFilter()
        val params = mFilterParamsFactory.createParams(
            mCheckinDate.timeInMillis,
            mCheckoutDate.timeInMillis,
            BookingFilterParams.Room.DEFAULT
        )
        mListener!!.onFilterApply(filter, params)
        Statistics.INSTANCE.trackFilterEvent(
            Statistics.EventName.SEARCH_FILTER_APPLY,
            Statistics.EventParam.HOTEL
        )
        logBookingFilterUsedEvent()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Statistics.INSTANCE.trackFilterEvent(
            Statistics.EventName.SEARCH_FILTER_OPEN,
            Statistics.EventParam.HOTEL
        )
    }

    private fun initDateViews(root: View) {
        mCheckIn = root.findViewById(R.id.checkIn)
        mCheckIn.setOnClickListener { v: View? -> onCheckInClicked() }
        mCheckInTitle = root.findViewById(R.id.checkin_title)
        mCheckOut = root.findViewById(R.id.checkOut)
        mCheckOut.setOnClickListener { v: View? -> onCheckOutClicked() }
        mCheckOutTitle = root.findViewById(R.id.checkout_title)
        mOfflineWarning = root.findViewById(R.id.offlineWarning)
        enableDateViewsIfConnected()
    }

    private fun onCheckOutClicked() {
        val dialog = DatePickerDialog(
            activity!!, mCheckoutListener,
            mCheckoutDate[Calendar.YEAR],
            mCheckoutDate[Calendar.MONTH],
            mCheckoutDate[Calendar.DAY_OF_MONTH]
        )
        dialog.datePicker.minDate = getDayAfter(mCheckinDate).timeInMillis
        dialog.datePicker.maxDate = getMaxDateForCheckout(mCheckinDate).timeInMillis
        dialog.show()
        Statistics.INSTANCE.trackFilterClick(
            Statistics.EventParam.HOTEL,
            Pair(
                Statistics.EventParam.DATE,
                Statistics.ParamValue.CHECKOUT
            )
        )
    }

    private fun onCheckInClicked() {
        val dialog = DatePickerDialog(
            activity!!, mCheckinListener, mCheckinDate[Calendar.YEAR],
            mCheckinDate[Calendar.MONTH],
            mCheckinDate[Calendar.DAY_OF_MONTH]
        )
        dialog.datePicker.minDate = minDateForCheckin.timeInMillis
        dialog.datePicker.maxDate = maxDateForCheckin.timeInMillis
        dialog.show()
        Statistics.INSTANCE.trackFilterClick(
            Statistics.EventParam.HOTEL,
            Pair(
                Statistics.EventParam.DATE,
                Statistics.ParamValue.CHECKIN
            )
        )
    }

    private fun enableDateViewsIfConnected() {
        val connected = ConnectionState.isConnected
        UiUtils.showIf(!connected, mOfflineWarning)
        mCheckIn.isEnabled = connected
        mCheckOut.isEnabled = connected
        mCheckInTitle.isEnabled = connected
        mCheckOutTitle.isEnabled = connected
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        toolbarController.setTitle(R.string.booking_filters)
        toolbarController.toolbar.findViewById<View>(R.id.reset)
            .setOnClickListener { v: View? ->
                Statistics.INSTANCE.trackFilterEvent(
                    Statistics.EventName.SEARCH_FILTER_RESET,
                    Statistics.EventParam.HOTEL
                )
                updateViews(null, null)
            }
    }

    private fun populateFilter(): HotelsFilter? {
        mPrice.updateFilter()
        val rating = mRating.filter
        val price = mPrice.filter
        val oneOf = FilterUtils.makeOneOf(mHotelTypes.iterator())
        return FilterUtils.combineFilters(rating!!, price!!, oneOf!!)
    }

    private fun updateViews(filter: HotelsFilter?, params: BookingFilterParams?) {
        if (filter == null) {
            mRating.update(null)
            mPrice.update(null)
            if (mTypeAdapter != null) updateTypeAdapter(mTypeAdapter!!, null)
        } else {
            mRating.update(FilterUtils.findRatingFilter(filter))
            mPrice.update(FilterUtils.findPriceFilter(filter))
            if (mTypeAdapter != null) updateTypeAdapter(
                mTypeAdapter!!,
                FilterUtils.findTypeFilter(filter)
            )
        }
        updateDateViews(params)
    }

    private fun updateDateViews(params: BookingFilterParams?) {
        if (params == null) {
            mCheckinDate = Calendar.getInstance()
            mCheckIn.text = mDateFormatter.format(mCheckinDate.time)
            mCheckoutDate = getDayAfter(mCheckinDate)
            mCheckOut.text = mDateFormatter.format(mCheckoutDate.time)
        } else {
            val checkin = Calendar.getInstance()
            checkin.timeInMillis = params.mCheckinMillisec
            mCheckinDate = checkin
            mCheckIn.text = mDateFormatter.format(mCheckinDate.time)
            val checkout = Calendar.getInstance()
            checkout.timeInMillis = params.mCheckoutMillisec
            mCheckoutDate = checkout
            mCheckOut.text = mDateFormatter.format(mCheckoutDate.time)
        }
    }

    private fun updateTypeAdapter(
        typeAdapter: HotelsTypeAdapter,
        types: OneOf?
    ) {
        mHotelTypes.clear()
        types?.let { populateHotelTypes(mHotelTypes, it) }
        typeAdapter.updateItems(mHotelTypes)
    }

    private fun populateHotelTypes(
        hotelTypes: MutableSet<HotelType>,
        types: OneOf
    ) {
        hotelTypes.add(types.mType)
        if (types.mTile != null) populateHotelTypes(hotelTypes, types.mTile)
    }

    override fun onTypeSelected(selected: Boolean, type: HotelType) {
        if (selected) {
            Statistics.INSTANCE.trackFilterClick(
                Statistics.EventParam.HOTEL,
                Pair(
                    Statistics.EventParam.TYPE,
                    type.tag
                )
            )
            mHotelTypes.add(type)
        } else {
            mHotelTypes.remove(type)
        }
    }

    internal interface Listener {
        fun onFilterApply(filter: HotelsFilter?, params: BookingFilterParams?)
    }

    companion object {
        const val ARG_FILTER = "arg_filter"
        const val ARG_FILTER_PARAMS = "arg_filter_params"
        private const val MAX_STAYING_DAYS = 30
        private const val MAX_CHECKIN_WINDOW_IN_DAYS = 360
        // This little subtraction is needed to avoid the crash on old androids (e.g. 4.4).
        private val minDateForCheckin: Calendar
            private get() {
                val date = Calendar.getInstance()
                // This little subtraction is needed to avoid the crash on old androids (e.g. 4.4).
                date.add(Calendar.SECOND, -1)
                return date
            }

        private val maxDateForCheckin: Calendar
            private get() {
                val date = Calendar.getInstance()
                date.add(
                    Calendar.DAY_OF_YEAR,
                    MAX_CHECKIN_WINDOW_IN_DAYS
                )
                return date
            }

        private fun getMaxDateForCheckout(checkin: Calendar): Calendar {
            val difference =
                checkin.timeInMillis - System.currentTimeMillis()
            val daysToCheckin =
                TimeUnit.MILLISECONDS.toDays(difference).toInt()
            val leftDays = MAX_CHECKIN_WINDOW_IN_DAYS - daysToCheckin
            val date = Calendar.getInstance()
            date.time = checkin.time
            date.add(
                Calendar.DAY_OF_YEAR,
                if (leftDays >= MAX_STAYING_DAYS) MAX_STAYING_DAYS else leftDays
            )
            return date
        }

        private fun getDayAfter(date: Calendar): Calendar {
            val dayAfter = Calendar.getInstance()
            dayAfter.time = date.time
            dayAfter.add(Calendar.DAY_OF_YEAR, 1)
            return dayAfter
        }
    }
}