package com.mapswithme.util.log

import android.app.Application
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log

import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.MwmApplication
import com.mapswithme.maps.R
import com.mapswithme.util.StorageUtils
import net.jcip.annotations.GuardedBy
import net.jcip.annotations.ThreadSafe

import java.io.File
import java.util.EnumMap
import java.util.Objects
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ThreadSafe
class LoggerFactory private constructor() {
    @GuardedBy("this")
    private val mLoggers = EnumMap<Type, BaseLogger>(Type::class.java)
    @GuardedBy("this")
    private var mFileLoggerExecutor: ExecutorService? = null
    private var mApplication: Application? = null

    var isFileLoggingEnabled: Boolean
        get() {
            if (mApplication == null) {
//                check(!BuildConfig.DEBUG) { "Application is not created," + "but logger is used!" }
                return false
            }

            val prefs = MwmApplication.prefs(mApplication!!)
            val enableLoggingKey = mApplication!!.getString(R.string.pref_enable_logging)
            return prefs.getBoolean(enableLoggingKey, BuildConfig.BUILD_TYPE == "beta")
        }
        set(enabled) {
            Objects.requireNonNull<Application>(mApplication)
            nativeToggleCoreDebugLogs(enabled)
            val prefs = MwmApplication.prefs(mApplication!!)
            val editor = prefs.edit()
            val enableLoggingKey = mApplication!!.getString(R.string.pref_enable_logging)
            editor.putBoolean(enableLoggingKey, enabled).apply()
            updateLoggers()
        }

    private val fileLoggerExecutor: ExecutorService
        @Synchronized get() {
            if (mFileLoggerExecutor == null)
                mFileLoggerExecutor = Executors.newSingleThreadExecutor()
            return mFileLoggerExecutor!!
        }

    enum class Type {
        MISC, LOCATION, TRAFFIC, GPS_TRACKING, TRACK_RECORDER, ROUTING, NETWORK, STORAGE, DOWNLOADER,
        CORE, THIRD_PARTY, BILLING
    }

    interface OnZipCompletedListener {
        /**
         * Indicates about completion of zipping operation.
         *
         *
         * **NOTE:** called from the logger thread
         *
         * @param success indicates about a status of zipping operation
         */
        fun onCompleted(success: Boolean)
    }

    fun initialize(application: Application) {
        mApplication = application
    }

    @Synchronized
    fun getLogger(type: Type): Logger {
        var logger = mLoggers[type]
        if (logger == null) {
            logger = createLogger(type)
            mLoggers[type] = logger
        }
        return logger
    }

    @Synchronized
    private fun updateLoggers() {
        for (type in mLoggers.keys) {
            val logger = mLoggers[type]
            logger!!.setStrategy(createLoggerStrategy(type))
        }
    }

    @Synchronized
    fun zipLogs(listener: OnZipCompletedListener?) {
        if (mApplication == null)
            return

        val logsFolder = StorageUtils.getLogsFolder(mApplication!!)

        if (TextUtils.isEmpty(logsFolder)) {
            listener?.onCompleted(false)
            return
        }

        val task = ZipLogsTask(mApplication!!, logsFolder!!, "$logsFolder.zip", listener)
        fileLoggerExecutor.execute(task)
    }

    private fun createLogger(type: Type): BaseLogger {
        val strategy = createLoggerStrategy(type)
        return BaseLogger(strategy)
    }

    private fun createLoggerStrategy(type: Type): LoggerStrategy {
        if (isFileLoggingEnabled && mApplication != null) {
            nativeToggleCoreDebugLogs(true)
            val logsFolder = StorageUtils.getLogsFolder(mApplication!!)
            if (!TextUtils.isEmpty(logsFolder))
                return FileLoggerStrategy(
                    mApplication!!, logsFolder + File.separator
                            + type.name.toLowerCase() + ".log", fileLoggerExecutor
                )
        }

        return LogCatStrategy()
    }

    companion object {

        @JvmStatic
        val INSTANCE = LoggerFactory()
        private val CORE_TAG = "MapsmeCore"

        // Called from JNI.
        @JvmStatic
        fun logCoreMessage(level: Int, msg: String) {
            val logger = INSTANCE.getLogger(Type.CORE)
            when (level) {
                Log.DEBUG -> logger.d(CORE_TAG, msg)
                Log.INFO -> logger.i(CORE_TAG, msg)
                Log.WARN -> logger.w(CORE_TAG, msg)
                Log.ERROR -> logger.e(CORE_TAG, msg)
                else -> logger.v(CORE_TAG, msg)
            }
        }

        @JvmStatic external fun nativeToggleCoreDebugLogs(enabled: Boolean)
    }
}
