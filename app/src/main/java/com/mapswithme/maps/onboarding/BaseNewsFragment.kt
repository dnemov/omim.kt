package com.mapswithme.maps.onboarding

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.base.BaseMwmDialogFragment
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils
import com.mapswithme.util.statistics.Statistics
import java.util.*

abstract class BaseNewsFragment : BaseMwmDialogFragment() {
    private var mPager: ViewPager? = null
    private var mPrevButton: View? = null
    private var mNextButton: View? = null
    private var mDoneButton: View? = null
    private lateinit var mDots: Array<ImageView?>
    private var mPageCount = 0

    protected var listener: NewsDialogListener? = null
        private set

    inner abstract class Adapter : PagerAdapter() {
        private val mImages: IntArray
        private val mTitles: Array<String?>
        private val mSubtitles: Array<String>
        private val mPromoButtons: List<PromoButton>
        private val mSwitchTitles: Array<String>
        private val mSwitchSubtitles: Array<String>
        private fun getTitles(res: Resources): Array<String?> {
            val keys = res.getStringArray(titleKeys)
            val length = keys.size
            if (length == 0) throw AssertionError("Title keys must me non-empty!")
            val titles = arrayOfNulls<String>(length)
            for (i in 0 until length) titles[i] =
                Utils.getStringValueByKey(context!!, keys[i])
            return titles
        }

        @get:ArrayRes
        abstract val titleKeys: Int

        @get:ArrayRes
        abstract val subtitles1: Int

        @get:ArrayRes
        abstract val subtitles2: Int

        @get:ArrayRes
        abstract val buttonLabels: Int

        @get:ArrayRes
        abstract val buttonLinks: Int

        @get:ArrayRes
        abstract val switchTitles: Int

        @get:ArrayRes
        abstract val switchSubtitles: Int

        @get:ArrayRes
        abstract val images: Int

        override fun getCount(): Int {
            return mImages.size
        }

        override fun isViewFromObject(
            view: View,
            `object`: Any
        ): Boolean {
            return view === `object`
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val res = LayoutInflater.from(container.context)
                .inflate(R.layout.news_page, container, false)
            (res.findViewById<View>(R.id.image) as ImageView)
                .setImageResource(mImages[position])
            (res.findViewById<View>(R.id.title) as TextView).text = mTitles[position]
            (res.findViewById<View>(R.id.subtitle) as TextView).text = mSubtitles[position]
            processSwitchBlock(position, res)
            processButton(position, res)
            container.addView(res)
            return res
        }

        private fun getPromoButtons(res: Resources): List<PromoButton> {
            val labels = res.getStringArray(buttonLabels)
            val links = res.getStringArray(buttonLinks)
            if (labels.size != links.size) throw AssertionError("Button labels count must be equal to links count!")
            val result: MutableList<PromoButton> =
                ArrayList()
            for (i in labels.indices) result.add(PromoButton(labels[i], links[i]))
            return result
        }

        private fun processSwitchBlock(position: Int, res: View) {
            val switchBlock =
                res.findViewById<View>(R.id.switch_block)
            val text = mSwitchTitles[position]
            if (TextUtils.isEmpty(text)) UiUtils.hide(switchBlock) else {
                (switchBlock.findViewById<View>(R.id.switch_title) as TextView).text = text
                val subtitle = switchBlock.findViewById<TextView>(R.id.switch_subtitle)
                if (TextUtils.isEmpty(mSwitchSubtitles[position])) UiUtils.hide(subtitle) else subtitle.text =
                    mSwitchSubtitles[position]
                val checkBox: SwitchCompat = switchBlock.findViewById(R.id.switch_box)
                checkBox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                    onSwitchChanged(
                        position,
                        isChecked
                    )
                }
                switchBlock.setOnClickListener { v: View? -> checkBox.performClick() }
            }
        }

        private fun processButton(position: Int, res: View) {
            val button = res.findViewById<TextView>(R.id.button)
            val promo = mPromoButtons[position]
            if (promo == null || TextUtils.isEmpty(promo.label)) {
                UiUtils.hide(button)
                return
            }
            button.text = promo.label
            button.setOnClickListener { v: View? ->
                Utils.openUrl(
                    context!!,
                    promo.link
                )
            }
        }

        override fun destroyItem(
            container: ViewGroup,
            position: Int,
            `object`: Any
        ) {
            container.removeView(`object` as View)
        }

