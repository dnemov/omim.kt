package com.mapswithme.util.push

import com.google.android.gms.gcm.GoogleCloudMessaging
import com.google.android.gms.iid.InstanceID
import com.google.android.gms.iid.InstanceIDListenerService
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory
import java.io.IOException

class GcmInstanceIDRouterListenerService : InstanceIDListenerService() {
    override fun onTokenRefresh() {
        LOGGER.i(
            TAG,
            "onTokenRefresh()"
        )
        super.onTokenRefresh()
        try {
            onTokenRefreshInternal()
        } catch (e: IOException) {
            LOGGER.e(
                TAG,
                "Failed to obtained refreshed token: ",
                e
            )
        }
    }

    @Throws(IOException::class)
    private fun onTokenRefreshInternal() {
        val token = refreshedToken
    }

    @get:Throws(IOException::class)
    private val refreshedToken: String?
        private get() {
            val projectId: String = GCMListenerRouterService.getPWProjectId(this)!!
            val instanceID: InstanceID = InstanceID.getInstance(getApplicationContext())
            return instanceID.getToken(projectId, GoogleCloudMessaging.INSTANCE_ID_SCOPE)
        }

    companion object {
        private val LOGGER: Logger =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.THIRD_PARTY)
        private val TAG =
            GcmInstanceIDRouterListenerService::class.java.simpleName
    }
}