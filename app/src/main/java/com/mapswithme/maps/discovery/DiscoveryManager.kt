package com.mapswithme.maps.discovery

import android.annotation.SuppressLint
import androidx.annotation.MainThread
import com.mapswithme.maps.promo.PromoCityGallery
import com.mapswithme.maps.search.SearchResult
import com.mapswithme.util.log.LoggerFactory
import java.util.*

enum class DiscoveryManager {
    INSTANCE;

    private var mCallback: DiscoveryResultReceiver? = null
    private var mRequestedTypesCount = 0
    fun discover(params: DiscoveryParams) {
        LOGGER.d(TAG, "discover: $params")
        AGGREGATE_EMPTY_RESULTS.clear()
        mRequestedTypesCount = params.mItemTypes.size
        nativeDiscover(params)
    }

    // Called from JNI.
    @SuppressLint("SwitchIntDef")
    @MainThread
    private fun onResultReceived(
        results: Array<SearchResult>,
        @DiscoveryParams.ItemType typeIndex: Int
    ) {
        if (typeIndex >= ItemType.values().size) {
            throw AssertionError(
                "Unsupported discovery item type " +
                        "'" + typeIndex + "' for search results!"
            )
        }
        val type =
            ItemType.values()[typeIndex]
        notifyUiWithCheck(
            results,
            type,
            object : Action {
                override fun run(callback: DiscoveryResultReceiver) {
                    onResultReceivedSafely(
                        callback,
                        type,
                        results
                    )
                }
            }
        )
    }

    private fun onResultReceivedSafely(
        callback: DiscoveryResultReceiver,
        type: ItemType,
        results: Array<SearchResult>
    ) {
        type.onResultReceived(callback, results)
    }

    // Called from JNI.
    @MainThread
    private fun onLocalExpertsReceived(experts: Array<LocalExpert>) {
        notifyUiWithCheck(experts,
            ItemType.LOCAL_EXPERTS,
            object : Action {
                override fun run(callback: DiscoveryResultReceiver) {
                    callback.onLocalExpertsReceived(
                        experts
                    )
                }
            }
        )
    }

    // Called from JNI.
    @MainThread
    private fun onPromoCityGalleryReceived(gallery: PromoCityGallery) {
        notifyUiWithCheck(gallery.items,
            ItemType.PROMO,
            object : Action {
                override fun run(callback: DiscoveryResultReceiver) {
                    callback.onCatalogPromoResultReceived(
                        gallery
                    )
                }
            }
        )
    }

    // Called from JNI.
    @MainThread
    private fun onError(@DiscoveryParams.ItemType type: Int) {
        LOGGER.w(
            TAG,
            "onError for type: $type"
        )
        if (mCallback != null) mCallback!!.onError(
            ItemType.values()[type]
        )
    }

    private fun <T> notifyUiWithCheck(
        results: Array<T>, type: ItemType,
        action: Action
    ) {
        LOGGER.d(
            TAG,
            "Results size = " + results.size + " for type: " + type
        )
        if (mCallback == null) return
        if (isAggregateResultsEmpty(results, type)) {
            mCallback!!.onNotFound()
            return
        }
        action.run(mCallback!!)
    }

    private fun <T> isAggregateResultsEmpty(
        results: Array<T>,
        type: ItemType
    ): Boolean {
        if (results.size == 0) AGGREGATE_EMPTY_RESULTS.add(type)
        return mRequestedTypesCount == AGGREGATE_EMPTY_RESULTS.size
    }

    fun attach(callback: DiscoveryResultReceiver) {
        LOGGER.d(
            TAG,
            "attach callback: $callback"
        )
        mCallback = callback
    }

    fun detach() {
        LOGGER.d(
            TAG,
            "detach callback: $mCallback"
        )
        mCallback = null
    }

    internal interface Action {
        fun run(callback: DiscoveryResultReceiver)
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = DiscoveryManager::class.java.simpleName
        private val AGGREGATE_EMPTY_RESULTS =
            EnumSet.noneOf(
                ItemType::class.java
            )

        @JvmStatic external fun nativeDiscover(params: DiscoveryParams)
        @JvmStatic external fun nativeGetLocalExpertsUrl(): String
    }
}