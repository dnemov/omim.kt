package com.mapswithme.maps.editor

import android.annotation.SuppressLint
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.editor.HoursMinutesPickerFragment.OnPickListener
import com.mapswithme.maps.editor.SimpleTimetableAdapter.BaseTimetableViewHolder
import com.mapswithme.maps.editor.data.HoursMinutes
import com.mapswithme.maps.editor.data.TimeFormatUtils
import com.mapswithme.maps.editor.data.Timespan
import com.mapswithme.maps.editor.data.Timetable
import com.mapswithme.util.UiUtils
import java.util.*

class SimpleTimetableAdapter(private val mFragment: Fragment) :
    RecyclerView.Adapter<BaseTimetableViewHolder>(), OnPickListener, TimetableProvider {
    private var mItems: MutableList<Timetable> = ArrayList()
    private var mComplementItem: Timetable? = null
    private var mPickingPosition = 0


    override var timetables: String?
        get() = OpeningHours.nativeTimetablesToString(mItems.toTypedArray())
        set(timetables) {
            if (timetables == null) return
            val items = OpeningHours.nativeTimetablesFromString(timetables) ?: return
            mItems = ArrayList(listOf(*items))
            refreshComplement()
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseTimetableViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_TIMETABLE) TimetableViewHolder(
            inflater.inflate(R.layout.item_timetable, parent, false)
        ) else AddTimetableViewHolder(inflater.inflate(R.layout.item_timetable_add, parent, false))
    }

    override fun onBindViewHolder(
        holder: BaseTimetableViewHolder,
        position: Int
    ) {
        holder.onBind()
    }

    override fun getItemCount(): Int {
        return mItems.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) TYPE_ADD_TIMETABLE else TYPE_TIMETABLE
    }

    private fun addTimetable() {
        mItems.add(OpeningHours.nativeGetComplementTimetable(mItems.toTypedArray()))
        notifyItemInserted(mItems.size - 1)
        refreshComplement()
    }

    private fun removeTimetable(position: Int) {
        mItems.removeAt(position)
        notifyItemRemoved(position)
        refreshComplement()
    }

    private fun refreshComplement() {
        mComplementItem = OpeningHours.nativeGetComplementTimetable(mItems.toTypedArray())
        notifyItemChanged(itemCount - 1)
    }

    @SuppressLint("Range")
    private fun pickTime(
        position: Int, @IntRange(
            from = HoursMinutesPickerFragment.Companion.TAB_FROM.toLong(),
            to = HoursMinutesPickerFragment.Companion.TAB_TO.toLong()
        ) tab: Int,
        @IntRange(
            from = ID_OPENING.toLong(),
            to = ID_CLOSING.toLong()
        ) id: Int
    ) {
        val data = mItems[position]
        mPickingPosition = position
        HoursMinutesPickerFragment.pick(
            mFragment.activity, mFragment.childFragmentManager,
            data.workingTimespan.start!!, data.workingTimespan.end!!,
            tab, id
        )
    }

    override fun onHoursMinutesPicked(
        from: HoursMinutes?,
        to: HoursMinutes?,
        id: Int
    ) {
        val item = mItems[mPickingPosition]
        if (id == ID_OPENING) mItems[mPickingPosition] = OpeningHours.nativeSetOpeningTime(
            item,
            Timespan(from, to)
        ) else mItems[mPickingPosition] = OpeningHours.nativeAddClosedSpan(item, Timespan(from, to))
        notifyItemChanged(mPickingPosition)
    }

    private fun removeClosedHours(position: Int, closedPosition: Int) {
        mItems[position] = OpeningHours.nativeRemoveClosedSpan(mItems[position], closedPosition)
        notifyItemChanged(position)
    }

    private fun addWorkingDay(day: Int, position: Int) {
        val tts: Array<Timetable> = mItems.toTypedArray()
        mItems = ArrayList(
            Arrays.asList(
                *OpeningHours.nativeAddWorkingDay(
                    tts,
                    position,
                    day
                )
            )
        )
        refreshComplement()
        notifyDataSetChanged()
    }

    private fun removeWorkingDay(day: Int, position: Int) {
        val tts: Array<Timetable> = mItems.toTypedArray()
        mItems = ArrayList(
            Arrays.asList(
                *OpeningHours.nativeRemoveWorkingDay(
                    tts,
                    position,
                    day
                )
            )
        )
        refreshComplement()
        notifyDataSetChanged()
    }

    private fun setFullday(position: Int, fullday: Boolean) {
        mItems[position] = OpeningHours.nativeSetIsFullday(mItems[position], fullday)
        notifyItemChanged(position)
    }

    public abstract class BaseTimetableViewHolder(itemView: View?) :
        RecyclerView.ViewHolder(itemView!!) {
        abstract fun onBind()
    }

    private inner class TimetableViewHolder internal constructor(itemView: View) :
        BaseTimetableViewHolder(itemView), View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
        var days = SparseArray<CheckBox>(7)
        var allday: View
        var swAllday: SwitchCompat
        var schedule: View
        var openClose: View
        var open: View
        var close: View
        var tvOpen: TextView
        var tvClose: TextView
        var closedHours =
            arrayOfNulls<View>(Companion.MAX_CLOSED_SPANS)
        var addClosed: View
        var deleteTimetable: View
        private fun initDays() {
            val firstDay = Calendar.getInstance().firstDayOfWeek
            var day = 0
            for (i in firstDay..7) addDay(i, DAYS[day++])
            for (i in 1 until firstDay) addDay(i, DAYS[day++])
        }

        override fun onBind() {
            val position = adapterPosition
            val data = mItems[position]
            UiUtils.showIf(position > 0, deleteTimetable)
            tvOpen.text = data.workingTimespan.start.toString()
            tvClose.text = data.workingTimespan.end.toString()
            showDays(data.weekdays)
            showSchedule(!data.isFullday)
            showClosedHours(data.closedTimespans)
        }

        override fun onClick(v: View) {
            when (v.id) {
                R.id.time_open -> pickTime(
                    adapterPosition,
                    HoursMinutesPickerFragment.Companion.TAB_FROM,
                    ID_OPENING
                )
                R.id.time_close -> pickTime(
                    adapterPosition,
                    HoursMinutesPickerFragment.Companion.TAB_TO,
                    ID_OPENING
                )
                R.id.tv__remove_timetable -> removeTimetable(adapterPosition)
                R.id.tv__add_closed -> pickTime(
                    adapterPosition,
                    HoursMinutesPickerFragment.Companion.TAB_FROM,
                    ID_CLOSING
                )
                R.id.allday -> swAllday.toggle()
            }
        }

        override fun onCheckedChanged(
            buttonView: CompoundButton,
            isChecked: Boolean
        ) {
            when (buttonView.id) {
                R.id.sw__allday -> setFullday(adapterPosition, isChecked)
                R.id.chb__day -> {
                    val dayIndex = buttonView.tag as Int
                    switchWorkingDay(dayIndex)
                }
            }
        }

        fun showDays(
            @IntRange(
                from = 1,
                to = 7
            ) weekdays: IntArray?
        ) {
            for (i in 1..7) checkWithoutCallback(days[i], false)
            for (checked in weekdays!!) checkWithoutCallback(days[checked], true)
        }

        fun showSchedule(show: Boolean) {
            UiUtils.showIf(show, schedule)
            checkWithoutCallback(swAllday, !show)
        }

        private fun showClosedHours(closedSpans: Array<Timespan>?) {
            var i = 0
            for (timespan in closedSpans!!) {
                if (i == Companion.MAX_CLOSED_SPANS) return
                if (timespan == null) UiUtils.hide(closedHours[i]!!) else {
                    UiUtils.show(closedHours[i])
                    (closedHours[i]!!.findViewById<View>(R.id.tv__closed) as TextView).text =
                        timespan.toString()
                }
                i++
            }
            while (i < Companion.MAX_CLOSED_SPANS) UiUtils.hide(
                closedHours[i++]!!
            )
        }

        /**
         * @param dayIndex 1 based index of a day in the week
         * @param id       resource id of day view
         */
        private fun addDay(
            @IntRange(
                from = 1,
                to = 7
            ) dayIndex: Int, @IdRes id: Int
        ) {
            val day = itemView.findViewById<View>(id)
            val checkBox = day.findViewById<View>(R.id.chb__day) as CheckBox
            // Save index of the day to get it back when checkbox will be toggled.
            checkBox.tag = dayIndex
            days.put(dayIndex, checkBox)
            day.setOnClickListener { checkBox.toggle() }
            (day.findViewById<View>(R.id.tv__day) as TextView).text =
                TimeFormatUtils.formatShortWeekday(
                    dayIndex
                )
        }

        private fun switchWorkingDay(
            @IntRange(
                from = 1,
                to = 7
            ) dayIndex: Int
        ) {
            val checkBox = days[dayIndex]
            if (checkBox.isChecked) addWorkingDay(
                dayIndex,
                adapterPosition
            ) else removeWorkingDay(dayIndex, adapterPosition)
        }

        private fun checkWithoutCallback(
            button: CompoundButton,
            check: Boolean
        ) {
            button.setOnCheckedChangeListener(null)
            button.isChecked = check
            button.setOnCheckedChangeListener(this)
        }

        init {
            initDays()
            allday = itemView.findViewById(R.id.allday)
            allday.setOnClickListener(this)
            swAllday = allday.findViewById<View>(R.id.sw__allday) as SwitchCompat
            schedule = itemView.findViewById(R.id.schedule)
            openClose = schedule.findViewById(R.id.time_open_close)
            open = openClose.findViewById(R.id.time_open)
            open.setOnClickListener(this)
            close = openClose.findViewById(R.id.time_close)
            close.setOnClickListener(this)
            tvOpen = open.findViewById<View>(R.id.tv__time_open) as TextView
            tvClose = close.findViewById<View>(R.id.tv__time_close) as TextView
            addClosed = schedule.findViewById(R.id.tv__add_closed)
            addClosed.setOnClickListener(this)
            deleteTimetable = itemView.findViewById(R.id.tv__remove_timetable)
            deleteTimetable.setOnClickListener(this)
            val closedHost =
                itemView.findViewById<View>(R.id.closed_host) as ViewGroup
            for (i in 0 until Companion.MAX_CLOSED_SPANS) {
                val span = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_timetable_closed_hours, closedHost, false)
                closedHost.addView(
                    span,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        UiUtils.dimen(R.dimen.editor_height_closed)
                    )
                )
                closedHours[i] = span
                span.findViewById<View>(R.id.iv__remove_closed)
                    .setOnClickListener { removeClosedHours(adapterPosition, i) }
            }
        }
    }

    private inner class AddTimetableViewHolder internal constructor(itemView: View) :
        BaseTimetableViewHolder(itemView) {
        private val mAdd: Button
        override fun onBind() {
            val enable =
                mComplementItem != null && mComplementItem!!.weekdays.size != 0
            val text = mFragment.getString(R.string.editor_time_add)
            mAdd.isEnabled = enable
            mAdd.text =
                if (enable) text + " (" + TimeFormatUtils.formatWeekdays(mComplementItem!!) + ")" else text
        }

        init {
            mAdd =
                itemView.findViewById<View>(R.id.btn__add_time) as Button
            mAdd.setOnClickListener { addTimetable() }
        }
    }

    companion object {
        const val MAX_CLOSED_SPANS = 10
        private const val TYPE_TIMETABLE = 0
        private const val TYPE_ADD_TIMETABLE = 1
        private const val ID_OPENING = 0
        private const val ID_CLOSING = 1
        private val DAYS =
            intArrayOf(R.id.day1, R.id.day2, R.id.day3, R.id.day4, R.id.day5, R.id.day6, R.id.day7)
    }

    init {
        mItems =
            ArrayList(Arrays.asList(*OpeningHours.nativeGetDefaultTimetables()))
        refreshComplement()
    }
}