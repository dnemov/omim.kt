package com.mapswithme.maps.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragment

class FullScreenGalleryFragment : BaseMwmFragment() {
    private var mImage: Image? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fullscreen_image, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        readArguments()
        if (mImage != null) {
            val imageView =
                view.findViewById<View>(R.id.iv__image) as ImageView
            val progress =
                view.findViewById<View>(R.id.pb__loading_image)
            Glide.with(view.context)
                .load(mImage!!.url)
                .listener(object : RequestListener<String?, GlideDrawable?> {
                    override fun onException(
                        e: Exception,
                        model: String?,
                        target: Target<GlideDrawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (isVisible) {
                            progress.visibility = View.GONE
                            Toast.makeText(
                                context, getString(R.string.download_failed),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return false
                    }

                    override fun onResourceReady(
                        resource: GlideDrawable?,
                        model: String?,
                        target: Target<GlideDrawable?>,
                        isFromMemoryCache: Boolean,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (isVisible) progress.visibility = View.GONE
                        return false
                    }
                })
                .into(imageView)
        }
    }

    private fun readArguments() {
        val args = arguments
        if (args != null) mImage =
            args.getParcelable(ARGUMENT_IMAGE)
    }

    companion object {
        const val ARGUMENT_IMAGE = "argument_image"
    }
}