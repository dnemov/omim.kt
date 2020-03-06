package com.mapswithme.maps.search

import androidx.annotation.MainThread
import com.mapswithme.maps.Framework
import com.mapswithme.maps.api.ParsedMwmRequest.Companion.currentRequest
import com.mapswithme.maps.api.ParsedMwmRequest.Companion.hasRequest
import com.mapswithme.maps.base.Initializable
import com.mapswithme.maps.bookmarks.data.FeatureId
import com.mapswithme.maps.search.HotelsFilter.HotelType
import com.mapswithme.util.Language
import com.mapswithme.util.Listeners
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.LoggerFactory
import java.io.UnsupportedEncodingException
import java.util.*

enum class SearchEngine : NativeSearchListener, NativeMapSearchListener,
    NativeBookmarkSearchListener, NativeBookingFilterListener, Initializable {
    INSTANCE;

    // Query, which results are shown on the map.
    var query: String? = null


    override fun onResultsUpdate(
        results: Array<SearchResult>?, timestamp: Long,
        isHotel: Boolean
    ) {
        UiThread.run {
            for (listener in mListeners) listener.onResultsUpdate(
                results,
                timestamp,
                isHotel
            )
            mListeners.finishIterate()
        }
    }

    override fun onResultsEnd(timestamp: Long) {
        UiThread.run {
            for (listener in mListeners) listener.onResultsEnd(timestamp)
            mListeners.finishIterate()
        }
    }

    override fun onMapSearchResults(
        results: Array<NativeMapSearchListener.Result>?,
        timestamp: Long,
        isLast: Boolean
    ) {
        UiThread.run {
            for (listener in mMapListeners) listener.onMapSearchResults(
                results,
                timestamp,
                isLast
            )
            mMapListeners.finishIterate()
        }
    }

    override fun onBookmarkSearchResultsUpdate(
        bookmarkIds: LongArray?,
        timestamp: Long
    ) {
        for (listener in mBookmarkListeners) listener.onBookmarkSearchResultsUpdate(
            bookmarkIds,
            timestamp
        )
        mBookmarkListeners.finishIterate()
    }

    override fun onBookmarkSearchResultsEnd(
        bookmarkIds: LongArray?,
        timestamp: Long
    ) {
        for (listener in mBookmarkListeners) listener.onBookmarkSearchResultsEnd(
            bookmarkIds,
            timestamp
        )
        mBookmarkListeners.finishIterate()
    }

    override fun onFilterHotels(
        @BookingFilter.Type type: Int, hotels: Array<FeatureId>?
    ) {
        UiThread.run {
            for (listener in mHotelListeners) listener.onFilterHotels(
                type,
                hotels
            )
            mHotelListeners.finishIterate()
        }
    }

    private val mListeners = Listeners<NativeSearchListener>()
    private val mMapListeners =
        Listeners<NativeMapSearchListener>()
    private val mBookmarkListeners =
        Listeners<NativeBookmarkSearchListener>()
    private val mHotelListeners =
        Listeners<NativeBookingFilterListener>()

    fun addListener(listener: NativeSearchListener) {
        mListeners.register(listener)
    }

    fun removeListener(listener: NativeSearchListener) {
        mListeners.unregister(listener)
    }

    fun addMapListener(listener: NativeMapSearchListener) {
        mMapListeners.register(listener)
    }

    fun removeMapListener(listener: NativeMapSearchListener) {
        mMapListeners.unregister(listener)
    }

    fun addBookmarkListener(listener: NativeBookmarkSearchListener) {
        mBookmarkListeners.register(listener)
    }

    fun removeBookmarkListener(listener: NativeBookmarkSearchListener) {
        mBookmarkListeners.unregister(listener)
    }

    fun addHotelListener(listener: NativeBookingFilterListener) {
        mHotelListeners.register(listener)
    }

    fun removeHotelListener(listener: NativeBookingFilterListener) {
        mHotelListeners.unregister(listener)
    }


    /**
     * @param timestamp Search results are filtered according to it after multiple requests.
     * @return whether search was actually started.
     */
    @MainThread
    fun search(
        query: String, timestamp: Long, hasLocation: Boolean,
        lat: Double, lon: Double, hotelsFilter: HotelsFilter?,
        bookingParams: BookingFilterParams?
    ): Boolean {
        try {
            return nativeRunSearch(
                query.toByteArray(charset("utf-8")),
                Language.keyboardLocale,
                timestamp,
                hasLocation,
                lat,
                lon,
                hotelsFilter,
                bookingParams
            )
        } catch (ignored: UnsupportedEncodingException) {
        }
        return false
    }

    @MainThread
    fun searchInteractive(
        query: String, locale: String, timestamp: Long,
        isMapAndTable: Boolean, hotelsFilter: HotelsFilter?,
        bookingParams: BookingFilterParams?
    ) {
        try {
            nativeRunInteractiveSearch(
                query.toByteArray(charset("utf-8")), locale, timestamp, isMapAndTable,
                hotelsFilter, bookingParams
            )
        } catch (ignored: UnsupportedEncodingException) {
        }
    }

    @MainThread
    fun searchInteractive(
        query: String, timestamp: Long, isMapAndTable: Boolean,
        hotelsFilter: HotelsFilter?, bookingParams: BookingFilterParams?
    ) {
        searchInteractive(
            query,
            Language.keyboardLocale,
            timestamp,
            isMapAndTable,
            hotelsFilter,
            bookingParams
        )
    }

    @MainThread
    fun searchInBookmarks(
        query: String,
        categoryId: Long,
        timestamp: Long
    ): Boolean {
        try {
            return nativeRunSearchInBookmarks(
                query.toByteArray(charset("utf-8")),
                categoryId,
                timestamp
            )
        } catch (ex: UnsupportedEncodingException) {
            LOGGER.w(
                TAG,
                "Unsupported encoding in bookmarks search.",
                ex
            )
        }
        return false
    }

    @MainThread
    fun cancel() {
        cancelApiCall()
        cancelAllSearches()
    }

    @MainThread
    fun cancelInteractiveSearch() {
        query = ""
        nativeCancelInteractiveSearch()
    }

    @MainThread
    private fun cancelAllSearches() {
        query = ""
        nativeCancelAllSearches()
    }

    @MainThread
    fun showResult(index: Int) {
        query = ""
        nativeShowResult(index)
    }

    override fun initialize() {
        nativeInit()
    }

    override fun destroy() { // Do nothing.
    }

    /**
     * @param bytes utf-8 formatted bytes of query.
     */
    external fun nativeRunSearch(
            bytes: ByteArray,
            language: String,
            timestamp: Long,
            hasLocation: Boolean,
            lat: Double,
            lon: Double,
            hotelsFilter: HotelsFilter?,
            bookingParams: BookingFilterParams?
    ): Boolean

    /**
     * @param bytes utf-8 formatted query bytes
     * @param bookingParams
     */
    external fun nativeRunInteractiveSearch(
            bytes: ByteArray, language: String, timestamp: Long,
            isMapAndTable: Boolean, hotelsFilter: HotelsFilter?,
            bookingParams: BookingFilterParams?
    )

    external fun nativeRunSearchInBookmarks(
            bytes: ByteArray,
            categoryId: Long,
            timestamp: Long
    ): Boolean

    external fun nativeShowResult(index: Int)
    external fun nativeCancelInteractiveSearch()
    external fun nativeCancelEverywhereSearch()
    external fun nativeCancelAllSearches()

    external fun nativeInit()


    @MainThread
    private fun cancelApiCall() {
        if (hasRequest()) currentRequest = null
        Framework.nativeClearApiPoints()
    }

    companion object {

        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
        private val TAG = SearchEngine::class.java.simpleName

        /**
         * @param bytes utf-8 formatted query bytes
         */
        @JvmStatic external fun nativeRunSearchMaps(
                bytes: ByteArray,
                language: String,
                timestamp: Long
        )

        /**
         * @return all existing hotel types
         */
        @JvmStatic external fun nativeGetHotelTypes(): Array<HotelType>

        @MainThread
        fun searchMaps(query: String, timestamp: Long) {
            try {
                nativeRunSearchMaps(
                        query.toByteArray(charset("utf-8")),
                        Language.keyboardLocale,
                        timestamp
                )
            } catch (ignored: UnsupportedEncodingException) {
            }
        }
    }
}