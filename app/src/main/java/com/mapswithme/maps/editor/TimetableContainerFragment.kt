package com.mapswithme.maps.editor

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.maps.base.OnBackPressListener
import com.mapswithme.util.UiUtils

class TimetableContainerFragment : BaseMwmFragment(), OnBackPressListener,
    TimetableChangedListener {
    private enum class Mode {
        SIMPLE {

            override val fragmentClassname: String
                get() = SimpleTimetableFragment::class.java.name

            override val switchButtonLabel: Int
                @StringRes get() = R.string.editor_time_advanced
        },
        ADVANCED {

            override val fragmentClassname: String
                get() = AdvancedTimetableFragment::class.java.name

            override val switchButtonLabel: Int
                @StringRes get() = R.string.editor_time_simple

            override fun setTimetableChangedListener(
                fragment: Fragment,
                listener: TimetableChangedListener
            ) {
                (fragment as AdvancedTimetableFragment).setTimetableChangedListener(listener)
            }
        };

        abstract val fragmentClassname: String
        @get:StringRes
        abstract val switchButtonLabel: Int

        open fun setTimetableChangedListener(
            fragment: Fragment,
            listener: TimetableChangedListener
        ) {
        }

        companion object {
            fun getTimetableProvider(fragment: Fragment): TimetableProvider {
                return fragment as TimetableProvider
            }
        }
    }

    private var mMode =
        Mode.ADVANCED
    private val mFragments =
        arrayOfNulls<Fragment>(Mode.values().size)
    private var mTimetableProvider: TimetableProvider? = null
    private lateinit var mSwitchMode: TextView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timetable, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val activity: Activity? = activity
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        initViews(view)
        val args = arguments
        var time: String? = null
        if (args != null) time = args.getString(EXTRA_TIME)
        // Show Simple fragment when opening hours can be represented by UI.
        if (TextUtils.isEmpty(time) || OpeningHours.nativeTimetablesFromString(time) != null) setMode(
            Mode.SIMPLE,
            time
        ) else setMode(Mode.ADVANCED, time)
    }

    val timetable: String?
        get() = if (mTimetableProvider == null) null else mTimetableProvider!!.timetables

    override fun onTimetableChanged(timetable: String?) {
        UiUtils.showIf(
            TextUtils.isEmpty(timetable)
                    || OpeningHours.nativeTimetablesFromString(timetable) != null,
            mSwitchMode
        )
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    private fun initViews(root: View) {
        mSwitchMode = root.findViewById(R.id.tv__mode_switch)
        mSwitchMode.setOnClickListener { v: View? -> switchMode() }
    }

    private fun switchMode() {
        val filledTimetables =
            if (mTimetableProvider != null) mTimetableProvider!!.timetables else null
        if (filledTimetables != null && !OpeningHours.nativeIsTimetableStringValid(filledTimetables)) {
            val activity = activity ?: return
            AlertDialog.Builder(activity)
                .setMessage(R.string.editor_correct_mistake)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        when (mMode) {
            Mode.SIMPLE -> setMode(
                Mode.ADVANCED,
                filledTimetables
            )
            Mode.ADVANCED -> setMode(
                Mode.SIMPLE,
                filledTimetables
            )
        }
    }

    private fun setMode(
        mode: Mode,
        timetables: String?
    ) {
        mMode = mode
        mSwitchMode.setText(mMode.switchButtonLabel)
        if (mFragments[mMode.ordinal] == null) mFragments[mMode.ordinal] =
            instantiate(activity!!, mMode.fragmentClassname)
        val fragment = mFragments[mMode.ordinal]
        childFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment!!)
            .commit()
        mMode.setTimetableChangedListener(fragment, this)
        mTimetableProvider =
            Mode.getTimetableProvider(
                fragment
            )
        mTimetableProvider!!.timetables = timetables
    }

    companion object {
        const val EXTRA_TIME = "Time"
    }
}