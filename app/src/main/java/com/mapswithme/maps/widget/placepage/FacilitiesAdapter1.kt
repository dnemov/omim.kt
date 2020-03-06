package com.mapswithme.maps.widget.placepage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.placepage.Sponsored.FacilityType
import java.util.*

internal class FacilitiesAdapter :
    RecyclerView.Adapter<FacilitiesAdapter.ViewHolder>() {
    private var mItems: List<FacilityType> = ArrayList()
    private var isShowAll = false
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_facility, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(mItems[position])
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return if (mItems.size > MAX_COUNT && !isShowAll) {
            MAX_COUNT
        } else mItems.size
    }

    fun setItems(items: List<FacilityType>) {
        mItems = items
        notifyDataSetChanged()
    }

    fun setShowAll(showAll: Boolean) {
        isShowAll = showAll
        notifyDataSetChanged()
    }

    fun isShowAll(): Boolean {
        return isShowAll
    }

    internal class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        //    ImageView mIcon;
        var mName: TextView

        override fun onClick(v: View) {
            Toast.makeText(mName.context, mName.text, Toast.LENGTH_LONG).show()
        }

        fun bind(facility: FacilityType) { //      TODO map facility key to image resource id
//      mIcon.setImageResource(R.drawable.ic_entrance);
            mName.text = facility.name
        }

        init {
            //      TODO we need icons from designer
//      mIcon = (ImageView) view.findViewById(R.id.iv__icon);
            mName = view.findViewById<View>(R.id.tv__facility) as TextView
            view.setOnClickListener(this)
        }
    }

    companion object {
        const val MAX_COUNT = 6
    }
}