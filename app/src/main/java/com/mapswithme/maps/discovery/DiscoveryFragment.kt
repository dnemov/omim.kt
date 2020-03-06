package com.mapswithme.maps.discovery

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapswithme.maps.R
import com.mapswithme.maps.activity.CustomNavigateUpListener
import com.mapswithme.maps.base.BaseMwmToolbarFragment
import com.mapswithme.maps.bookmarks.data.FeatureId
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.bookmarks.data.MapObject.Companion.createMapObject
import com.mapswithme.maps.gallery.ItemSelectedListener
import com.mapswithme.maps.gallery.Items
import com.mapswithme.maps.gallery.Items.*
import com.mapswithme.maps.gallery.impl.Factory
import com.mapswithme.maps.gallery.impl.LoggableItemSelectedListener
import com.mapswithme.maps.gallery.impl.RegularCatalogPromoListener
import com.mapswithme.maps.metrics.UserActionsLogger
import com.mapswithme.maps.promo.PromoCityGallery
import com.mapswithme.maps.promo.PromoEntity
import com.mapswithme.maps.search.SearchResult
import com.mapswithme.maps.widget.PlaceholderView
import com.mapswithme.maps.widget.ToolbarController
import com.mapswithme.maps.widget.placepage.ErrorCatalogPromoListener
import com.mapswithme.maps.widget.recycler.ItemDecoratorFactory
import com.mapswithme.util.*
import com.mapswithme.util.NetworkPolicy.NetworkPolicyListener
import com.mapswithme.util.statistics.Destination
import com.mapswithme.util.statistics.GalleryPlacement
import com.mapswithme.util.statistics.GalleryType
import com.mapswithme.util.statistics.Statistics

