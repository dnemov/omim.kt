package com.mapswithme.maps.search

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.search.HotelsFilter.HotelType
import com.mapswithme.maps.search.HotelsTypeAdapter
import com.mapswithme.maps.search.HotelsTypeAdapter.HotelsTypeViewHolder
import com.mapswithme.util.UiUtils
import com.mapswithme.util.log.LoggerFactory
import java.util.*

internal class HotelsTypeAdapter(private val mListener: OnTypeSelectedListener?) :
    RecyclerView.Adapter<HotelsTypeViewHolder>() {
    private val mItems: MutableList<Item>
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelsTypeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return HotelsTypeViewHolder(
            inflater.inflate(R.layout.item_tag, parent, false), mItems,
            mListener
        )
    }

    override fun onBindViewHolder(holder: HotelsTypeViewHolder, position: Int) {
        holder.bind(mItems[position])
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun updateItems(selectedTypes: Set<HotelType>) {
        for (item in mItems) item.mSelected =
            selectedTypes.contains(item.mType)
        notifyDataSetChanged()
    }

    internal class HotelsTypeViewHolder(
        private val mFrame: View,
        private val mItems: List<Item>,
        private val mListener: OnTypeSelectedListener?
    ) : RecyclerView.ViewHolder(mFrame), View.OnClickListener {
        private val mTitle: TextView
        fun bind(item: Item) {
            mTitle.text = getStringResourceByTag(item.mType.tag)
            mFrame.isSelected = item.mSelected
            updateTitleColor()
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            if (position == RecyclerView.NO_POSITION) return
            mFrame.isSelected = !mFrame.isSelected
            updateTitleColor()
            val item = mItems[position]
            item.mSelected = mFrame.isSelected
            mListener?.onTypeSelected(item.mSelected, item.mType)
        }

        private fun getStringResourceByTag(tag: String): String {
            try {
                val context = mFrame.context
                val resources = context.resources
                return resources.getString(
                    resources.getIdentifier(
                        String.format(SEARCH_HOTEL_FILTER, tag),
                        "string",
                        context.packageName
                    )
                )
            } catch (e: Resources.NotFoundException) {
                LOGGER.e(
                    TAG,
                    "Not found resource for hotel tag $tag",
                    e
                )
            }
            return tag
        }

        private fun updateTitleColor() {
            val select = mFrame.isSelected
            @ColorRes val titleColor = if (select) UiUtils.getStyledResourceId(
                mFrame.context,
                R.attr.accentButtonTextColor
            ) else UiUtils.getStyledResourceId(mFrame.context, android.R.attr.textColorPrimary)
            mTitle.setTextColor(ContextCompat.getColor(mFrame.context, titleColor))
        }

        init {
            mTitle = mFrame.findViewById<View>(R.id.tv__tag) as TextView
            mFrame.setOnClickListener(this)
        }
    }

    internal interface OnTypeSelectedListener {
        fun onTypeSelected(selected: Boolean, type: HotelType)
    }

    internal class Item(val mType: HotelType) {
        var mSelected = false
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = HotelsTypeAdapter::class.java.name
        private val SEARCH_HOTEL_FILTER = MwmApplication
            .get().getString(R.string.search_hotel_filter)
        private val TYPES = SearchEngine.nativeGetHotelTypes()
    }

    init {
        mItems = ArrayList()
        for (type in TYPES) mItems.add(
            Item(
                type!!
            )
        )
    }
}