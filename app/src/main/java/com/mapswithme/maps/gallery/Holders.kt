package com.mapswithme.maps.gallery

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.mapswithme.HotelUtils
import com.mapswithme.maps.R
import com.mapswithme.maps.gallery.Items.LocalExpertItem
import com.mapswithme.maps.gallery.Items.SearchItem
import com.mapswithme.maps.promo.PromoEntity
import com.mapswithme.maps.search.Popularity
import com.mapswithme.maps.ugc.Impress
import com.mapswithme.maps.ugc.UGC.Companion.nativeFormatRating
import com.mapswithme.maps.ugc.UGC.Companion.nativeToImpress
import com.mapswithme.maps.widget.RatingView
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.NetworkPolicy
import com.mapswithme.util.UiUtils
import com.mapswithme.util.Utils

class Holders {
    open class GenericMoreHolder<T : RegularAdapterStrategy.Item?>(
        itemView: View, items: MutableList<T>,
        listener: ItemSelectedListener<T>?
    ) : BaseViewHolder<T>(itemView, items, listener) {
        override fun onItemSelected(item: T, position: Int) {
            val listener: ItemSelectedListener<T>? = listener
            if (listener == null || TextUtils.isEmpty(item!!.url)) return
            listener.onMoreItemSelected(item)
        }
    }

    class SearchMoreHolder(
        itemView: View, items: MutableList<SearchItem>,
        listener: ItemSelectedListener<SearchItem>?
    ) : GenericMoreHolder<SearchItem>(itemView, items, listener) {
        override fun onItemSelected(item: SearchItem, position: Int) {
            val listener: ItemSelectedListener<SearchItem>? = listener
            listener?.onMoreItemSelected(item)
        }
    }

    class LocalExpertViewHolder(
        itemView: View, items: List<LocalExpertItem>,
        listener: ItemSelectedListener<LocalExpertItem>?
    ) : BaseViewHolder<LocalExpertItem>(
        itemView,
        items,
        listener
    ) {
        private val mAvatar: ImageView
        private val mRating: RatingView
        private val mButton: TextView
        override fun bind(item: LocalExpertItem) {
            super.bind(item)
            Glide.with(mAvatar.context)
                .load(item.photoUrl)
                .asBitmap()
                .centerCrop()
                .placeholder(R.drawable.ic_local_expert_default)
                .into(object : BitmapImageViewTarget(mAvatar) {
                    override fun setResource(resource: Bitmap) {
                        val circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(
                                mAvatar.context.resources,
                                resource
                            )
                        circularBitmapDrawable.isCircular = true
                        mAvatar.setImageDrawable(circularBitmapDrawable)
                    }
                })
            val context = mButton.context
            val priceLabel: String
            priceLabel = if (item.price == 0.0 && TextUtils.isEmpty(item.currency)) {
                context.getString(R.string.free)
            } else {
                val formattedPrice = Utils.formatCurrencyString(
                    item.price.toString(),
                    item.currency
                )
                context.getString(R.string.price_per_hour, formattedPrice)
            }
            UiUtils.setTextAndHideIfEmpty(mButton, priceLabel)
            val rating = item.rating.toFloat()
            val impress =
                Impress.values()[nativeToImpress(rating)]
            mRating.setRating(impress, nativeFormatRating(rating))
        }

        init {
            mAvatar =
                itemView.findViewById<View>(R.id.avatar) as ImageView
            mRating = itemView.findViewById<View>(R.id.ratingView) as RatingView
            mButton = itemView.findViewById<View>(R.id.button) as TextView
        }
    }

    abstract class ActionButtonViewHolder<T : RegularAdapterStrategy.Item?> internal constructor(
        itemView: View,
        items: List<T>,
        listener: ItemSelectedListener<T>?
    ) : BaseViewHolder<T>(itemView, items, listener) {
        private val mButton: TextView
        override fun onClick(v: View) {
            val position = adapterPosition
            if (position == RecyclerView.NO_POSITION || mItems.isEmpty()) return
            val listener = listener ?: return
            val item = mItems[position]
            when (v.id) {
                R.id.infoLayout -> listener.onItemSelected(item, position)
                R.id.button -> listener.onActionButtonSelected(item, position)
            }
        }

        init {
            mButton = itemView.findViewById(R.id.button)
            mButton.setOnClickListener(this)
            itemView.findViewById<View>(R.id.infoLayout).setOnClickListener(this)
            mButton.setText(R.string.p2p_to_here)
        }
    }

