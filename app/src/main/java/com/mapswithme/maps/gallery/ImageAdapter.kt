package com.mapswithme.maps.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mapswithme.maps.R
import com.mapswithme.maps.widget.recycler.RecyclerClickListener
import java.util.*

internal class ImageAdapter(
    private val mItems: ArrayList<Image>,
    private val mListener: RecyclerClickListener?
) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_image, parent, false), mListener
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(mItems[position], position)
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    internal class ViewHolder(
        itemView: View,
        private val mListener: RecyclerClickListener?
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val mImage: ImageView
        private var mPosition = 0
        override fun onClick(v: View) {
            mListener?.onItemClick(v, mPosition)
        }

        fun bind(image: Image, position: Int) {
            mPosition = position
            Glide.with(mImage.context)
                .load(image.smallUrl)
                .centerCrop()
                .into(mImage)
        }

        init {
            itemView.setOnClickListener(this)
            mImage =
                itemView.findViewById<View>(R.id.iv__image) as ImageView
        }
    }

}