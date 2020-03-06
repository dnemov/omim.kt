package com.mapswithme.maps.widget.placepage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.placepage.Sponsored.NearbyObject
import com.mapswithme.util.ThemeUtils
import java.util.*

internal class NearbyAdapter(private val mListener: OnItemClickListener?) :
    BaseAdapter() {
    private var mItems: List<NearbyObject> = ArrayList()

    internal interface OnItemClickListener {
        fun onItemClick(item: NearbyObject)
    }

    override fun getCount(): Int {
        return mItems.size
    }

    override fun getItem(position: Int): Any {
        return mItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(
        position: Int,
        convertView: View,
        parent: ViewGroup
    ): View {
        var convertView = convertView
        val holder: ViewHolder
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_nearby, parent, false)
            holder = ViewHolder(
                convertView,
                mListener
            )
            convertView.tag = holder
        } else {
            holder =
                convertView.tag as ViewHolder
        }
        holder.bind(mItems[position])
        return convertView
    }

    fun setItems(items: List<NearbyObject>) {
        mItems = items
        notifyDataSetChanged()
    }

    private class ViewHolder(
        view: View,
        val mListener: OnItemClickListener?
    ) : View.OnClickListener {
        var mIcon: ImageView
        var mTitle: TextView
        var mType: TextView
        var mDistance: TextView
        var mItem: NearbyObject? = null
        override fun onClick(v: View) {
            if (mListener != null && mItem != null) mListener.onItemClick(mItem!!)
        }

        fun bind(item: NearbyObject) {
            mItem = item
            val packageName = mType.context.packageName
            val isNightTheme = ThemeUtils.isNightTheme
            val resources = mType.resources
            val categoryRes =
                resources.getIdentifier(item.category, "string", packageName)
            check(categoryRes != 0) { "Can't get string resource id for category:" + item.category }
            var iconId = "ic_category_" + item.category
            if (isNightTheme) iconId = iconId + "_night"
            val iconRes = resources.getIdentifier(iconId, "drawable", packageName)
            check(iconRes != 0) { "Can't get icon resource id:$iconId" }
            mIcon.setImageResource(iconRes)
            mTitle.text = item.title
            mType.setText(categoryRes)
            mDistance.text = item.distance
        }

        init {
            mIcon = view.findViewById<View>(R.id.iv__icon) as ImageView
            mTitle = view.findViewById<View>(R.id.tv__title) as TextView
            mType = view.findViewById<View>(R.id.tv__type) as TextView
            mDistance = view.findViewById<View>(R.id.tv__distance) as TextView
            view.setOnClickListener(this)
        }
    }

}