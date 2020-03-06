package com.mapswithme.maps.bookmarks

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.BookmarkManager.SortingType
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics

class ChooseBookmarksSortingTypeFragment : BaseMwmDialogFragment(),
    RadioGroup.OnCheckedChangeListener {
    private var mListener: ChooseSortingTypeListener? = null

    interface ChooseSortingTypeListener {
        fun onResetSorting()
        fun onSort(@SortingType sortingType: Int)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_sorting_types, container, false)
    }

    override val style: Int
        protected get() = STYLE_NO_TITLE

    @IdRes
    private fun getViewId(sortingType: Int): Int {
        if (sortingType >= 0) {
            when (sortingType) {
                BookmarkManager.SORT_BY_TYPE -> return R.id.sort_by_type
                BookmarkManager.SORT_BY_DISTANCE -> return R.id.sort_by_distance
                BookmarkManager.SORT_BY_TIME -> return R.id.sort_by_time
            }
        }
        return R.id.sort_by_default
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments
            ?: throw AssertionError("Arguments of choose sorting type view can't be null.")
        UiUtils.hide(view, R.id.sort_by_type, R.id.sort_by_distance, R.id.sort_by_time)
        @SortingType val availableSortingTypes =
            args.getIntArray(EXTRA_SORTING_TYPES)
                ?: throw AssertionError("Available sorting types can't be null.")
        for (@SortingType type in availableSortingTypes) UiUtils.show(
            view.findViewById(
                getViewId(type)
            )
        )
        val currentType =
            args.getInt(EXTRA_CURRENT_SORT_TYPE)
        val radioGroup = view.findViewById<RadioGroup>(R.id.sorting_types)
        radioGroup.clearCheck()
        radioGroup.check(getViewId(currentType))
        radioGroup.setOnCheckedChangeListener(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onAttachInternal()
    }

    private fun onAttachInternal() {
        mListener =
            (if (parentFragment == null) targetFragment else parentFragment) as ChooseSortingTypeListener?
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun resetSorting() {
        if (mListener != null) mListener!!.onResetSorting()
        trackBookmarksListResetSort()
        dismiss()
    }

    private fun setSortingType(@SortingType sortingType: Int) {
        if (mListener != null) mListener!!.onSort(sortingType)
        trackBookmarksListSort(sortingType)
        dismiss()
    }

    override fun onCheckedChanged(group: RadioGroup, @IdRes id: Int) {
        when (id) {
            R.id.sort_by_default -> resetSorting()
            R.id.sort_by_type -> setSortingType(BookmarkManager.SORT_BY_TYPE)
            R.id.sort_by_distance -> setSortingType(BookmarkManager.SORT_BY_DISTANCE)
            R.id.sort_by_time -> setSortingType(BookmarkManager.SORT_BY_TIME)
        }
    }

    companion object {
        private const val EXTRA_SORTING_TYPES = "sorting_types"
        private const val EXTRA_CURRENT_SORT_TYPE = "current_sort_type"

        @JvmStatic
        fun chooseSortingType(
            @SortingType availableTypes: IntArray,
            currentType: Int, context: Context,
            manager: FragmentManager
        ) {
            val args = Bundle()
            args.putIntArray(
                EXTRA_SORTING_TYPES,
                availableTypes
            )
            args.putInt(
                EXTRA_CURRENT_SORT_TYPE,
                currentType
            )
            val name = ChooseBookmarksSortingTypeFragment::class.java.name
            val fragment =
                Fragment.instantiate(
                    context,
                    name,
                    args
                ) as ChooseBookmarksSortingTypeFragment
            fragment.arguments = args
            fragment.show(manager, name)
        }

        private fun trackBookmarksListSort(@SortingType sortingType: Int) {
            Statistics.INSTANCE.trackBookmarksListSort(sortingType)
        }

        private fun trackBookmarksListResetSort() {
            Statistics.INSTANCE.trackBookmarksListResetSort()
        }
    }
}