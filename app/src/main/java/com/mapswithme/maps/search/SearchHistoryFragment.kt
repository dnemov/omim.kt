package com.mapswithme.maps.search

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmRecyclerFragment
import com.mapswithme.maps.widget.PlaceholderView
import com.mapswithme.maps.widget.SearchToolbarController
import com.mapswithme.util.UiUtils

class SearchHistoryFragment : BaseMwmRecyclerFragment<SearchHistoryAdapter?>() {
    private var mPlaceHolder: PlaceholderView? = null
    private fun updatePlaceholder() {
        UiUtils.showIf(adapter!!.itemCount == 0, mPlaceHolder)
    }

    override fun createAdapter(): SearchHistoryAdapter {
        return SearchHistoryAdapter((parentFragment as SearchToolbarController.Container).controller!!)
    }

    @get:LayoutRes
    override val layoutRes: Int
        protected get() = R.layout.fragment_search_base

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        mPlaceHolder = view.findViewById<View>(R.id.placeholder) as PlaceholderView
        mPlaceHolder!!.setContent(
            R.drawable.img_search_empty_history_light,
            R.string.search_history_title, R.string.search_history_text
        )
        adapter!!.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() {
                updatePlaceholder()
            }
        })
        updatePlaceholder()
    }

    @CallSuper
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (parentFragment as SearchFragment?)!!.setRecyclerScrollListener(recyclerView)
    }
}