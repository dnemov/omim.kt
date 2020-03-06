package com.mapswithme.maps.gallery.impl

import android.content.Context
import com.mapswithme.maps.R
import com.mapswithme.maps.discovery.LocalExpert
import com.mapswithme.maps.gallery.Constants
import com.mapswithme.maps.gallery.GalleryAdapter
import com.mapswithme.maps.gallery.ItemSelectedListener
import com.mapswithme.maps.gallery.Items
import com.mapswithme.maps.gallery.Items.*
import com.mapswithme.maps.promo.PromoCityGallery
import com.mapswithme.maps.promo.PromoEntity
import com.mapswithme.maps.search.SearchResult
import com.mapswithme.maps.widget.placepage.PlacePageView
import com.mapswithme.util.statistics.GalleryPlacement
import com.mapswithme.util.statistics.GalleryState
import com.mapswithme.util.statistics.GalleryType
import com.mapswithme.util.statistics.Statistics

object Factory {
    fun createSearchBasedAdapter(
        results: Array<SearchResult>,
        listener: ItemSelectedListener<SearchItem>?,
        type: GalleryType,
        placement: GalleryPlacement,
        item: MoreSearchItem?
    ): GalleryAdapter<*, *> {
        trackProductGalleryShownOrError(
            results,
            type,
            GalleryState.OFFLINE,
            placement
        )
        return GalleryAdapter(
            SearchBasedAdapterStrategy(results, item, listener)
        )
    }

    fun createSearchBasedLoadingAdapter(): GalleryAdapter<*, *> {
        return GalleryAdapter(
            SimpleLoadingAdapterStrategy(null)
        )
    }

    fun createSearchBasedErrorAdapter(): GalleryAdapter<*, *> {
        return GalleryAdapter(
            SimpleErrorAdapterStrategy(null)
        )
    }

    fun createHotelAdapter(
        results: Array<SearchResult>,
        listener: ItemSelectedListener<SearchItem>?,
        type: GalleryType,
        placement: GalleryPlacement
    ): GalleryAdapter<*, *> {
        trackProductGalleryShownOrError(
            results,
            type,
            GalleryState.OFFLINE,
            placement
        )
        return GalleryAdapter(
            HotelAdapterStrategy(results, listener)
        )
    }

    fun createLocalExpertsAdapter(
        experts: Array<LocalExpert>,
        expertsUrl: String?,
        listener: ItemSelectedListener<LocalExpertItem>?,
        placement: GalleryPlacement
    ): GalleryAdapter<*, *> {
        trackProductGalleryShownOrError(
            experts,
            GalleryType.LOCAL_EXPERTS,
            GalleryState.ONLINE,
            placement
        )
        return GalleryAdapter(
            LocalExpertsAdapterStrategy(experts, expertsUrl, listener)
        )
    }

    fun createLocalExpertsLoadingAdapter(): GalleryAdapter<*, *> {
        return GalleryAdapter(
            LocalExpertsLoadingAdapterStrategy(null)
        )
    }

    fun createLocalExpertsErrorAdapter(): GalleryAdapter<*, *> {
        return GalleryAdapter(
            LocalExpertsErrorAdapterStrategy(null)
        )
    }

    @kotlin.jvm.JvmStatic
    fun createCatalogPromoAdapter(
        context: Context,
        gallery: PromoCityGallery,
        url: String?,
        listener: ItemSelectedListener<PromoEntity>?,
        placement: GalleryPlacement
    ): GalleryAdapter<*, *> {
        val item = PromoEntity(
            Constants.TYPE_MORE,
            context.getString(R.string.placepage_more_button),
            null, url, null, null
        )
        val entities = PlacePageView.toEntities(gallery).toMutableList()
        val strategy = CatalogPromoAdapterStrategy(
            entities,
            item,
            listener
        )
        trackProductGalleryShownOrError(
            gallery.items,
            GalleryType.PROMO,
            GalleryState.ONLINE,
            placement
        )
        return GalleryAdapter(
            strategy
        )
    }

    fun createCatalogPromoLoadingAdapter(): GalleryAdapter<*, *> {
        val strategy =
            CatalogPromoLoadingAdapterStrategy(null, null)
        return GalleryAdapter(
            strategy
        )
    }

    fun createCatalogPromoErrorAdapter(listener: ItemSelectedListener<Item>?): GalleryAdapter<*, *> {
        return GalleryAdapter(
            CatalogPromoErrorAdapterStrategy(listener)
        )
    }

    private fun <Product> trackProductGalleryShownOrError(
        products: Array<Product>,
        type: GalleryType,
        state: GalleryState,
        placement: GalleryPlacement
    ) {
        if (products.size == 0) Statistics.INSTANCE.trackGalleryError(
            type,
            placement,
            Statistics.ParamValue.NO_PRODUCTS
        ) else Statistics.INSTANCE.trackGalleryShown(
            type,
            state,
            placement,
            products.size
        )
    }
}