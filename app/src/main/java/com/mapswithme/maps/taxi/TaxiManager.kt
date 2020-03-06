package com.mapswithme.maps.taxi

import android.content.Context
import androidx.annotation.MainThread
import com.mapswithme.maps.bookmarks.data.MapObject
import com.mapswithme.maps.location.LocationHelper
import com.mapswithme.maps.routing.RoutingController.Companion.get
import com.mapswithme.util.NetworkPolicy
import com.mapswithme.util.SponsoredLinks
import com.mapswithme.util.Utils
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.statistics.Statistics
import java.util.*

@MainThread
class TaxiManager private constructor() {
    private val mProviders: MutableList<TaxiInfo> =
        ArrayList()
    private val mErrors: MutableList<TaxiInfoError> =
        ArrayList()
    private var mListener: TaxiListener? = null
    // Called from JNI.
    @MainThread
    fun onTaxiProvidersReceived(providers: Array<TaxiInfo>) {
        if (!UiThread.isUiThread) throw AssertionError("Must be called from UI thread!")
        if (providers.size == 0) {
            if (mListener != null) mListener!!.onNoTaxiProviders()
            return
        }
        mProviders.clear()
        mProviders.addAll(providers.asList())
        if (mListener != null) { // Taxi provider list must contain only one element until we implement taxi aggregator feature.
            mListener!!.onTaxiProviderReceived(mProviders[0])
        }
    }

    // Called from JNI.
    @MainThread
    fun onTaxiErrorsReceived(errors: Array<TaxiInfoError>) {
        if (!UiThread.isUiThread) throw AssertionError("Must be called from UI thread!")
        if (errors.size == 0) throw AssertionError("Taxi error array must be non-empty!")
        mErrors.clear()
        mErrors.addAll(errors.asList())
        if (mListener != null) { // Taxi error list must contain only one element until we implement taxi aggregator feature.
            mListener!!.onTaxiErrorReceived(mErrors[0])
        }
    }

    fun setTaxiListener(listener: TaxiListener?) {
        mListener = listener
    }

    enum class ErrorCode {
        NoProducts, RemoteError, NoProviders
    }

    interface TaxiListener {
        fun onTaxiProviderReceived(provider: TaxiInfo)
        fun onTaxiErrorReceived(error: TaxiInfoError)
        fun onNoTaxiProviders()
    }

    companion object {
        val INSTANCE = TaxiManager()

        @JvmStatic external fun nativeRequestTaxiProducts(
            policy: NetworkPolicy, srcLat: Double,
            srcLon: Double, dstLat: Double, dstLon: Double
        )

        @JvmStatic external fun nativeGetTaxiLinks(
            policy: NetworkPolicy, type: Int,
            productId: String, srcLon: Double,
            srcLat: Double, dstLat: Double, dstLon: Double
        ): SponsoredLinks

        fun getTaxiLink(
            productId: String, type: TaxiType,
            startPoint: MapObject?, endPoint: MapObject?
        ): SponsoredLinks? {
            return if (startPoint == null || endPoint == null) null else nativeGetTaxiLinks(
                NetworkPolicy.newInstance(true /* canUse */),
                type.ordinal, productId, startPoint.lat,
                startPoint.lon, endPoint.lat,
                endPoint.lon
            )
        }

        fun launchTaxiApp(
            context: Context, links: SponsoredLinks,
            type: TaxiType
        ) {
            Utils.openPartner(
                context,
                links,
                type.packageName,
                type.openMode
            )
            val isTaxiInstalled =
                Utils.isAppInstalled(context, type.packageName)
            trackTaxiStatistics(type.providerName, isTaxiInstalled)
        }

        private fun trackTaxiStatistics(
            taxiName: String,
            isTaxiAppInstalled: Boolean
        ) {
            val from = get().startPoint
            val to = get().endPoint
            val location = LocationHelper.INSTANCE.lastKnownLocation
            Statistics.INSTANCE.trackTaxiInRoutePlanning(
                from,
                to,
                location,
                taxiName,
                isTaxiAppInstalled
            )
        }
    }
}