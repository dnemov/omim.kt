package com.mapswithme.maps.location

import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.location.*
import com.mapswithme.maps.MwmApplication

internal class GoogleFusedLocationProvider(locationFixChecker: LocationFixChecker) :
    BaseLocationProvider(locationFixChecker), ConnectionCallbacks, OnConnectionFailedListener {
    private val mGoogleApiClient: GoogleApiClient
    private var mLocationRequest: LocationRequest? = null
    private var mLocationSettingsResult: PendingResult<LocationSettingsResult>? =
        null
    private val mListener: BaseLocationListener
    override fun start() {
        BaseLocationProvider.Companion.LOGGER.d(
            TAG,
            "Google fused provider is started"
        )
        if (mGoogleApiClient.isConnected || mGoogleApiClient.isConnecting) {
            isActive = true
            return
        }
        mLocationRequest = LocationRequest.create()
        mLocationRequest?.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        val interval = LocationHelper.INSTANCE.interval
        mLocationRequest?.setInterval(interval)
        BaseLocationProvider.Companion.LOGGER.d(
            TAG,
            "Request Google fused provider to provide locations at this interval = "
                    + interval + " ms"
        )
        mLocationRequest?.setFastestInterval(interval / 2)
        mGoogleApiClient.connect()
        isActive = true
    }

    override fun stop() {
        BaseLocationProvider.Companion.LOGGER.d(
            TAG,
            "Google fused provider is stopped"
        )
        if (mGoogleApiClient.isConnected) LocationServices.FusedLocationApi.removeLocationUpdates(
            mGoogleApiClient,
            mListener
        )
        if (mLocationSettingsResult != null && !mLocationSettingsResult!!.isCanceled) mLocationSettingsResult!!.cancel()
        mGoogleApiClient.disconnect()
        isActive = false
    }

    override fun onConnected(bundle: Bundle?) {
        BaseLocationProvider.Companion.LOGGER.d(
            TAG,
            "Fused onConnected. Bundle $bundle"
        )
        checkSettingsAndRequestUpdates()
    }

    private fun checkSettingsAndRequestUpdates() {
        BaseLocationProvider.Companion.LOGGER.d(
            TAG,
            "checkSettingsAndRequestUpdates()"
        )
        val builder =
            LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest!!)
        builder.setAlwaysShow(true) // hides 'never' button in resolve dialog afterwards.
        mLocationSettingsResult =
            LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build())
        mLocationSettingsResult?.setResultCallback(ResultCallback { locationSettingsResult ->
            val status =
                locationSettingsResult.status
            BaseLocationProvider.Companion.LOGGER.d(
                TAG,
                "onResult status: $status"
            )
            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> {
                }
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                    isActive = false
                    // Location settings are not satisfied. AndroidNativeProvider should be used.
                    resolveResolutionRequired()
                    return@ResultCallback
                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE ->  // Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.
                    isActive = false
            }
            requestLocationUpdates()
        })
    }

    // A permission is checked externally
    private fun requestLocationUpdates() {
        if (!mGoogleApiClient.isConnected) return
        LocationServices.FusedLocationApi.requestLocationUpdates(
            mGoogleApiClient,
            mLocationRequest,
            mListener
        )
        LocationHelper.INSTANCE.startSensors()
        val last =
            LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        if (last != null) mListener.onLocationChanged(last)
    }

    override fun onConnectionSuspended(i: Int) {
        isActive = false
        BaseLocationProvider.Companion.LOGGER.d(
            TAG,
            "Fused onConnectionSuspended. Code $i"
        )
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        isActive = false
        BaseLocationProvider.Companion.LOGGER.d(
            TAG,
            "Fused onConnectionFailed. Fall back to native provider. ConnResult $connectionResult"
        )
        // TODO handle error in a smarter way
        LocationHelper.INSTANCE.initNativeProvider()
        LocationHelper.INSTANCE.start()
    }

    companion object {
        private val TAG = GoogleFusedLocationProvider::class.java.simpleName
        private fun resolveResolutionRequired() {
            BaseLocationProvider.Companion.LOGGER.d(
                TAG,
                "resolveResolutionRequired()"
            )
            LocationHelper.INSTANCE.initNativeProvider()
            LocationHelper.INSTANCE.start()
        }
    }

    init {
        mGoogleApiClient = GoogleApiClient.Builder(MwmApplication.get())
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()
        mListener = BaseLocationListener(locationFixChecker)
    }
}