    class SearchViewHolder(
        itemView: View, items: List<SearchItem>,
        adapter: ItemSelectedListener<SearchItem>?
    ) : ActionButtonViewHolder<SearchItem>(itemView, items, adapter) {
        private val mSubtitle: TextView
        private val mDistance: TextView
        private val mNumberRating: RatingView
        private val mPopularTagRating: RatingView
        override fun bind(item: SearchItem) {
            super.bind(item)
            val featureType = item.featureType
            val localizedType = Utils.getLocalizedFeatureType(
                mSubtitle.context,
                featureType
            )
            val title =
                if (TextUtils.isEmpty(item.title)) localizedType else item.title
            UiUtils.setTextAndHideIfEmpty(super.title, title)
            UiUtils.setTextAndHideIfEmpty(mSubtitle, localizedType)
            UiUtils.setTextAndHideIfEmpty(mDistance, item.distance)
            UiUtils.showIf(item.popularity.type === Popularity.Type.POPULAR, mPopularTagRating)
            val rating = item.rating
            val impress =
                Impress.values()[nativeToImpress(rating)]
            mNumberRating.setRating(impress, nativeFormatRating(rating))
        }

        init {
            mSubtitle = itemView.findViewById(R.id.subtitle)
            mDistance = itemView.findViewById(R.id.distance)
            mNumberRating = itemView.findViewById(R.id.counter_rating_view)
            mPopularTagRating = itemView.findViewById(R.id.popular_rating_view)
        }
    }

    class HotelViewHolder(
        itemView: View,
        items: List<SearchItem>,
        listener: ItemSelectedListener<SearchItem>?
    ) : ActionButtonViewHolder<SearchItem>(itemView, items, listener) {
        private val mTitle: TextView
        private val mSubtitle: TextView
        private val mRatingView: RatingView
        private val mDistance: TextView
        override fun bind(item: SearchItem) {
            val featureType = item.featureType
            val localizedType = Utils.getLocalizedFeatureType(
                mSubtitle.context,
                featureType
            )
            val title =
                if (TextUtils.isEmpty(item.title)) localizedType else item.title
            UiUtils.setTextAndHideIfEmpty(mTitle, title)
            UiUtils.setTextAndHideIfEmpty(
                mSubtitle, formatDescription(
                    item.stars,
                    localizedType,
                    item.price,
                    mSubtitle.resources
                )
            )
            val rating = item.rating
            val impress =
                Impress.values()[nativeToImpress(rating)]
            mRatingView.setRating(impress, nativeFormatRating(rating))
            UiUtils.setTextAndHideIfEmpty(mDistance, item.distance)
        }

        companion object {
            private fun formatDescription(
                stars: Int, type: String?,
                priceCategory: String?,
                res: Resources
            ): CharSequence {
                val sb = SpannableStringBuilder()
                if (stars > 0) sb.append(
                    HotelUtils.formatStars(
                        stars,
                        res
                    )
                ) else if (!TextUtils.isEmpty(type)) sb.append(type)
                if (!TextUtils.isEmpty(priceCategory)) {
                    sb.append(" • ")
                    sb.append(priceCategory)
                }
                return sb
            }
        }

        init {
            mTitle = itemView.findViewById(R.id.title)
            mSubtitle = itemView.findViewById(R.id.subtitle)
            mRatingView = itemView.findViewById(R.id.ratingView)
            mDistance = itemView.findViewById(R.id.distance)
        }
    }

    open class BaseViewHolder<I : Items.Item?>(
        itemView: View,
        items: List<I>,
        listener: ItemSelectedListener<I>?
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        protected val title: TextView
        protected val listener: ItemSelectedListener<I>?
        protected val mItems: List<I>
        open fun bind(item: I) {
            title.text = item!!.title
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            if (position == RecyclerView.NO_POSITION || mItems.isEmpty()) return
            onItemSelected(mItems[position], position)
        }

        protected open fun onItemSelected(item: I, position: Int) {
            val listener = listener
            if (listener == null || TextUtils.isEmpty(item!!.url)) return
            listener.onItemSelected(item, position)
        }

        init {
            title = itemView.findViewById(R.id.title)
            this.listener = listener
            itemView.setOnClickListener(this)
            mItems = items
        }
    }

    open class LoadingViewHolder internal constructor(
        itemView: View,
        items: MutableList<Items.Item>,
        listener: ItemSelectedListener<Items.Item>?
    ) : BaseViewHolder<Items.Item>(
        itemView,
        items,
        listener
    ), View.OnClickListener {
        val mProgressBar: ProgressBar
        val mSubtitle: TextView
        @CallSuper
        override fun bind(item: Items.Item) {
            super.bind(item)
            UiUtils.setTextAndHideIfEmpty(mSubtitle, item.subtitle)
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            if (position == RecyclerView.NO_POSITION) return
            onItemSelected(mItems[position], position)
        }

        override fun onItemSelected(
            item: Items.Item,
            position: Int
        ) {
            if (listener == null || TextUtils.isEmpty(item.url)) return
            listener.onActionButtonSelected(item, position)
        }

        init {
            mProgressBar =
                itemView.findViewById<View>(R.id.pb__progress) as ProgressBar
            mSubtitle = itemView.findViewById<View>(R.id.tv__subtitle) as TextView
        }
    }

