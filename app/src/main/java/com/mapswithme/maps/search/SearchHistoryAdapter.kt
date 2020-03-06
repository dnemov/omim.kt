package com.mapswithme.maps.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.routing.RoutingController.Companion.get
import com.mapswithme.maps.widget.SearchToolbarController
import com.mapswithme.util.Graphics

class SearchHistoryAdapter(searchToolbarController: SearchToolbarController) :
    RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {
    private val mSearchToolbarController: SearchToolbarController
    private val mShowMyPosition: Boolean

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mText: TextView

        init {
            mText = itemView as TextView
            Graphics.tint(mText)
        }
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        type: Int
    ): ViewHolder {
        val res: ViewHolder
        when (type) {
            TYPE_ITEM -> {
                res = ViewHolder(
                    LayoutInflater.from(viewGroup.context).inflate(
                        R.layout.item_search_recent,
                        viewGroup,
                        false
                    )
                )
                res.mText.setOnClickListener { mSearchToolbarController.query = res.mText.text.toString() }
            }
            TYPE_CLEAR -> {
                res = ViewHolder(
                    LayoutInflater.from(viewGroup.context).inflate(
                        R.layout.item_search_clear_history,
                        viewGroup,
                        false
                    )
                )
                res.mText.setOnClickListener {
                    SearchRecents.clear()
                    notifyDataSetChanged()
                }
            }
            TYPE_MY_POSITION -> {
                res = ViewHolder(
                    LayoutInflater.from(viewGroup.context).inflate(
                        R.layout.item_search_my_position,
                        viewGroup,
                        false
                    )
                )
                res.mText.setOnClickListener {
                    get().onPoiSelected(LocationHelper.INSTANCE.myPosition)
                    mSearchToolbarController.onUpClick()
                }
            }
            else -> throw IllegalArgumentException("Unsupported ViewHolder type given")
        }
        Graphics.tint(res.mText)
        return res
    }

    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        position: Int
    ) {
        var position = position
        if (getItemViewType(position) == TYPE_ITEM) {
            if (mShowMyPosition) position--
            viewHolder.mText.text = SearchRecents.get(position)
        }
    }

    override fun getItemCount(): Int {
        var res = SearchRecents.size
        if (res > 0) res++
        if (mShowMyPosition) res++
        return res
    }

    override fun getItemViewType(position: Int): Int {
        var position = position
        if (mShowMyPosition) {
            if (position == 0) return TYPE_MY_POSITION
            position--
        }
        return if (position < SearchRecents.size) TYPE_ITEM else TYPE_CLEAR
    }

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_CLEAR = 1
        private const val TYPE_MY_POSITION = 2
    }

    init {
        SearchRecents.refresh()
        mSearchToolbarController = searchToolbarController
        mShowMyPosition = get().isWaitingPoiPick &&
                LocationHelper.INSTANCE.myPosition != null
    }
}