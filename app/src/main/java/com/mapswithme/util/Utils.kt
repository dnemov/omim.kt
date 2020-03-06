package com.mapswithme.util

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.util.AndroidRuntimeException
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.fragment.app.Fragment
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.maps.activity.CustomNavigateUpListener
import com.mapswithme.util.ConnectionState.isConnected
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory
import com.mapswithme.util.statistics.AlohaHelper
import java.io.Closeable
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.NetworkInterface
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.*

object Utils {
    @StringRes
    @JvmStatic val INVALID_ID = 0
    const val UTF_8 = "utf-8"
    const val TEXT_HTML = "text/html;"
    private val LOGGER: Logger = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.MISC)
    private const val TAG = "Utils"
    @JvmStatic val isLollipopOrLater: Boolean
        get() = isTargetOrLater(Build.VERSION_CODES.LOLLIPOP)

    @JvmStatic val isOreoOrLater: Boolean
        get() = isTargetOrLater(Build.VERSION_CODES.O)

    @JvmStatic val isMarshmallowOrLater: Boolean
        get() = isTargetOrLater(Build.VERSION_CODES.M)

    private fun isTargetOrLater(target: Int): Boolean {
        return Build.VERSION.SDK_INT >= target
    }

    @JvmStatic val isAmazonDevice: Boolean
        get() = "Amazon".equals(Build.MANUFACTURER, ignoreCase = true)

    /**
     * Enable to keep screen on.
     * Disable to let system turn it off automatically.
     */
    @JvmStatic fun keepScreenOn(enable: Boolean, w: Window) {
        if (enable) w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else w.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    @JvmStatic fun toastShortcut(context: Context?, message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    @JvmStatic fun toastShortcut(context: Context, messageResId: Int) {
        val message = context.getString(messageResId)
        toastShortcut(context, message)
    }

    @JvmStatic fun isIntentSupported(context: Context, intent: Intent?): Boolean {
        return context.packageManager.resolveActivity(intent, 0) != null
    }

    @JvmStatic fun checkNotNull(`object`: Any?) {
        if (null == `object`) throw NullPointerException("Argument here must not be NULL")
    }

    @JvmStatic fun copyTextToClipboard(context: Context, text: String) {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("maps.me: $text", text)
        clipboard.setPrimaryClip(clip)
    }

    @JvmStatic fun <K, V> mapPrettyPrint(map: Map<K, V>?): String {
        if (map == null) return "[null]"
        if (map.isEmpty()) return "[]"
        var joined = ""
        for (key in map.keys) {
            val keyVal = key.toString() + "=" + map[key]
            joined =
                if (joined.length > 0) TextUtils.join(",", arrayOf<Any>(joined, keyVal)) else keyVal
        }
        return "[$joined]"
    }

    @JvmStatic fun isPackageInstalled(packageUri: String?): Boolean {
        val pm: PackageManager = MwmApplication.get().getPackageManager()
        val installed: Boolean
        installed = try {
            pm.getPackageInfo(packageUri, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: NameNotFoundException) {
            false
        }
        return installed
    }

    @JvmStatic fun buildMailUri(
        to: String?,
        subject: String?,
        body: String?
    ): Uri {
        val uriString: String =
            Constants.Url.MAILTO_SCHEME.toString() + Uri.encode(to) +
                    Constants.Url.MAIL_SUBJECT + Uri.encode(subject) +
                    Constants.Url.MAIL_BODY + Uri.encode(body)
        return Uri.parse(uriString)
    }

    val fullDeviceModel: String
        get() {
            var model = Build.MODEL
            if (!model.startsWith(Build.MANUFACTURER)) model = Build.MANUFACTURER + " " + model
            return model
        }

    @JvmStatic fun openAppInMarket(activity: Activity?, url: String?) {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT) else marketIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
        )
        try {
            activity?.startActivity(marketIntent)
        } catch (e: ActivityNotFoundException) {
            AlohaHelper.logException(e)
        }
    }

    @JvmStatic fun showFacebookPage(activity: Activity) {
        try { // Exception is thrown if we don't have installed Facebook application.
            activity.packageManager.getPackageInfo(Constants.Package.FB_PACKAGE, 0)
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(Constants.Url.FB_MAPSME_COMMUNITY_NATIVE)
                )
            )
        } catch (e: Exception) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(Constants.Url.FB_MAPSME_COMMUNITY_HTTP)
                )
            )
        }
    }

    @JvmStatic fun showTwitterPage(activity: Activity) {
        activity.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(Constants.Url.TWITTER_MAPSME_HTTP)
            )
        )
    }

    @JvmStatic fun openUrl(context: Context, url: String?) {
        if (TextUtils.isEmpty(url)) return
        val intent = Intent(Intent.ACTION_VIEW)
        val uri =
            if (isHttpOrHttpsScheme(url!!)) Uri.parse(url) else Uri.Builder().scheme(
                "http"
            ).appendEncodedPath(url).build()
        intent.data = uri
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            CrashlyticsUtils.logException(e)
        } catch (e: AndroidRuntimeException) {
            CrashlyticsUtils.logException(e)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    private fun isHttpOrHttpsScheme(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    @JvmStatic fun <T> castTo(instance: Any): T { // Noinspection unchecked
        return instance as T
    }

    @JvmStatic fun closeSafely(vararg closeable: Closeable) {
        for (each in closeable) {
            if (each != null) {
                try {
                    each.close()
                } catch (e: IOException) {
                    LOGGER.e(
                        TAG,
                        "Failed to close '$each'",
                        e
                    )
                }
            }
        }
    }

    @JvmStatic fun sendBugReport(activity: Activity, subject: String) {
        LoggerFactory.INSTANCE.zipLogs(
            SupportInfoWithLogsCallback(
                activity, subject,
                Constants.Email.SUPPORT
            )
        )
    }

    @JvmStatic fun sendFeedback(activity: Activity) {
        LoggerFactory.INSTANCE.zipLogs(
            SupportInfoWithLogsCallback(
                activity, "Feedback",
                Constants.Email.FEEDBACK
            )
        )
    }

    @JvmStatic fun navigateToParent(activity: Activity?) {
        if (activity == null) return
        if (activity is CustomNavigateUpListener) (activity as CustomNavigateUpListener).customOnNavigateUp() else NavUtils.navigateUpFromSameTask(
            activity
        )
    }

    @JvmStatic fun formatUnitsText(
        context: Context?, @DimenRes size: Int, @DimenRes units: Int,
        dimension: String,
        unitText: String?
    ): SpannableStringBuilder {
        val res =
            SpannableStringBuilder(dimension).append(" ").append(unitText)
        res.setSpan(
            AbsoluteSizeSpan(UiUtils.dimen(context!!, size), false),
            0,
            dimension.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        res.setSpan(
            AbsoluteSizeSpan(UiUtils.dimen(context, units), false),
            dimension.length,
            res.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return res
    }

    @JvmStatic fun checkConnection(
        context: Context?, @StringRes message: Int,
        onCheckPassedCallback: Proc<Boolean>
    ) {
        if (isConnected) {
            onCheckPassedCallback.invoke(true)
            return
        }
        class Holder {
            var accepted = false
        }

        val holder = Holder()
        AlertDialog.Builder(context!!)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(
                R.string.downloader_retry
            ) { dialog, which ->
                holder.accepted = true
                checkConnection(context, message, onCheckPassedCallback)
            }.setOnDismissListener { if (!holder.accepted) onCheckPassedCallback.invoke(false) }
            .show()
    }

    // "UNIQUE_ID" is the value of org.alohalytics.Statistics.PREF_UNIQUE_ID, but it private.
    @JvmStatic
    val installationId: String?
        get() {
            val context: Context = MwmApplication.get()
            val sharedPrefs = context.getSharedPreferences(
                org.alohalytics.Statistics.PREF_FILE, Context.MODE_PRIVATE
            )
            // "UNIQUE_ID" is the value of org.alohalytics.Statistics.PREF_UNIQUE_ID, but it private.
            val installationId = sharedPrefs.getString("UNIQUE_ID", null)
            return if (TextUtils.isEmpty(installationId)) "" else installationId
        }

    @JvmStatic fun getMacAddress(md5Decoded: Boolean): String {
        val context: Context = MwmApplication.get()
        var macBytes: ByteArray? = null
        var address = ""
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                val manager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        ?: return ""
                val info = manager.connectionInfo
                address = info.macAddress
                macBytes = address.toByteArray()
            } else {
                val all: List<NetworkInterface> =
                    Collections.list(NetworkInterface.getNetworkInterfaces())
                for (nif in all) {
                    if (!nif.name.equals("wlan0", ignoreCase = true)) continue
                    macBytes = nif.hardwareAddress
                    if (macBytes == null) return ""
                    val result = StringBuilder()
                    for (i in macBytes.indices) {
                        result.append(String.format("%02X", 0xFF and macBytes[i].toInt()))
                        if (i + 1 != macBytes.size) result.append(":")
                    }
                    address = result.toString()
                }
            }
        } catch (exc: Exception) {
            return ""
        }
        return if (md5Decoded) decodeMD5(macBytes) else address
    }

    private fun decodeMD5(bytes: ByteArray?): String {
        return if (bytes == null || bytes.size == 0) "" else try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(bytes)
            val messageDigest = digest.digest()
            val hexString = StringBuilder()
            for (i in messageDigest.indices) hexString.append(
                String.format(
                    "%02X",
                    0xFF and messageDigest[i].toInt()
                )
            )
            hexString.toString()
        } catch (e: Exception) {
            ""
        }
    }

    @JvmStatic fun isAppInstalled(
        context: Context,
        packageName: String
    ): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: NameNotFoundException) {
            false
        }
    }

    private fun launchAppDirectly(
        context: Context,
        links: SponsoredLinks
    ) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = Uri.parse(links.getDeepLink())
        context.startActivity(intent)
    }

    private fun launchAppIndirectly(
        context: Context,
        links: SponsoredLinks
    ) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(links.getDeepLink())
        context.startActivity(intent)
    }

    @JvmStatic fun openPartner(
        activity: Context, links: SponsoredLinks,
        packageName: String, openMode: PartnerAppOpenMode
    ) {
        when (openMode) {
            PartnerAppOpenMode.Direct -> {
                if (!isAppInstalled(activity, packageName)) {
                    openUrl(activity, links.getUniversalLink())
                    return
                }
                launchAppDirectly(activity, links)
            }
            PartnerAppOpenMode.Indirect -> launchAppIndirectly(
                activity,
                links
            )
            else -> throw AssertionError(
                "Unsupported partner app open mode: " + openMode +
                        "; Package name: " + packageName
            )
        }
    }

    @JvmStatic fun sendTo(context: Context, email: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = buildMailUri(email, "", "")
        context.startActivity(intent)
    }

    @JvmStatic fun callPhone(context: Context, phone: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phone")
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            LOGGER.e(
                TAG,
                "Failed to call phone",
                e
            )
            AlohaHelper.logException(e)
        }
    }

    @JvmStatic fun showSystemSettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            LOGGER.e(
                TAG,
                "Failed to open system settings",
                e
            )
        }
    }

    @JvmStatic val currencyCode: String?
        get() {
            val locales =
                arrayOf(Locale.getDefault(), Locale.US)
            for (locale in locales) {
                val currency =
                    getCurrencyForLocale(locale)
                if (currency != null) return currency.currencyCode
            }
            return null
        }

    @JvmStatic fun getCurrencyForLocale(locale: Locale): Currency? {
        return try {
            Currency.getInstance(locale)
        } catch (e: Throwable) {
            LOGGER.e(
                TAG,
                "Failed to obtain a currency for locale: $locale",
                e
            )
            null
        }
    }

    @JvmStatic fun formatCurrencyString(price: String, currencyCode: String): String {
        val value = java.lang.Float.valueOf(price)
        return formatCurrencyString(value, currencyCode)
    }

    @JvmStatic fun formatCurrencyString(price: Float, currencyCode: String): String {
        val text: String
        try {
            var locale = Locale.getDefault()
            val currency =
                getCurrencyForLocale(locale)
            // If the currency cannot be obtained for the default locale we will use Locale.US.
            if (currency == null) locale = Locale.US
            val formatter =
                NumberFormat.getCurrencyInstance(locale)
            if (!TextUtils.isEmpty(currencyCode)) formatter.currency = Currency.getInstance(
                currencyCode
            )
            return formatter.format(price.toDouble())
        } catch (e: Throwable) {
            LOGGER.e(
                TAG, "Failed to format string for price = " + price
                        + " and currencyCode = " + currencyCode, e
            )
            text = "$price $currencyCode"
        }
        return text
    }

    @JvmStatic fun makeUrlSafe(url: String): String {
        return url.replace("(token|password|key)=([^&]+)".toRegex(), "***")
    }

    @StringRes
    @JvmStatic fun getStringIdByKey(context: Context, key: String): Int {
        try {
            val res = context.resources
            @StringRes val nameId = res.getIdentifier(key, "string", context.packageName)
            if (nameId == INVALID_ID || nameId == View.NO_ID) throw NotFoundException(
                "String id '$key' is not found"
            )
            return nameId
        } catch (e: RuntimeException) {
            LOGGER.e(
                TAG,
                "Failed to get string with id '$key'",
                e
            )
            if (BuildConfig.BUILD_TYPE.equals("debug") || BuildConfig.BUILD_TYPE.equals("beta")) {
                Toast.makeText(
                    context, "Add string id for '$key'!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        return INVALID_ID
    }

    /**
     * Returns a string value for the specified key. If the value is not found then its key will be
     * returned.
     *
     * @return string value or its key if there is no string for the specified key.
     */
    @JvmStatic fun getStringValueByKey(context: Context, key: String): String {
        @StringRes val id = getStringIdByKey(context, key)
        try {
            return context.getString(id)
        } catch (e: NotFoundException) {
            LOGGER.e(
                TAG,
                "Failed to get value for string '$key'",
                e
            )
        }
        return key
    }

    @JvmStatic val deviceName: String
        get() = Build.MANUFACTURER

    @JvmStatic val deviceModel: String
        get() = Build.MODEL

    @JvmStatic val isJellyBeanOrLater: Boolean
        get() = isTargetOrLater(Build.VERSION_CODES.JELLY_BEAN_MR1)

    @JvmStatic fun <T> concatArrays(a: Array<T>?, vararg b: T): Array<T> {
        if (a == null || a.size == 0) return b as Array<T>
        if (b == null || b.size == 0) return a
        val c = Arrays.copyOf(a, a.size + b.size)
        System.arraycopy(b, 0, c, a.size, b.size)
        return c
    }

    @JvmStatic fun detachFragmentIfCoreNotInitialized(
        context: Context,
        fragment: Fragment
    ) {
        if (context is AppCompatActivity && !MwmApplication.get().arePlatformAndCoreInitialized()) {
            (context as AppCompatActivity).getSupportFragmentManager()
                .beginTransaction()
                .detach(fragment)
                .commit()
        }
    }

    @JvmStatic fun capitalize(src: String?): String? {
        if (TextUtils.isEmpty(src)) return src
        return if (src!!.length == 1) Character.toString(
            Character.toUpperCase(
                src[0]
            )
        ) else Character.toUpperCase(src[0]).toString() + src.substring(1)
    }

    @JvmStatic fun unCapitalize(src: String?): String? {
        if (TextUtils.isEmpty(src)) return src
        return if (src!!.length == 1) Character.toString(
            Character.toLowerCase(
                src[0]
            )
        ) else Character.toLowerCase(src[0]).toString() + src.substring(1)
    }

    private fun getLocalizedFeatureByKey(
        context: Context,
        key: String
    ): String {
        @StringRes val id = getStringIdByKey(context, key)
        try {
            return context.getString(id)
        } catch (e: NotFoundException) {
            LOGGER.e(
                TAG,
                "Failed to get localized string for key '$key'",
                e
            )
        }
        return key
    }

    @JvmStatic fun getLocalizedFeatureType(
        context: Context,
        type: String?
    ): String {
        if (TextUtils.isEmpty(type)) return ""
        val key = "type." + type!!.replace('-', '.')
            .replace(':', '_')
        return getLocalizedFeatureByKey(context, key)
    }

    @JvmStatic fun getLocalizedBrand(context: Context, brand: String?): String {
        if (TextUtils.isEmpty(brand)) return ""
        val key = "brand.$brand"
        return getLocalizedFeatureByKey(context, key)
    }

    enum class PartnerAppOpenMode {
        None, Direct, Indirect
    }

    interface Proc<T> {
        operator fun invoke(param: T)
    }

    private class SupportInfoWithLogsCallback(
        activity: Activity, subject: String,
        email: String
    ) : LoggerFactory.OnZipCompletedListener {
        private val mActivityRef: WeakReference<Activity>
        private val mSubject: String
        private val mEmail: String
        override fun onCompleted(success: Boolean) {
            UiThread.run {
                val activity = mActivityRef.get() ?: return@run
                val intent = Intent(Intent.ACTION_SEND)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(mEmail))
                intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "[" + BuildConfig.VERSION_NAME.toString() + "] " + mSubject
                )
                if (success) {
                    val logsZipFile: String =
                        StorageUtils.getLogsZipPath(activity.application)!!
                    if (!TextUtils.isEmpty(logsZipFile)) {
                        val uri: Uri =
                            StorageUtils.getUriForFilePath(activity, logsZipFile)
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                    }
                }
                // Do this so some email clients don't complain about empty body.
                intent.putExtra(Intent.EXTRA_TEXT, "")
                intent.type = "message/rfc822"
                try {
                    activity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    CrashlyticsUtils.logException(e)
                }
            }
        }

        init {
            mActivityRef = WeakReference(activity)
            mSubject = subject
            mEmail = email
        }
    }
}