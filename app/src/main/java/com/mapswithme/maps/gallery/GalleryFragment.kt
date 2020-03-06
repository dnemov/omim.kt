package com.mapswithme.maps.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment
import com.mapswithme.maps.widget.recycler.GridDividerItemDecoration
import com.mapswithme.maps.widget.recycler.RecyclerClickListener
import java.util.*

class GalleryFragment : BaseMwmFragment(), RecyclerClickListener {
    private var mImages: ArrayList<Image>? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        readArguments()
        if (mImages != null) {
            val rvGallery =
                view.findViewById<View>(R.id.rv__gallery) as RecyclerView
            rvGallery.layoutManager = GridLayoutManager(
                context,
                NUM_COLUMNS
            )
            rvGallery.adapter = ImageAdapter(mImages!!, this)
            val divider =
                ContextCompat.getDrawable(context!!, R.drawable.divider_transparent_quarter)
            rvGallery.addItemDecoration(
                GridDividerItemDecoration(
                    divider!!,
                    divider,
                    NUM_COLUMNS
                )
            )
        }
    }

    private fun readArguments() {
        val arguments = arguments ?: return
        mImages = arguments.getParcelableArrayList(GalleryActivity.Companion.EXTRA_IMAGES)
    }

    override fun onItemClick(v: View?, position: Int) {
        FullScreenGalleryActivity.Companion.start(context, mImages, position)
    }

    companion object {
        private const val NUM_COLUMNS = 3
    }
}