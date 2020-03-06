package com.mapswithme.maps.editor

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.IntRange
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmToolbarFragment
import com.mapswithme.util.UiUtils

class ReportFragment : BaseMwmToolbarFragment(), View.OnClickListener {
    private var mSimpleProblems: View? = null
    private var mAdvancedProblem: View? = null
    private var mSave: View? = null
    private var mProblemInput: EditText? = null
    private var mAdvancedMode = false
    @IntRange(from = 0, to = 3)
    private val mSelectedProblem = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        toolbarController.setTitle(R.string.editor_report_problem_title)
        mSave = toolbarController.toolbar.findViewById(R.id.save)
        mSave?.setOnClickListener(this)
        mSimpleProblems = view.findViewById(R.id.ll__problems)
        mSimpleProblems?.findViewById<View>(R.id.problem_not_exist)?.setOnClickListener(this)
        mSimpleProblems?.findViewById<View>(R.id.problem_other)?.setOnClickListener(this)
        mAdvancedProblem = view.findViewById(R.id.ll__other_problem)
        mProblemInput = mAdvancedProblem?.findViewById<View>(R.id.input) as EditText
        refreshProblems()
    }

    private fun refreshProblems() {
        UiUtils.showIf(mAdvancedMode, mAdvancedProblem, mSave)
        UiUtils.showIf(!mAdvancedMode, mSimpleProblems)
    }

    private fun send(text: String) {
        Editor.nativeCreateNote(text)
        toolbarController.onUpClick()
    }

    private fun sendNotExist() {
        Editor.nativePlaceDoesNotExist("")
        toolbarController.onUpClick()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.problem_not_exist ->  //    case R.id.problem_closed_repair:
//    case R.id.problem_duplicated_place:
                sendNotExist()
            R.id.problem_other -> {
                mAdvancedMode = true
                refreshProblems()
            }
            R.id.save -> {
                val text = mProblemInput!!.text.toString().trim { it <= ' ' }
                if (TextUtils.isEmpty(text)) return
                send(text)
            }
        }
    }
}