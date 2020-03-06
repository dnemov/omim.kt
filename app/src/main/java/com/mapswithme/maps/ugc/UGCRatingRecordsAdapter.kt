package com.mapswithme.maps.ugc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.util.Utils
import java.util.*

internal class UGCRatingRecordsAdapter :
    RecyclerView.Adapter<UGCRatingRecordsAdapter.ViewHolder>() {
    private val mItems =
        ArrayList<UGC.Rating>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ugc_rating_record, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(mItems[position])
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    fun setItems(items: List<UGC.Rating>) {
        mItems.clear()
        mItems.addAll(items)
        notifyDataSetChanged()
    }

    internal class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val mName: TextView
        val mBar: RatingBar
        fun bind(rating: UGC.Rating?) {
            @StringRes val nameId =
                Utils.getStringIdByKey(mName.context, rating!!.mName!!)
            if (nameId != Utils.INVALID_ID) mName.setText(nameId)
            mBar.rating = rating.mValue
        }

        init {
            mName = itemView.findViewById<View>(R.id.name) as TextView
            mBar = itemView.findViewById<View>(R.id.rating) as RatingBar
        }
    }
}