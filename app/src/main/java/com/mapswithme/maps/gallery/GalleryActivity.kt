package com.mapswithme.maps.gallery

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.mapswithme.maps.base.BaseMwmExtraTitleActivity
import java.util.*

class GalleryActivity : BaseMwmExtraTitleActivity() {
    override val fragmentClass: Class<out Fragment>
        get() = GalleryFragment::class.java

    companion object {
        const val EXTRA_IMAGES = "gallery_images"
        @kotlin.jvm.JvmStatic
        fun start(
            context: Context,
            images: ArrayList<Image>,
            title: String
        ) {
            val i = Intent(context, GalleryActivity::class.java)
            i.putParcelableArrayListExtra(EXTRA_IMAGES, images)
            i.putExtra(EXTRA_TITLE, title)
            context.startActivity(i)
        }
    }
}