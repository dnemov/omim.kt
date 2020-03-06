package com.mapswithme.maps.editor

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import androidx.annotation.IntRange
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.tabs.TabLayout
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.maps.editor.data.HoursMinutes
import com.mapswithme.util.ThemeUtils

class HoursMinutesPickerFragment : BaseMwmDialogFragment() {
    private var mFrom: HoursMinutes? = null
    private var mTo: HoursMinutes? = null
    private var mPicker: TimePicker? = null
    private var mPickerHoursLabel: View? = null
    @IntRange(from = 0, to = 1)
    private var mSelectedTab = 0
    private var mTabs: TabLayout? = null
    private var mId = 0
    private var mOkButton: Button? = null

    interface OnPickListener {
        fun onHoursMinutesPicked(from: HoursMinutes?, to: HoursMinutes?, id: Int)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        readArgs()
        val root = createView()
        mTabs!!.getTabAt(mSelectedTab)!!.select()
        val theme =
            if (ThemeUtils.isNightTheme) R.style.MwmMain_DialogFragment_TimePicker_Night else R.style.MwmMain_DialogFragment_TimePicker
        val dialog =
            AlertDialog.Builder(activity!!, theme)
                .setView(root)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true)
                .create()
        dialog.setOnShowListener {
            mOkButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            mOkButton?.setOnClickListener(View.OnClickListener {
                if (mSelectedTab == TAB_FROM) {
                    mTabs!!.getTabAt(TAB_TO)!!.select()
                    return@OnClickListener
                }
                saveHoursMinutes()
                dismiss()
                if (parentFragment is OnPickListener) (parentFragment as OnPickListener?)!!.onHoursMinutesPicked(
                    mFrom,
                    mTo,
                    mId
                )
            })
            refreshPicker()
        }
        return dialog
    }

    private fun readArgs() {
        val args = arguments
        mFrom = args!!.getParcelable(EXTRA_FROM)
        mTo = args.getParcelable(EXTRA_TO)
        mSelectedTab = args.getInt(EXTRA_SELECT_FIRST)
        mId = args.getInt(EXTRA_ID)
    }

    private fun createView(): View {
        val inflater = LayoutInflater.from(activity)
        @SuppressLint("InflateParams") val root =
            inflater.inflate(R.layout.fragment_timetable_picker, null)
        mPicker = root.findViewById<View>(R.id.picker) as TimePicker
        mPicker!!.setIs24HourView(DateFormat.is24HourFormat(activity))
        val id = resources.getIdentifier("hours", "id", "android")
        if (id != 0) {
            mPickerHoursLabel = mPicker!!.findViewById(id)
            if (mPickerHoursLabel !is TextView) mPickerHoursLabel = null
        }
        mTabs = root.findViewById<View>(R.id.tabs) as TabLayout
        var tabView = inflater.inflate(R.layout.tab_timepicker, mTabs, false) as TextView
        // TODO @yunik add translations
        tabView.text = "From"
        val textColor =
            resources.getColorStateList(if (ThemeUtils.isNightTheme) R.color.accent_color_selector_night else R.color.accent_color_selector)
        tabView.setTextColor(textColor)
        mTabs!!.addTab(mTabs!!.newTab().setCustomView(tabView), true)
        tabView = inflater.inflate(R.layout.tab_timepicker, mTabs, false) as TextView
        tabView.text = "To"
        tabView.setTextColor(textColor)
        mTabs!!.addTab(mTabs!!.newTab().setCustomView(tabView), true)
        mTabs!!.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (!isInit) return
                saveHoursMinutes()
                mSelectedTab = tab.position
                refreshPicker()
                if (mPickerHoursLabel != null) mPickerHoursLabel!!.performClick()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        return root
    }

    private fun saveHoursMinutes() {
        val hoursMinutes =
            HoursMinutes(mPicker!!.currentHour.toLong(), mPicker!!.currentMinute.toLong())
        if (mSelectedTab == TAB_FROM) mFrom =
            hoursMinutes else mTo = hoursMinutes
    }

    private val isInit: Boolean
        private get() = mOkButton != null && mPicker != null

    private fun refreshPicker() {
        if (!isInit) return
        val hoursMinutes: HoursMinutes?
        val okBtnRes: Int
        if (mSelectedTab == TAB_FROM) {
            hoursMinutes = mFrom
            okBtnRes = R.string.whats_new_next_button
        } else {
            hoursMinutes = mTo
            okBtnRes = R.string.ok
        }
        mPicker!!.currentMinute = hoursMinutes!!.minutes.toInt()
        mPicker!!.currentHour = hoursMinutes.hours.toInt()
        mOkButton!!.setText(okBtnRes)
    }

    companion object {
        private const val EXTRA_FROM = "HoursMinutesFrom"
        private const val EXTRA_TO = "HoursMinutesTo"
        private const val EXTRA_SELECT_FIRST = "SelectedTab"
        private const val EXTRA_ID = "Id"
        const val TAB_FROM = 0
        const val TAB_TO = 1
        fun pick(
            context: Context?,
            manager: FragmentManager?,
            from: HoursMinutes,
            to: HoursMinutes,
            @IntRange(from = 0, to = 1) selectedPosition: Int,
            id: Int
        ) {
            val args = Bundle()
            args.putParcelable(EXTRA_FROM, from)
            args.putParcelable(EXTRA_TO, to)
            args.putInt(EXTRA_SELECT_FIRST, selectedPosition)
            args.putInt(EXTRA_ID, id)
            val fragment = Fragment.instantiate(
                context!!,
                HoursMinutesPickerFragment::class.java.name,
                args
            ) as HoursMinutesPickerFragment
            fragment.show(manager!!, null)
        }
    }
}