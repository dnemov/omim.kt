package com.mapswithme.maps.gallery

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

internal class GalleryPageAdapter(
    fm: FragmentManager,
    private val mImages: List<Image>
) : FragmentStatePagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        val args = Bundle()
        args.putParcelable(
            FullScreenGalleryFragment.Companion.ARGUMENT_IMAGE,
            mImages[position]
        )
        val fragment = FullScreenGalleryFragment()
        fragment.arguments = args
        return fragment
    }

    override fun getCount(): Int {
        return mImages.size
    }

    fun getImage(position: Int): Image {
        return mImages[position]
    }

}