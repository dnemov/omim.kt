package com.mapswithme.util

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import androidx.core.content.FileProvider
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.settings.StoragePathManager
import com.mapswithme.util.ConnectionState
import com.mapswithme.util.Graphics
import com.mapswithme.util.HttpClient

import com.mapswithme.util.Language
import com.mapswithme.util.log.Logger
import com.mapswithme.util.log.LoggerFactory
import java.io.File

object StorageUtils {
    private val LOGGER: Logger = LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.STORAGE)
    private val TAG = StorageUtils::class.java.simpleName
    private const val LOGS_FOLDER = "logs"
    /**
     * Checks if external storage is available for read and write
     *
     * @return true if external storage is mounted and ready for reading/writing
     */
    private fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    /**
     * Safely returns the external files directory path with the preliminary
     * checking the availability of the mentioned directory
     *
     * @return the absolute path of external files directory or null if directory can not be obtained
     * @see Context.getExternalFilesDir
     */
    private fun getExternalFilesDir(application: Application): String? {
        if (!isExternalStorageWritable()) return null
        val dir = application.getExternalFilesDir(null)
        if (dir != null) return dir.absolutePath
        Log.e(
            StorageUtils::class.java.simpleName,
            "Cannot get the external files directory for some reasons", Throwable()
        )
        return null
    }

    /**
     * Check existence of the folder for writing the logs. If that folder is absent this method will
     * try to create it and all missed parent folders.
     * @return true - if folder exists, otherwise - false
     */
    fun ensureLogsFolderExistence(application: Application): Boolean {
        val externalDir =
            getExternalFilesDir(application)
        if (TextUtils.isEmpty(externalDir)) return false
        val folder =
            File(externalDir + File.separator + LOGS_FOLDER)
        var success = true
        if (!folder.exists()) success = folder.mkdirs()
        return success
    }

    fun getLogsFolder(application: Application): String? {
        if (!ensureLogsFolderExistence(application)) return null
        val externalDir =
            getExternalFilesDir(application)
        return externalDir + File.separator + LOGS_FOLDER
    }

    fun getLogsZipPath(application: Application): String? {
        val zipFile =
            getExternalFilesDir(application) + File.separator + LOGS_FOLDER + ".zip"
        val file = File(zipFile)
        return if (file.isFile && file.exists()) zipFile else null
    }

    fun getApkPath(application: Application): String {
        return try {
            application.packageManager
                .getApplicationInfo(BuildConfig.APPLICATION_ID, 0).sourceDir
        } catch (e: NameNotFoundException) {
            LOGGER.e(
                TAG,
                "Can't get apk path from PackageManager",
                e
            )
            ""
        }
    }

    @JvmStatic
    fun getSettingsPath(): String {
        return Environment.getExternalStorageDirectory().absolutePath + Constants.MWM_DIR_POSTFIX
    }

    fun getStoragePath(settingsPath: String): String {
        var path: String = Config.getStoragePath()
        if (!TextUtils.isEmpty(path)) {
            val f = File(path)
            if (f.exists() && f.isDirectory) return path
            path = StoragePathManager().findMapsMeStorage(settingsPath)!!
            Config.setStoragePath(path)
            return path
        }
        return settingsPath
    }

    fun getFilesPath(application: Application): String {
        val filesDir = application.getExternalFilesDir(null)
        return if (filesDir != null) filesDir.absolutePath else Environment.getExternalStorageDirectory().absolutePath +
                java.lang.String.format(
                    Constants.STORAGE_PATH,
                    BuildConfig.APPLICATION_ID,
                    Constants.FILES_DIR
                )
    }

    fun getTempPath(application: Application): String {
        val cacheDir = application.externalCacheDir
        return if (cacheDir != null) cacheDir.absolutePath else Environment.getExternalStorageDirectory().absolutePath +
                java.lang.String.format(
                    Constants.STORAGE_PATH,
                    BuildConfig.APPLICATION_ID,
                    Constants.CACHE_DIR
                )
    }

    fun getObbGooglePath(): String {
        val storagePath =
            Environment.getExternalStorageDirectory().absolutePath
        return storagePath + java.lang.String.format(Constants.OBB_PATH, BuildConfig.APPLICATION_ID)
    }

    fun createDirectory(path: String): Boolean {
        val directory = File(path)
        if (!directory.exists() && !directory.mkdirs()) {
            val isPermissionGranted: Boolean = PermissionsUtils.isExternalStorageGranted()
            val error: Throwable = IllegalStateException(
                "Can't create directories for: " + path
                        + " state = " + Environment.getExternalStorageState()
                        + " isPermissionGranted = " + isPermissionGranted
            )
            LOGGER.e(
                TAG, "Can't create directories for: " + path
                        + " state = " + Environment.getExternalStorageState()
                        + " isPermissionGranted = " + isPermissionGranted
            )
            CrashlyticsUtils.logException(error)
            return false
        }
        return true
    }

    fun getFileSize(path: String): Long {
        val file = File(path)
        return file.length()
    }

    fun getUriForFilePath(context: Context, path: String): Uri {
        return FileProvider.getUriForFile(
            context.applicationContext,
            BuildConfig.FILE_PROVIDER_AUTHORITY, File(path)
        )
    }
}