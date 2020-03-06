package com.mapswithme.maps.promo

import android.annotation.SuppressLint
import android.app.Activity
import android.text.Html
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mapswithme.maps.R

import com.mapswithme.maps.base.Detachable
import com.mapswithme.maps.bookmarks.BookmarkCategoriesActivity
import com.mapswithme.maps.bookmarks.BookmarksCatalogActivity.Companion.startForResult
import com.mapswithme.maps.bookmarks.data.BookmarkManager
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.bookmarks.data.MapObject.Companion.isOfType
import com.mapswithme.maps.gallery.impl.Factory.createCatalogPromoAdapter
import com.mapswithme.maps.gallery.impl.RegularCatalogPromoListener
import com.mapswithme.maps.widget.placepage.PlacePageView
import com.mapswithme.maps.widget.placepage.Sponsored
import com.mapswithme.maps.widget.placepage.Sponsored.SponsoredType
import com.mapswithme.maps.widget.recycler.ItemDecoratorFactory
import com.mapswithme.util.NetworkPolicy
import com.mapswithme.util.UTM
import com.mapswithme.util.UiUtils
import com.mapswithme.util.statistics.*

class CatalogPromoController(private val mPlacePageView: PlacePageView) :
    Promo.Listener, Detachable<Activity?> {
    private var mActivity: Activity? = null
    private val mRecycler: RecyclerView
    private val mTitle: TextView
    private var mRequester: PromoRequester? = null
    override fun onCityGalleryReceived(promo: PromoCityGallery) {
        val sponsored = mPlacePageView.getSponsored() ?: return
        val handler = createPromoResponseHandler(sponsored.type, promo) ?: return
        handler.handleResponse(promo)
    }

    override fun onErrorReceived() {
        if (mRequester == null) return
        val placement =
            getGalleryPlacement(mRequester!!.sponsoredType)
        Statistics.INSTANCE.trackGalleryError(
            GalleryType.PROMO, placement,
            Statistics.ParamValue.NO_PRODUCTS
        )
    }

    fun updateCatalogPromo(policy: NetworkPolicy, mapObject: MapObject?) {
        UiUtils.hide(mPlacePageView, R.id.catalog_promo_container)
        if (isOfType(MapObject.BOOKMARK, mapObject)) return
        val sponsored = mPlacePageView.getSponsored()
        if (sponsored == null || mapObject == null) return
        mRequester = createPromoRequester(sponsored.type)
        if (mRequester == null) return
        mRequester!!.requestPromo(policy, mapObject)
    }

    override fun attach(`object`: Activity?) {

        mActivity = `object`
        Promo.INSTANCE.setListener(this)
    }

    override fun detach() {
        mActivity = null
        Promo.INSTANCE.setListener(null)
    }

    private fun createPromoResponseHandler(
        @SponsoredType type: Int,
        promo: PromoCityGallery
    ): PromoResponseHandler? {
        if (type != Sponsored.TYPE_PROMO_CATALOG_CITY && type != Sponsored.TYPE_PROMO_CATALOG_SIGHTSEEINGS && type != Sponsored.TYPE_PROMO_CATALOG_OUTDOOR
        ) return null
        val items = promo.items
        if (items.size <= 0) return null
        return if (items.size == 1) SinglePromoResponseHandler(type) else GalleryPromoResponseHandler(
            type
        )
    }

    private fun requireActivity(): Activity {
        if (mActivity == null) throw AssertionError("Activity must be non-null at this point!")
        return mActivity!!
    }

    internal interface PromoRequester {
        fun requestPromo(policy: NetworkPolicy, mapObject: MapObject)
        @get:SponsoredType
        val sponsoredType: Int
    }

    internal class SightseeingsPromoRequester : PromoRequester {
        override fun requestPromo(policy: NetworkPolicy, mapObject: MapObject) {
            Promo.nativeRequestPoiGallery(
                policy, mapObject.lat, mapObject.lon,
                mapObject.rawTypes!!, UTM.UTM_SIGHTSEEINGS_PLACEPAGE_GALLERY
            )
        }

        override val sponsoredType: Int
            get() = Sponsored.TYPE_PROMO_CATALOG_SIGHTSEEINGS
    }

    internal class OutdoorPromoRequester : PromoRequester {
        override fun requestPromo(policy: NetworkPolicy, mapObject: MapObject) {
            Promo.nativeRequestPoiGallery(
                policy, mapObject.lat, mapObject.lon,
                mapObject.rawTypes!!, UTM.UTM_OUTDOOR_PLACEPAGE_GALLERY
            )
        }

        override val sponsoredType: Int
            get() = Sponsored.TYPE_PROMO_CATALOG_OUTDOOR
    }

    internal class CityPromoRequester : PromoRequester {
        override fun requestPromo(policy: NetworkPolicy, mapObject: MapObject) {
            Promo.nativeRequestCityGallery(
                policy, mapObject.lat, mapObject.lon,
                UTM.UTM_LARGE_TOPONYMS_PLACEPAGE_GALLERY
            )
        }

        override val sponsoredType: Int
            get() = Sponsored.TYPE_PROMO_CATALOG_CITY
    }

    internal interface PromoResponseHandler {
        fun handleResponse(promo: PromoCityGallery)
    }

    internal inner class SinglePromoResponseHandler(@field:SponsoredType private val mSponsoredType: Int) :
        PromoResponseHandler {

        override fun handleResponse(promo: PromoCityGallery) {
            val items =
                promo.items
            if (items.size <= 0) return
            UiUtils.show(
                mPlacePageView, R.id.catalog_promo_container,
                R.id.promo_poi_description_container, R.id.promo_poi_description_divider,
                R.id.promo_poi_card
            )
            UiUtils.hide(mRecycler)
            mTitle.setText(R.string.pp_discovery_place_related_header)
            val item = items[0]
            val poiImage =
                mPlacePageView.findViewById<ImageView>(R.id.promo_poi_image)
            Glide.with(poiImage.context)
                .load(item.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.img_guidespp_placeholder)
                .into(poiImage)
            val bookmarkName =
                mPlacePageView.findViewById<TextView>(R.id.place_single_bookmark_name)
            bookmarkName.text = item.name
            val subtitle =
                mPlacePageView.findViewById<TextView>(R.id.place_single_bookmark_subtitle)
            subtitle.text =
                if (TextUtils.isEmpty(item.tourCategory)) item.author.name else item.tourCategory
            val cta =
                mPlacePageView.findViewById<View>(R.id.place_single_bookmark_cta)
            val placement =
                getGalleryPlacement(mSponsoredType)
            cta.setOnClickListener { v: View? ->
                onCtaClicked(
                    placement,
                    item.url
                )
            }
            val place = item.place
            UiUtils.hide(mPlacePageView, R.id.poi_description_container)
            val poiName = mPlacePageView.findViewById<TextView>(R.id.promo_poi_name)
            poiName.text = place.name
            val poiDescription =
                mPlacePageView.findViewById<TextView>(R.id.promo_poi_description)
            poiDescription.text = Html.fromHtml(place.description)
            val more =
                mPlacePageView.findViewById<View>(R.id.promo_poi_more)
            more.setOnClickListener { v: View? ->
                onMoreDescriptionClicked(
                    item.url
                )
            }
            Statistics.INSTANCE.trackGalleryShown(
                GalleryType.PROMO,
                GalleryState.ONLINE,
                placement,
                1
            )
        }

        private fun onCtaClicked(placement: GalleryPlacement, url: String) {
            val utmContentUrl = BookmarkManager.INSTANCE.injectCatalogUTMContent(
                url,
                UTM.UTM_CONTENT_VIEW
            )
            startForResult(
                requireActivity(),
                BookmarkCategoriesActivity.REQ_CODE_DOWNLOAD_BOOKMARK_CATEGORY,
                utmContentUrl
            )
            Statistics.INSTANCE.trackGalleryProductItemSelected(
                GalleryType.PROMO, placement, 0,
                Destination.CATALOGUE
            )
        }

        private fun onMoreDescriptionClicked(url: String) {
            val utmContentUrl = BookmarkManager.INSTANCE.injectCatalogUTMContent(
                url,
                UTM.UTM_CONTENT_DESCRIPTION
            )
            startForResult(
                requireActivity(),
                BookmarkCategoriesActivity.REQ_CODE_DOWNLOAD_BOOKMARK_CATEGORY,
                utmContentUrl
            )
        }

    }

    internal inner class GalleryPromoResponseHandler(@field:SponsoredType private val mSponsoredType: Int) :
        PromoResponseHandler {

        override fun handleResponse(promo: PromoCityGallery) {
            UiUtils.show(
                mPlacePageView, R.id.catalog_promo_container,
                R.id.catalog_promo_title_divider, R.id.catalog_promo_recycler
            )
            UiUtils.hide(
                mPlacePageView, R.id.promo_poi_description_container,
                R.id.promo_poi_description_divider, R.id.promo_poi_card
            )
            val resources = mPlacePageView.resources
            val category = promo.category
            val showCategoryHeader = (!TextUtils.isEmpty(category)
                    && (mSponsoredType == Sponsored.TYPE_PROMO_CATALOG_SIGHTSEEINGS
                    || mSponsoredType == Sponsored.TYPE_PROMO_CATALOG_OUTDOOR))
            val galleryHeader: String
            galleryHeader = if (showCategoryHeader) {
                resources.getString(
                    R.string.pp_discovery_place_related_tag_header,
                    promo.category
                )
            } else {
                resources.getString(R.string.guides)
            }
            mTitle.text = galleryHeader
            val url = promo.moreUrl
            val placement =
                getGalleryPlacement(mSponsoredType)
            val promoListener = RegularCatalogPromoListener(
                requireActivity(),
                placement
            )
            val adapter =
                createCatalogPromoAdapter(
                    requireActivity(), promo, url,
                    promoListener, placement
                )
            mRecycler.adapter = adapter
        }

    }

    companion object {
        @SuppressLint("SwitchIntDef")
        private fun getGalleryPlacement(@SponsoredType type: Int): GalleryPlacement {
            return when (type) {
                Sponsored.TYPE_PROMO_CATALOG_CITY -> GalleryPlacement.PLACEPAGE_LARGE_TOPONYMS
                Sponsored.TYPE_PROMO_CATALOG_SIGHTSEEINGS -> GalleryPlacement.PLACEPAGE_SIGHTSEEINGS
                Sponsored.TYPE_PROMO_CATALOG_OUTDOOR -> GalleryPlacement.PLACEPAGE_OUTDOOR
                else -> throw AssertionError("Unsupported catalog gallery type '$type'!")
            }
        }

        @SuppressLint("SwitchIntDef")
        private fun createPromoRequester(@SponsoredType type: Int): PromoRequester? {
            return when (type) {
                Sponsored.TYPE_PROMO_CATALOG_SIGHTSEEINGS -> SightseeingsPromoRequester()
                Sponsored.TYPE_PROMO_CATALOG_CITY -> CityPromoRequester()
                Sponsored.TYPE_PROMO_CATALOG_OUTDOOR -> OutdoorPromoRequester()
                else -> null
            }
        }
    }

    init {
        mRecycler = mPlacePageView.findViewById(R.id.catalog_promo_recycler)
        mTitle = mPlacePageView.findViewById(R.id.catalog_promo_title)
        mRecycler.isNestedScrollingEnabled = false
        val layoutManager =
            LinearLayoutManager(
                mPlacePageView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        mRecycler.layoutManager = layoutManager
        val decor = ItemDecoratorFactory.createPlacePagePromoGalleryDecorator(
            mPlacePageView.context,
            LinearLayoutManager.HORIZONTAL
        )
        mRecycler.addItemDecoration(decor)
    }
}