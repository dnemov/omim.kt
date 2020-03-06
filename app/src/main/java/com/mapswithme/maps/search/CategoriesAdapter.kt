package com.mapswithme.maps.search

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.util.ThemeUtils
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.Statistics

class CategoriesAdapter(fragment: Fragment) :
    RecyclerView.Adapter<CategoriesAdapter.ViewHolder>() {
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(
        TYPE_CATEGORY,
        TYPE_PROMO_CATEGORY
    )
    internal annotation class ViewType

    @StringRes
    private lateinit var mCategoryResIds: IntArray
    @DrawableRes
    private lateinit var mIconResIds: IntArray
    private val mInflater: LayoutInflater
    private val mResources: Resources

    internal interface CategoriesUiListener {
        fun onSearchCategorySelected(category: String)
        fun onPromoCategorySelected(promo: PromoCategory)
        fun onAdsRemovalSelected()
    }

    private var mListener: CategoriesUiListener? = null
    fun updateCategories(fragment: Fragment) {
        val packageName = fragment.activity!!.packageName
        val isNightTheme = ThemeUtils.isNightTheme
        val resources = fragment.activity!!.resources
        val keys = allCategories
        val numKeys = keys.size
        mCategoryResIds = IntArray(numKeys)
        mIconResIds = IntArray(numKeys)
        for (i in 0 until numKeys) {
            val key = keys[i]
            mCategoryResIds[i] = resources.getIdentifier(key, "string", packageName)
            check(mCategoryResIds[i] != 0) { "Can't get string resource id for category:$key" }
            var iconId = "ic_category_$key"
            if (isNightTheme) iconId = iconId + "_night"
            mIconResIds[i] = resources.getIdentifier(iconId, "drawable", packageName)
            check(mIconResIds[i] != 0) { "Can't get icon resource id:$iconId" }
        }
    }

    @ViewType
    override fun getItemViewType(position: Int): Int {
        val promo = PromoCategory.findByStringId(mCategoryResIds[position])
        return if (promo != null) {
            TYPE_PROMO_CATEGORY
        } else TYPE_CATEGORY
    }

    override fun onCreateViewHolder(parent: ViewGroup, @ViewType viewType: Int): ViewHolder {
        val view: View
        val viewHolder: ViewHolder
        when (viewType) {
            TYPE_CATEGORY -> {
                view = mInflater.inflate(R.layout.item_search_category, parent, false)
                viewHolder =
                    ViewHolder(view, (view as TextView))
            }
            TYPE_PROMO_CATEGORY -> {
                view = mInflater.inflate(R.layout.item_search_promo_category, parent, false)
                viewHolder = PromoViewHolder(view, view.findViewById(R.id.promo_title))
            }
            else -> throw AssertionError("Unsupported type detected: $viewType")
        }
        viewHolder.setupClickListeners()
        return viewHolder
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.setTextAndIcon(mCategoryResIds[position], mIconResIds[position])
    }

    override fun getItemCount(): Int {
        return mCategoryResIds.size
    }

    private inner class PromoViewHolder internal constructor(v: View, tv: TextView) :
        ViewHolder(v, tv) {
        private val mIcon: ImageView
        private val mRemoveAds: View
        private val mCallToActionView: TextView
        override fun setupClickListeners() {
            val action =
                view.findViewById<View>(R.id.promo_action)
            action.setOnClickListener(this)
            mRemoveAds.setOnClickListener(RemoveAdsClickListener())
        }

        override fun onItemClicked(position: Int) {
            @StringRes val categoryId = mCategoryResIds[position]
            val promo = PromoCategory.findByStringId(categoryId)
            if (promo != null) {
                val event =
                    Statistics.EventName.SEARCH_SPONSOR_CATEGORY_SELECTED
                Statistics.INSTANCE.trackSearchPromoCategory(
                    event,
                    promo.provider
                )
                if (mListener != null) mListener!!.onPromoCategorySelected(promo)
            }
        }

        override fun setTextAndIcon(textResId: Int, iconResId: Int) {
            title.setText(textResId)
            mIcon.setImageResource(iconResId)
            @StringRes val categoryId = mCategoryResIds[adapterPosition]
            val promo = PromoCategory.findByStringId(categoryId)
            if (promo != null) {
                mCallToActionView.setText(promo.callToActionText)
                val event =
                    Statistics.EventName.SEARCH_SPONSOR_CATEGORY_SHOWN
                Statistics.INSTANCE.trackSearchPromoCategory(
                    event,
                    promo.provider
                )
            }
        }

        private inner class RemoveAdsClickListener :
            View.OnClickListener {
            override fun onClick(v: View) {
                if (mListener != null) mListener!!.onAdsRemovalSelected()
            }
        }

        init {
            mIcon = v.findViewById(R.id.promo_icon)
            mRemoveAds = v.findViewById(R.id.remove_ads)
            mCallToActionView = v.findViewById(R.id.promo_action)
            val res = v.resources
            val crossArea = res.getDimensionPixelSize(R.dimen.margin_base)
            UiUtils.expandTouchAreaForView(mRemoveAds, crossArea)
        }
    }

    inner open class ViewHolder(val view: View, val title: TextView) :
        RecyclerView.ViewHolder(view), View.OnClickListener {
        open fun setupClickListeners() {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            onItemClicked(position)
        }

        open fun onItemClicked(position: Int) {
            val categoryEntryName =
                mResources.getResourceEntryName(mCategoryResIds[position])
            Statistics.INSTANCE.trackSearchCategoryClicked(
                categoryEntryName
            )
            if (mListener != null) {
                @StringRes val categoryId = mCategoryResIds[position]
                mListener!!.onSearchCategorySelected(mResources.getString(categoryId) + " ")
            }
        }

        open fun setTextAndIcon(@StringRes textResId: Int, @DrawableRes iconResId: Int) {
            title.setText(textResId)
            title.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0)
        }

    }

    companion object {
        internal const val TYPE_CATEGORY = 0
        internal const val TYPE_PROMO_CATEGORY = 1
        private val allCategories: Array<String?>
            get() {
                val searchCategories = DisplayedCategories.keys
                val promos = PromoCategory.supportedValues()
                val amountSize = searchCategories.size + promos.size
                val allCategories =
                    arrayOfNulls<String>(amountSize)
                for (promo in promos) {
                    if (promo.position >= amountSize) throw AssertionError(
                        "Promo position must be in range: "
                                + "[0 - " + amountSize + ")"
                    )
                    allCategories[promo.position] = promo.key
                }
                var i = 0
                var j = 0
                while (i < amountSize) {
                    if (allCategories[i] == null) {
                        allCategories[i] = searchCategories[j]
                        j++
                    }
                    i++
                }
                return allCategories
            }
    }

    init {
        if (fragment is CategoriesUiListener) mListener = fragment
        mResources = fragment.resources
        mInflater = LayoutInflater.from(fragment.activity)
    }
}