class DiscoveryFragment : BaseMwmToolbarFragment(), DiscoveryResultReceiver {
    private var mOnlineMode = false
    private var mNavigateUpListener: CustomNavigateUpListener? = null
    private var mDiscoveryListener: DiscoveryListener? =
        null
    private val mNetworkStateReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                if (mOnlineMode) return
                if (ConnectionState.isConnected) NetworkPolicy.checkNetworkPolicy(
                    fragmentManager!!,
                    object : NetworkPolicyListener {
                        override fun onResult(policy: NetworkPolicy) {
                            onNetworkPolicyResult(policy)
                        }
                    }
                )
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CustomNavigateUpListener) mNavigateUpListener =
            context
        if (context is DiscoveryListener) mDiscoveryListener =
            context
    }

    override fun onDetach() {
        super.onDetach()
        mNavigateUpListener = null
        mDiscoveryListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_discovery, container, false)
        UserActionsLogger.logDiscoveryShownEvent()
        return root
    }

    private fun initHotelGallery() {
        setLayoutManagerAndItemDecoration(
            context!!,
            getGallery(R.id.hotels)
        )
    }

    private fun initAttractionsGallery() {
        setLayoutManagerAndItemDecoration(
            context!!,
            getGallery(R.id.attractions)
        )
    }

    private fun initFoodGallery() {
        setLayoutManagerAndItemDecoration(
            context!!,
            getGallery(R.id.food)
        )
    }

    private fun initCatalogPromoGallery() {
        val catalogPromoRecycler = getGallery(R.id.catalog_promo_recycler)
        setLayoutManagerAndItemDecoration(
            requireContext(),
            catalogPromoRecycler
        )
    }

    private fun getGallery(@IdRes id: Int): RecyclerView {
        val view = view ?: throw AssertionError("Root view is not initialized yet!")
        return view.findViewById(id)
            ?: throw AssertionError("RecyclerView must be within root view!")
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        activity!!.registerReceiver(mNetworkStateReceiver, filter)
        DiscoveryManager.INSTANCE.attach(this)
    }

    override fun onStop() {
        activity!!.unregisterReceiver(mNetworkStateReceiver)
        DiscoveryManager.INSTANCE.detach()
        super.onStop()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        toolbarController.setTitle(R.string.discovery_button_title)
        initHotelGallery()
        initAttractionsGallery()
        initFoodGallery()
        initSearchBasedAdapters()
        initCatalogPromoGallery()
        NetworkPolicy.checkNetworkPolicy(
            fragmentManager!!,
            object : NetworkPolicyListener {
                override fun onResult(policy: NetworkPolicy) {
                    onNetworkPolicyResult(policy)
                }
            }
        )
    }

    private fun onNetworkPolicyResult(policy: NetworkPolicy) {
        mOnlineMode = policy.canUseNetwork()
        initNetworkBasedAdapters()
        requestDiscoveryInfo()
    }

    private fun initSearchBasedAdapters() {
        getGallery(R.id.hotels).adapter = Factory.createSearchBasedLoadingAdapter()
        getGallery(R.id.attractions).adapter = Factory.createSearchBasedLoadingAdapter()
        getGallery(R.id.food).adapter = Factory.createSearchBasedLoadingAdapter()
    }

    private fun initNetworkBasedAdapters() {
        val promoRecycler = getGallery(R.id.catalog_promo_recycler)
        val listener: ItemSelectedListener<Item> =
            if (mOnlineMode) CatalogPromoSelectedListener(requireActivity()) else ErrorCatalogPromoListener(
                requireActivity(),
                object : NetworkPolicyListener {
                    override fun onResult(policy: NetworkPolicy) {
                        onNetworkPolicyResult(policy)
                    }
                }
            )
        val adapter =
            if (mOnlineMode) Factory.createCatalogPromoLoadingAdapter() else Factory.createCatalogPromoErrorAdapter(
                listener
            )
        promoRecycler.adapter = adapter
    }

    private fun requestDiscoveryInfo() {
        val params: DiscoveryParams = if (mOnlineMode) {
            DiscoveryParams(
                Utils.currencyCode,
                Language.defaultLocale,
                ITEMS_COUNT,
                *ITEM_TYPES
            )
        } else {
            DiscoveryParams(
                Utils.currencyCode,
                Language.defaultLocale,
                ITEMS_COUNT,
                DiscoveryParams.ITEM_TYPE_HOTELS,
                DiscoveryParams.ITEM_TYPE_ATTRACTIONS,
                DiscoveryParams.ITEM_TYPE_CAFES
            )
        }
        DiscoveryManager.INSTANCE.discover(params)
    }

    @MainThread
    override fun onAttractionsReceived(results: Array<SearchResult>) {
        updateViewsVisibility(
            results,
            R.id.attractionsTitle,
            R.id.attractions
        )
        val listener: ItemSelectedListener<SearchItem> = SearchBasedListener(
            this, GalleryType.SEARCH_ATTRACTIONS,
            ItemType.ATTRACTIONS
        )
        val gallery = getGallery(R.id.attractions)
        val adapter =
            Factory.createSearchBasedAdapter(
                results, listener, GalleryType.SEARCH_ATTRACTIONS, GalleryPlacement.DISCOVERY,
                MoreSearchItem()
            )
        gallery.adapter = adapter
    }

    @MainThread
    override fun onCafesReceived(results: Array<SearchResult>) {
        updateViewsVisibility(
            results,
            R.id.eatAndDrinkTitle,
            R.id.food
        )
        val listener: ItemSelectedListener<SearchItem> = SearchBasedListener(
            this, GalleryType.SEARCH_RESTAURANTS,
            ItemType.CAFES
        )
        val gallery = getGallery(R.id.food)
        gallery.adapter = Factory.createSearchBasedAdapter(
            results,
            listener,
            GalleryType.SEARCH_RESTAURANTS,
            GalleryPlacement.DISCOVERY,
            MoreSearchItem()
        )
    }

    override fun onHotelsReceived(results: Array<SearchResult>) {
        updateViewsVisibility(
            results,
            R.id.hotelsTitle,
            R.id.hotels
        )
        val listener: ItemSelectedListener<SearchItem> = HotelListener(this)
        val adapter =
            Factory.createHotelAdapter(
                results,
                listener,
                GalleryType.SEARCH_HOTELS,
                GalleryPlacement.DISCOVERY
            )
        val gallery = getGallery(R.id.hotels)
        gallery.adapter = adapter
    }

    @MainThread
    override fun onLocalExpertsReceived(experts: Array<LocalExpert>) {
        updateViewsVisibility(experts, R.id.localGuidesTitle, R.id.localGuides)
        val url: String = DiscoveryManager.nativeGetLocalExpertsUrl()
        val listener =
            createOnlineProductItemListener<LocalExpertItem>(
                GalleryType.LOCAL_EXPERTS,
                ItemType.LOCAL_EXPERTS
            )
        val gallery = getGallery(R.id.localGuides)
        val adapter =
            Factory.createLocalExpertsAdapter(
                experts,
                url,
                listener,
                GalleryPlacement.DISCOVERY
            )
        gallery.adapter = adapter
    }

    override fun onCatalogPromoResultReceived(gallery: PromoCityGallery) {
        updateViewsVisibility(
            gallery.items, R.id.catalog_promo_recycler,
            R.id.catalog_promo_title
        )
        if (gallery.items.size == 0) return
        val url = gallery.moreUrl
        val listener: ItemSelectedListener<PromoEntity> =
            RegularCatalogPromoListener(requireActivity(), GalleryPlacement.DISCOVERY)
        val adapter =
            Factory.createCatalogPromoAdapter(
                requireContext(), gallery, url,
                listener, GalleryPlacement.DISCOVERY
            )
        val recycler = getGallery(R.id.catalog_promo_recycler)
        recycler.adapter = adapter
    }

    override fun onError(type: ItemType) {
        when (type) {
            ItemType.HOTELS -> {
                getGallery(R.id.hotels).adapter = Factory.createSearchBasedErrorAdapter()
                Statistics.INSTANCE.trackGalleryError(
                    GalleryType.SEARCH_HOTELS,
                    GalleryPlacement.DISCOVERY,
                    null
                )
            }
            ItemType.ATTRACTIONS -> {
                getGallery(R.id.attractions).adapter = Factory.createSearchBasedErrorAdapter()
                Statistics.INSTANCE.trackGalleryError(
                    GalleryType.SEARCH_ATTRACTIONS,
                    GalleryPlacement.DISCOVERY,
                    null
                )
            }
            ItemType.CAFES -> {
                getGallery(R.id.food).adapter = Factory.createSearchBasedErrorAdapter()
                Statistics.INSTANCE.trackGalleryError(
                    GalleryType.SEARCH_RESTAURANTS,
                    GalleryPlacement.DISCOVERY,
                    null
                )
            }
            ItemType.LOCAL_EXPERTS -> {
                getGallery(R.id.localGuides).adapter = Factory.createLocalExpertsErrorAdapter()
                Statistics.INSTANCE.trackGalleryError(
                    GalleryType.LOCAL_EXPERTS,
                    GalleryPlacement.DISCOVERY,
                    null
                )
            }
            ItemType.PROMO -> {
                val adapter =
                    Factory.createCatalogPromoErrorAdapter(
                        ErrorCatalogPromoListener(
                            requireActivity(),
                            object : NetworkPolicyListener {
                                override fun onResult(policy: NetworkPolicy) {
                                    onNetworkPolicyResult(policy)
                                }
                            }
                        )
                    )
                val gallery = getGallery(R.id.catalog_promo_recycler)
                gallery.adapter = adapter
                Statistics.INSTANCE.trackGalleryError(
                    GalleryType.PROMO,
                    GalleryPlacement.DISCOVERY,
                    null
                )
            }
        }
    }

    override fun onNotFound() {
        val view = rootView
        UiUtils.hide(view, R.id.galleriesLayout)
        val placeholder =
            view.findViewById<View>(R.id.placeholder) as PlaceholderView
        placeholder.setContent(
            R.drawable.img_cactus, R.string.discovery_button_404_error_title,
            R.string.discovery_button_404_error_message
        )
        UiUtils.show(placeholder)
    }

    private fun <T> updateViewsVisibility(results: Array<T>, @IdRes vararg viewsId: Int) {
        for (@IdRes id in viewsId) UiUtils.showIf(
            results.size != 0,
            rootView.findViewById(id)
        )
    }

    val rootView: View
        get() = this.view
            ?: throw AssertionError("Don't call getRootView when view is not created yet!")

    private fun routeTo(item: SearchItem) {
        if (mDiscoveryListener == null) return
        mDiscoveryListener!!.onRouteToDiscoveredObject(createMapObject(item))
    }

    private fun showSimilarItems(
        item: SearchItem,
        type: ItemType
    ) {
        if (mDiscoveryListener != null) mDiscoveryListener!!.onShowSimilarObjects(item, type)
    }

    private fun showOnMap(item: SearchItem) {
        if (mDiscoveryListener != null) mDiscoveryListener!!.onShowDiscoveredObject(
            createMapObject(
                item
            )
        )
    }

    private fun showFilter() {
        if (mDiscoveryListener != null) mDiscoveryListener!!.onShowFilter()
    }

    private fun createMapObject(item: SearchItem): MapObject {
        val featureType = item.featureType
        val subtitle =
            Utils.getLocalizedFeatureType(context!!, featureType)
        val title =
            if (TextUtils.isEmpty(item.title)) subtitle else item.title
        return createMapObject(
            FeatureId.EMPTY, MapObject.SEARCH, title, subtitle,
            item.lat, item.lon
        )
    }

    override fun onCreateToolbarController(root: View): ToolbarController {
        return object : ToolbarController(rootView, activity!!) {
            override fun onUpClick() {
                if (mNavigateUpListener == null) return
                mNavigateUpListener!!.customOnNavigateUp()
            }
        }
    }

    private fun <I : Item?> createOnlineProductItemListener(
        galleryType: GalleryType,
        itemType: ItemType
    ): ItemSelectedListener<I> {
        return object : LoggableItemSelectedListener<I>(activity!!, itemType) {
            public override fun onItemSelectedInternal(item: I, position: Int) {
                Statistics.INSTANCE.trackGalleryProductItemSelected(
                    galleryType,
                    GalleryPlacement.DISCOVERY,
                    position,
                    Destination.EXTERNAL
                )
            }

            public override fun onMoreItemSelectedInternal(item: I) {
                Statistics.INSTANCE.trackGalleryEvent(
                    Statistics.EventName.PP_SPONSOR_MORE_SELECTED,
                    galleryType, GalleryPlacement.DISCOVERY
                )
            }
        }
    }

    private open class SearchBasedListener(
        val fragment: DiscoveryFragment,
        private val mType: GalleryType,
        itemType: ItemType
    ) : LoggableItemSelectedListener<SearchItem>(fragment.activity!!, itemType) {
        override fun openUrl(item: SearchItem) { /* Do nothing */
        }

        public override fun onMoreItemSelectedInternal(item: SearchItem) {
            fragment.showSimilarItems(item, type)
        }

        @CallSuper
        public override fun onItemSelectedInternal(item: SearchItem, position: Int) {
            fragment.showOnMap(item)
            Statistics.INSTANCE.trackGalleryProductItemSelected(
                mType,
                GalleryPlacement.DISCOVERY,
                position,
                Destination.PLACEPAGE
            )
        }

        @CallSuper
        override fun onActionButtonSelected(item: SearchItem, position: Int) {
            fragment.routeTo(item)
            Statistics.INSTANCE.trackGalleryProductItemSelected(
                mType,
                GalleryPlacement.DISCOVERY,
                position,
                Destination.ROUTING
            )
        }

    }

    private class HotelListener(fragment: DiscoveryFragment) :
        SearchBasedListener(
            fragment,
            GalleryType.SEARCH_HOTELS,
            ItemType.HOTELS
        ) {
        override fun onMoreItemSelectedInternal(item: SearchItem) {
            fragment.showFilter()
            Statistics.INSTANCE.trackGalleryEvent(
                Statistics.EventName.PP_SPONSOR_MORE_SELECTED,
                GalleryType.SEARCH_HOTELS,
                GalleryPlacement.DISCOVERY
            )
        }
    }

    interface DiscoveryListener {
        fun onRouteToDiscoveredObject(`object`: MapObject)
        fun onShowDiscoveredObject(`object`: MapObject)
        fun onShowFilter()
        fun onShowSimilarObjects(
            item: SearchItem,
            type: ItemType
        )
    }

    private class CatalogPromoSelectedListener(activity: Activity) :
        LoggableItemSelectedListener<Item>(
            activity,
            ItemType.PROMO
        ) {
        override fun onMoreItemSelectedInternal(item: Item) {}
        override fun onItemSelectedInternal(
            item: Item,
            position: Int
        ) {
        }
    }

    companion object {
        private const val ITEMS_COUNT = 5
        private val ITEM_TYPES = intArrayOf(
            DiscoveryParams.Companion.ITEM_TYPE_HOTELS,
            DiscoveryParams.Companion.ITEM_TYPE_ATTRACTIONS,
            DiscoveryParams.Companion.ITEM_TYPE_CAFES,
            DiscoveryParams.Companion.ITEM_TYPE_PROMO
        )

        private fun setLayoutManagerAndItemDecoration(
            context: Context,
            rv: RecyclerView
        ) {
            rv.layoutManager = LinearLayoutManager(
                context, LinearLayoutManager.HORIZONTAL,
                false
            )
            rv.addItemDecoration(
                ItemDecoratorFactory.createSponsoredGalleryDecorator(
                    context,
                    LinearLayoutManager.HORIZONTAL
                )
            )
        }
    }
}