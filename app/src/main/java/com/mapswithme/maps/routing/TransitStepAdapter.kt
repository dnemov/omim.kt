package com.mapswithme.maps.routing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.routing.TransitStepAdapter.TransitStepViewHolder
import java.util.*

class TransitStepAdapter : RecyclerView.Adapter<TransitStepViewHolder>() {
    private val mItems: MutableList<TransitStepInfo> =
        ArrayList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TransitStepViewHolder {
        return TransitStepViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.routing_transit_step_view, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: TransitStepViewHolder,
        position: Int
    ) {
        holder.bind(mItems[position])
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun setItems(items: List<TransitStepInfo>) {
        mItems.clear()
        mItems.addAll(items)
        notifyDataSetChanged()
    }

    class TransitStepViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val mView: TransitStepView
        fun bind(info: TransitStepInfo) {
            mView.setTransitStepInfo(info)
        }

        init {
            mView = itemView as TransitStepView
        }
    }
}