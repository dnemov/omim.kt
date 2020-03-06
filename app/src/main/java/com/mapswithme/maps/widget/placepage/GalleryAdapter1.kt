package com.mapswithme.maps.widget.placepage

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.mapswithme.maps.R
import com.mapswithme.maps.gallery.Image
import com.mapswithme.maps.widget.recycler.RecyclerClickListener
import com.mapswithme.util.UiUtils
import java.util.*

internal class GalleryAdapter(private val mContext: Context) :
    RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {
    private var mItems =
        ArrayList<Image>()
    private val mLoadedItems: MutableList<Item> =
        ArrayList()
    private val mItemsToDownload: MutableList<Item> =
        ArrayList()
    private var mListener: RecyclerClickListener? = null
    private val mImageWidth: Int
    private val mImageHeight: Int
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(mContext)
                .inflate(R.layout.item_gallery, parent, false), mListener
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item =
            mLoadedItems[position]
        item.isShowMore = position == MAX_COUNT - 1 && mItems.size > MAX_COUNT
        holder.bind(item, position)
    }

    override fun getItemCount(): Int {
        return mLoadedItems.size
    }

    var items: ArrayList<Image>
        get() = mItems
        set(items) {
            mItems = items
            for (item in mItemsToDownload) {
                item.isCanceled = true
            }
            mItemsToDownload.clear()
            mLoadedItems.clear()
            loadImages()
            notifyDataSetChanged()
        }

    fun setListener(listener: RecyclerClickListener?) {
        mListener = listener
    }

    private fun loadImages() {
        val size = Math.min(
            mItems.size,
            MAX_COUNT
        )
        for (i in 0 until size) {
            val item =
                Item(null)
            mItemsToDownload.add(item)
            val (_, smallUrl) = mItems[i]
            Glide.with(mContext)
                .load(smallUrl)
                .asBitmap()
                .centerCrop()
                .into(object : SimpleTarget<Bitmap?>(mImageWidth, mImageHeight) {
                    override fun onResourceReady(
                        resource: Bitmap?,
                        glideAnimation: GlideAnimation<in Bitmap?>
                    ) {
                        if (item.isCanceled) return
                        item.bitmap = resource
                        val size = mLoadedItems.size
                        mLoadedItems.add(item)
                        notifyItemInserted(size)
                    }
                })
        }
    }

    internal class ViewHolder(
        itemView: View,
        private val mListener: RecyclerClickListener?
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val mImage: ImageView
        private val mMore: View
        private var mPosition = 0
        override fun onClick(v: View) {
            if (mListener == null) return
            mListener.onItemClick(v, mPosition)
        }

        fun bind(
            item: Item,
            position: Int
        ) {
            mPosition = position
            mImage.setImageBitmap(item.bitmap)
            UiUtils.showIf(item.isShowMore, mMore)
        }

        init {
            mImage =
                itemView.findViewById<View>(R.id.iv__image) as ImageView
            mMore = itemView.findViewById(R.id.tv__more)
            itemView.setOnClickListener(this)
        }
    }

    internal class Item(var bitmap: Bitmap?) {
        var isShowMore = false
        var isCanceled = false

    }

    companion object {
        const val MAX_COUNT = 5
    }

    init {
        mImageWidth =
            mContext.resources.getDimension(R.dimen.placepage_hotel_gallery_width).toInt()
        mImageHeight = mContext.resources
            .getDimension(R.dimen.placepage_hotel_gallery_height).toInt()
    }
}