    open class SimpleViewHolder(
        itemView: View,
        items: List<Items.Item>,
        listener: ItemSelectedListener<Items.Item>?
    ) : BaseViewHolder<Items.Item>(
        itemView,
        items,
        listener
    )

    internal class ErrorViewHolder(
        itemView: View,
        items: MutableList<Items.Item>,
        listener: ItemSelectedListener<Items.Item>?
    ) : LoadingViewHolder(itemView, items, listener) {
        init {
            UiUtils.hide(mProgressBar)
        }
    }

    class OfflineViewHolder internal constructor(
        itemView: View,
        items: MutableList<Items.Item>,
        listener: ItemSelectedListener<Items.Item>?
    ) : LoadingViewHolder(itemView, items, listener) {
        @CallSuper
        override fun bind(item: Items.Item) {
            super.bind(item)
            UiUtils.setTextAndHideIfEmpty(mSubtitle, item.subtitle)
        }

        override fun onItemSelected(
            item: Items.Item,
            position: Int
        ) {
        }

        init {
            UiUtils.hide(mProgressBar)
        }
    }

    class CatalogPromoHolder(
        itemView: View,
        items: List<PromoEntity>,
        listener: ItemSelectedListener<PromoEntity>?
    ) : BaseViewHolder<PromoEntity>(itemView, items, listener) {
        private val mImage: ImageView
        private val mSubTitle: TextView
        private val mProLabel: TextView
        override fun bind(item: PromoEntity) {
            super.bind(item)
            bindProLabel(item)
            bindSubTitle(item)
            bindImage(item)
        }

        private fun bindSubTitle(item: PromoEntity) {
            mSubTitle.text = item.subtitle
        }

        private fun bindImage(item: PromoEntity) {
            Glide.with(itemView.context)
                .load(Uri.parse(item.imageUrl))
                .placeholder(R.drawable.img_guides_gallery_placeholder)
                .into(mImage)
        }

        private fun bindProLabel(item: PromoEntity) {
            val category = item.category
            UiUtils.showIf(category != null && !TextUtils.isEmpty(category.name), mProLabel)
            if (item.category == null) return
            mProLabel.text = item.category.name
            val shapeDrawable = ShapeDrawable(RectShape())
            shapeDrawable.paint.color = item.category.color
            mProLabel.setBackgroundDrawable(shapeDrawable)
        }

        init {
            mImage = itemView.findViewById(R.id.image)
            mSubTitle = itemView.findViewById(R.id.subtitle)
            mProLabel = itemView.findViewById(R.id.label)
        }
    }

    open class CrossPromoLoadingHolder(
        itemView: View,
        items: List<Items.Item>,
        listener: ItemSelectedListener<Items.Item>?
    ) : SimpleViewHolder(itemView, items, listener) {
        private val mSubTitle: TextView
        protected val button: TextView

        override fun bind(item: Items.Item) {
            super.bind(item)
            title.setText(R.string.gallery_pp_download_guides_offline_title)
            mSubTitle.setText(R.string.gallery_pp_download_guides_offline_subtitle)
            UiUtils.invisible(button)
        }

        init {
            mSubTitle = itemView.findViewById(R.id.subtitle)
            button = itemView.findViewById(R.id.button)
        }
    }

    class CatalogErrorHolder(
        itemView: View,
        items: List<Items.Item>,
        listener: ItemSelectedListener<Items.Item>?
    ) : CrossPromoLoadingHolder(itemView, items, listener) {
        override fun bind(item: Items.Item) {
            super.bind(item)
            button.setText(R.string.gallery_pp_download_guides_offline_cta)
            val isBtnInvisible =
                ConnectionState.isConnected &&
                        NetworkPolicy.newInstance(NetworkPolicy.getCurrentNetworkUsageStatus()).canUseNetwork()
            if (isBtnInvisible) UiUtils.invisible(button) else UiUtils.show(button)
        }

        protected override fun onItemSelected(
            item: Items.Item,
            position: Int
        ) {
            val listener =
                listener ?: return
            listener.onItemSelected(item, position)
        }

        init {
            val progress =
                itemView.findViewById<View>(R.id.progress)
            UiUtils.invisible(progress)
        }
    }
}