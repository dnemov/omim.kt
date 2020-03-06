package com.mapswithme.maps.gallery

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmFragmentActivity
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils
import java.util.*

class FullScreenGalleryActivity : BaseMwmFragmentActivity(), OnPageChangeListener {
    private var mImages: List<Image>? = null
    private var mPosition = 0
    private var mUserBlock: View? = null
    private var mDescription: TextView? = null
    private var mUserName: TextView? = null
    private var mSource: TextView? = null
    private var mDate: TextView? = null
    private var mAvatar: ImageView? = null
    private var mGalleryPageAdapter: GalleryPageAdapter? = null
    override fun onSafeCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onSafeCreate(savedInstanceState)
        val toolbar = toolbar!!
        toolbar.title = ""
        UiUtils.showHomeUpButton(toolbar)
        displayToolbarAsActionBar()
        mUserBlock = findViewById(R.id.rl__user_block)
        mDescription = findViewById<View>(R.id.tv__description) as TextView
        mUserName = findViewById<View>(R.id.tv__name) as TextView
        mSource = findViewById<View>(R.id.tv__source) as TextView
        mDate = findViewById<View>(R.id.tv__date) as TextView
        mAvatar = findViewById<View>(R.id.iv__avatar) as ImageView
        readParameters()
        if (mImages != null) {
            mGalleryPageAdapter = GalleryPageAdapter(supportFragmentManager, mImages!!)
            val viewPager = findViewById<View>(R.id.vp__image) as ViewPager
            viewPager.addOnPageChangeListener(this)
            viewPager.adapter = mGalleryPageAdapter
            viewPager.currentItem = mPosition
            viewPager.post { onPageSelected(viewPager.currentItem) }
        }
    }

    @StyleRes
    override fun getThemeResourceId(theme: String): Int {
        if (ThemeUtils.isDefaultTheme(theme)) return R.style.MwmTheme_FullScreenGalleryActivity
        if (ThemeUtils.isNightTheme(theme)) return R.style.MwmTheme_Night_FullScreenGalleryActivity
        throw IllegalArgumentException("Attempt to apply unsupported theme: $theme")
    }

    override val contentLayoutResId: Int
        protected get() = R.layout.activity_full_screen_gallery

    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) {
    }

    override fun onPageSelected(position: Int) {
        updateInformation(mGalleryPageAdapter!!.getImage(position))
    }

    override fun onPageScrollStateChanged(state: Int) {}
    private fun readParameters() {
        val extras = intent.extras
        if (extras != null) {
            mImages =
                extras.getParcelableArrayList(EXTRA_IMAGES)
            mPosition = extras.getInt(EXTRA_POSITION)
        }
    }

    private fun updateInformation(image: Image) {
        UiUtils.setTextAndHideIfEmpty(mDescription, image.description)
        UiUtils.setTextAndHideIfEmpty(mUserName, image.userName)
        UiUtils.setTextAndHideIfEmpty(mSource, image.source)
        updateDate(image)
        updateUserAvatar(image)
        updateUserBlock()
    }

    private fun updateDate(image: Image) {
        if (image.date != null) {
            val date = Date(image.date!!)
            mDate!!.text = DateFormat.getMediumDateFormat(this).format(date)
            UiUtils.show(mDate)
        } else {
            UiUtils.hide(mDate)
        }
    }

    private fun updateUserAvatar(image: Image) {
        if (!TextUtils.isEmpty(image.userAvatar)) {
            UiUtils.show(mAvatar)
            Glide.with(this)
                .load(image.userAvatar)
                .asBitmap()
                .centerCrop()
                .into(object : BitmapImageViewTarget(mAvatar) {
                    override fun setResource(resource: Bitmap) {
                        val circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(resources, resource)
                        circularBitmapDrawable.isCircular = true
                        mAvatar!!.setImageDrawable(circularBitmapDrawable)
                    }
                })
        } else UiUtils.hide(mAvatar)
    }

    private fun updateUserBlock() {
        if (UiUtils.isHidden(mUserName)
            && UiUtils.isHidden(mSource)
            && UiUtils.isHidden(mDate)
            && UiUtils.isHidden(mAvatar)
        ) {
            UiUtils.hide(mUserBlock)
        } else {
            UiUtils.show(mUserBlock)
        }
    }

    companion object {
        const val EXTRA_IMAGES = "gallery_images"
        const val EXTRA_POSITION = "gallery_position"
        @kotlin.jvm.JvmStatic
        fun start(
            context: Context?,
            images: ArrayList<Image>?,
            position: Int
        ) {
            val i = Intent(context, FullScreenGalleryActivity::class.java)
            i.putParcelableArrayListExtra(EXTRA_IMAGES, images)
            i.putExtra(EXTRA_POSITION, position)
            context!!.startActivity(i)
        }
    }
}