        init {
            val res = MwmApplication.get().resources
            mTitles = getTitles(res)
            mSubtitles = res.getStringArray(subtitles1)
            val subtitles2 = subtitles2
            if (subtitles2 != 0) {
                val strings = res.getStringArray(subtitles2)
                for (i in mSubtitles.indices) {
                    val s = strings[i]
                    if (!TextUtils.isEmpty(s)) mSubtitles[i] += "\n\n" + s
                }
            }
            mPromoButtons = getPromoButtons(res)
            mSwitchTitles = res.getStringArray(switchTitles)
            mSwitchSubtitles = res.getStringArray(switchSubtitles)
            val images = res.obtainTypedArray(images)
            mImages = IntArray(images.length())
            for (i in mImages.indices) mImages[i] = images.getResourceId(i, 0)
            images.recycle()
        }
    }

    fun onSwitchChanged(index: Int, isChecked: Boolean) {}
    private fun update() {
        val cur = mPager!!.currentItem
        UiUtils.showIf(cur > 0, mPrevButton)
        UiUtils.showIf(cur + 1 < mPageCount, mNextButton)
        UiUtils.visibleIf(cur + 1 == mPageCount, mDoneButton)
        if (mPageCount == 1) return
        for (i in 0 until mPageCount) {
            mDots[i]
                ?.setImageResource(if (ThemeUtils.isNightTheme) if (i == cur) R.drawable.news_marker_active_night else R.drawable.news_marker_inactive_night else if (i == cur) R.drawable.news_marker_active else R.drawable.news_marker_inactive)
        }
    }

    private fun fixPagerSize() {
        if (!UiUtils.isTablet) return
        UiUtils.waitLayout(mPager!!, ViewTreeObserver.OnGlobalLayoutListener {
            val maxWidth = UiUtils.dimen(R.dimen.news_max_width)
            val maxHeight = UiUtils.dimen(R.dimen.news_max_height)
            if (mPager!!.width > maxWidth || mPager!!.height > maxHeight) {
                mPager!!.layoutParams = LinearLayout.LayoutParams(
                    Math.min(maxWidth, mPager!!.width),
                    Math.min(maxHeight, mPager!!.height)
                )
            }
        })
    }

    abstract fun createAdapter(): Adapter
    override val customTheme: Int
        protected get() = if (UiUtils.isTablet) super.customTheme else fullscreenTheme

    @get:StyleRes
    override val fullscreenLightTheme: Int
        protected get() = R.style.MwmTheme_DialogFragment_NoFullscreen

    @get:StyleRes
    override val fullscreenDarkTheme: Int
        protected get() = R.style.MwmTheme_DialogFragment_NoFullscreen_Night

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val res = super.onCreateDialog(savedInstanceState)
        res.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val content =
            View.inflate(activity, R.layout.fragment_news, null)
        res.setContentView(content)
        mPager = content.findViewById<View>(R.id.pager) as ViewPager
        fixPagerSize()
        val adapter = createAdapter()
        mPageCount = adapter.count
        mPager!!.adapter = adapter
        mPager!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                update()
            }
        })
        val dots = content.findViewById<View>(R.id.dots) as ViewGroup
        if (mPageCount == 1) UiUtils.hide(dots) else {
            val dotCount = dots.childCount
            mDots = arrayOfNulls(mPageCount)
            for (i in 0 until dotCount) {
                val dot = dots.getChildAt(i) as ImageView
                if (i < dotCount - mPageCount) UiUtils.hide(dot) else mDots[i - (dotCount - mPageCount)] =
                    dot
            }
        }
        mPrevButton = content.findViewById(R.id.back)
        mNextButton = content.findViewById(R.id.next)
        mDoneButton = content.findViewById(R.id.done)
        mPrevButton?.setOnClickListener(View.OnClickListener {
            mPager!!.setCurrentItem(
                mPager!!.currentItem - 1,
                true
            )
        })
        mNextButton?.setOnClickListener(View.OnClickListener {
            trackStatisticEvent(Statistics.ParamValue.NEXT)
            mPager!!.setCurrentItem(mPager!!.currentItem + 1, true)
        })
        mDoneButton?.setOnClickListener(View.OnClickListener { onDoneClick() })
        update()
        trackStatisticEvent(Statistics.ParamValue.OPEN)
        return res
    }

    private fun trackStatisticEvent(value: String) {
        val builder = Statistics
            .params()
            .add(Statistics.EventParam.ACTION, value)
            .add(
                Statistics.EventParam.VERSION,
                BuildConfig.VERSION_NAME
            )
        Statistics.INSTANCE.trackEvent(
            Statistics.EventName.WHATS_NEW_ACTION,
            builder
        )
    }

    @CallSuper
    protected open fun onDoneClick() {
        dismissAllowingStateLoss()
        if (listener != null) listener!!.onDialogDone()
        trackStatisticEvent(Statistics.ParamValue.CLOSE)
    }

    interface NewsDialogListener {
        fun onDialogDone()
    }

    companion object {
        @ArrayRes
        val TITLE_KEYS = R.array.news_title_keys

        fun create(
            activity: FragmentActivity,
            clazz: Class<out BaseNewsFragment>,
            listener: NewsDialogListener?
        ) {
            try {
                val fragment = clazz.newInstance()
                fragment.listener = listener
                activity.supportFragmentManager
                    .beginTransaction()
                    .add(fragment, clazz.name)
                    .commitAllowingStateLoss()
            } catch (ignored: java.lang.InstantiationException) {
            } catch (ignored: IllegalAccessException) {
            }
        }

        fun recreate(
            activity: FragmentActivity,
            clazz: Class<out BaseNewsFragment?>
        ): Boolean {
            val fm = activity.supportFragmentManager
            val f = fm.findFragmentByTag(clazz.name) ?: return false
            // If we're here, it means that the user has rotated the screen.
// We use different dialog themes for landscape and portrait modes on tablets,
// so the fragment should be recreated to be displayed correctly.
            fm.beginTransaction().remove(f).commitAllowingStateLoss()
            fm.executePendingTransactions()
            return true
        }
    }
}