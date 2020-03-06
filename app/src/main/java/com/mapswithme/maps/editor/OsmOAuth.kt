package com.mapswithme.maps.editor

import androidx.annotation.IntDef
import androidx.annotation.Size
import androidx.annotation.WorkerThread
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.editor.data.UserStats
import java.lang.ref.WeakReference

public object OsmOAuth {
    const val OK = 0
    const val FAIL_COOKIE = 1
    const val FAIL_LOGIN = 2
    const val NO_O_AUTH = 3
    const val FAIL_AUTH = 4
    const val NO_ACCESS = 5
    const val NETWORK_ERROR = 6
    const val SERVER_ERROR = 7
    private const val PREF_OSM_TOKEN = "OsmToken"
    private const val PREF_OSM_SECRET = "OsmSecret"
    private const val PREF_OSM_USERNAME = "OsmUsername"
    private var sListener: WeakReference<OnUserStatsChanged>? = null
    fun setUserStatsListener(listener: OnUserStatsChanged) {
        sListener = WeakReference(listener)
    }

    // Called from native OsmOAuth.cpp.
    @JvmStatic
    fun onUserStatsUpdated(stats: UserStats?) {
        if (sListener == null) return
        val listener = sListener!!.get()
        listener?.onStatsChange(stats)
    }

    const val URL_PARAM_VERIFIER = "oauth_verifier"

    @JvmStatic
    val isAuthorized: Boolean
        get() = MwmApplication.prefs()?.contains(PREF_OSM_TOKEN) ?: false &&
                MwmApplication.prefs()?.contains(PREF_OSM_SECRET) ?: false

    val authToken: String?
        get() = MwmApplication.prefs()?.getString(PREF_OSM_TOKEN, "") ?: ""

    val authSecret: String?
        get() = MwmApplication.prefs()?.getString(PREF_OSM_SECRET, "") ?: ""

    val username: String?
        get() = MwmApplication.prefs()?.getString(PREF_OSM_USERNAME, "") ?: ""

    fun setAuthorization(
        token: String?,
        secret: String?,
        username: String?
    ) {
        MwmApplication.prefs()?.edit()
            ?.putString(PREF_OSM_TOKEN, token)
            ?.putString(PREF_OSM_SECRET, secret)
            ?.putString(PREF_OSM_USERNAME, username)
            ?.apply()
    }

    fun clearAuthorization() {
        MwmApplication.prefs()?.edit()
            ?.remove(PREF_OSM_TOKEN)
            ?.remove(PREF_OSM_SECRET)
            ?.remove(PREF_OSM_USERNAME)
            ?.apply()
    }

    /**
     * Some redirect urls indicates that user wasn't registered before.
     * Initial auth url should be reloaded to get correct [.URL_PARAM_VERIFIER] then.
     */
    fun shouldReloadWebviewUrl(url: String): Boolean {
        return url.contains("/welcome") || url.endsWith("/")
    }

    /**
     * @return array containing auth token and secret
     */
    @WorkerThread
    @Size(2)
    @JvmStatic external fun nativeAuthWithPassword(
        login: String?,
        password: String?
    ): Array<String>?

    /**
     * @return array containing auth token and secret
     */
    @WorkerThread
    @Size(2)
    @JvmStatic external fun nativeAuthWithWebviewToken(
        key: String?,
        secret: String?,
        verifier: String?
    ): Array<String>?

    /**
     * @return url for web auth, and token with secret for finishing authorization later
     */
    @Size(3)
    @JvmStatic external fun nativeGetFacebookAuthUrl(): Array<String>?

    /**
     * @return url for web auth, and token with secret for finishing authorization later
     */
    @Size(3)
    @JvmStatic external fun nativeGetGoogleAuthUrl(): Array<String>?

    @WorkerThread
    @JvmStatic external fun nativeGetOsmUsername(token: String?, secret: String?): String?

    @JvmStatic external fun nativeUpdateOsmUserStats(
        username: String?,
        forceUpdate: Boolean
    )

    enum class AuthType(val type: String) {
        OSM("OSM"), FACEBOOK("Facebook"), GOOGLE("Google");

    }

    // Result type corresponds to OsmOAuth::AuthResult.
    @IntDef(
        OK,
        FAIL_COOKIE,
        FAIL_LOGIN,
        NO_O_AUTH,
        FAIL_AUTH,
        NO_ACCESS,
        NETWORK_ERROR,
        SERVER_ERROR
    )
    annotation class AuthResult

    interface OnUserStatsChanged {
        fun onStatsChange(stats: UserStats?)
    }
}