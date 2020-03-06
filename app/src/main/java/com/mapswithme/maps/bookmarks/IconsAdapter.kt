package com.mapswithme.maps.bookmarks

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import com.mapswithme.maps.R
import com.mapswithme.maps.bookmarks.data.Icon
import com.mapswithme.util.Graphics

class IconsAdapter(
    context: Context?,
    list: List<Icon>?
) : ArrayAdapter<Icon?>(context!!, 0, 0, list!!) {
    private var mCheckedIconColor = 0
    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var convertView = convertView
        val holder: SpinnerViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            convertView = inflater.inflate(R.layout.color_row, parent, false)
            holder = SpinnerViewHolder(convertView)
            convertView.tag = holder
        } else holder = convertView.tag as SpinnerViewHolder
        val icon = getItem(position)
        val circle: Drawable
        circle = if (icon!!.color == mCheckedIconColor) {
            Graphics.drawCircleAndImage(
                getItem(position)!!.argb(),
                R.dimen.track_circle_size,
                R.drawable.ic_bookmark_none,
                R.dimen.bookmark_icon_size,
                context.resources
            )
        } else {
            Graphics.drawCircle(
                getItem(position)!!.argb(),
                R.dimen.select_color_circle_size,
                context.resources
            )
        }
        holder.icon.setImageDrawable(circle)
        return convertView!!
    }

    private class SpinnerViewHolder internal constructor(convertView: View?) {
        val icon: ImageView

        init {
            icon =
                convertView!!.findViewById<View>(R.id.iv__color) as ImageView
        }
    }

    fun chooseItem(position: Int) {
        mCheckedIconColor = position
        notifyDataSetChanged()
    }
}