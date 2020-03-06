package com.mapswithme.maps.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmRecyclerFragment
import com.mapswithme.maps.editor.HoursMinutesPickerFragment.OnPickListener
import com.mapswithme.maps.editor.data.HoursMinutes

class SimpleTimetableFragment : BaseMwmRecyclerFragment<SimpleTimetableAdapter?>(),
    TimetableProvider, OnPickListener {
    private var mAdapter: SimpleTimetableAdapter? = null
    private var mInitTimetables: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun createAdapter(): SimpleTimetableAdapter {
        mAdapter = SimpleTimetableAdapter(this)
        mAdapter?.timetables = mInitTimetables
        return mAdapter!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timetable_simple, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
    }

    override var timetables: String?
        get() = mAdapter?.timetables
        set(timetables) {
            mInitTimetables = timetables
        }

    override fun onHoursMinutesPicked(
        from: HoursMinutes?,
        to: HoursMinutes?,
        id: Int
    ) {
        mAdapter!!.onHoursMinutesPicked(from, to, id)
    }
}