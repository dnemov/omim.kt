package com.mapswithme.maps.ugc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.RatingBar.OnRatingBarChangeListener
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.util.Utils
import java.util.*

internal class UGCRatingAdapter :
    RecyclerView.Adapter<UGCRatingAdapter.ViewHolder>() {
    private val mItems =
        ArrayList<UGC.Rating>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ugc_rating, parent, false)
        return ViewHolder(itemView)
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

    var items: List<UGC.Rating>
        get() = mItems
        set(items) {
            mItems.clear()
            mItems.addAll(items)
            notifyDataSetChanged()
        }

    internal inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val mName: TextView
        val mBar: RatingBar
        fun bind(rating: UGC.Rating) {
            @StringRes val nameId =
                Utils.getStringIdByKey(mName.context, rating.mName!!)
            if (nameId != Utils.INVALID_ID) mName.setText(nameId)
            mBar.rating = rating.mValue
        }

        init {
            mName = itemView.findViewById<View>(R.id.tv__name) as TextView
            mBar = itemView.findViewById<View>(R.id.rb__rate) as RatingBar
            mBar.onRatingBarChangeListener =
                OnRatingBarChangeListener { ratingBar, rating, fromUser ->
                    val position = adapterPosition
                    if (position >= mItems.size) throw AssertionError("Adapter position must be in range [0; mItems.size() - 1]!")
                    val item = mItems[position]
                    item.mValue = rating
                }
        }
    }
}