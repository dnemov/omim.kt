package com.mapswithme.maps.settings

import android.app.Activity
import android.content.*
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Environment
import androidx.appcompat.app.AlertDialog
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.Framework
import com.mapswithme.maps.R
import com.mapswithme.maps.dialog.DialogUtils.createModalProgressDialog
import com.mapswithme.maps.dialog.DialogUtils.showAlertDialog
import com.mapswithme.maps.downloader.MapManager.nativeMoveFile
import com.mapswithme.maps.settings.StorageUtils.parseKitkatStorages
import com.mapswithme.maps.settings.StorageUtils.parseStorages
import com.mapswithme.maps.settings.StorageUtils.writableDirRoot
import com.mapswithme.util.Config
import com.mapswithme.util.concurrency.ThreadPool
import com.mapswithme.util.concurrency.UiThread
import com.mapswithme.util.log.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.io.IOException
import java.util.*

class StoragePathManager {
    interface MoveFilesListener {
        fun moveFilesFinished(newPath: String?)
        fun moveFilesFailed(errorCode: Int)
    }

    interface OnStorageListChangedListener {
        fun onStorageListChanged(
            storageItems: List<StorageItem>?,
            currentStorageIndex: Int
        )
    }

    private var mStoragesChangedListener: OnStorageListChangedListener? = null
    private var mMoveFilesListener: MoveFilesListener? = null
    private var mInternalReceiver: BroadcastReceiver? = null
    private var mActivity: Activity? = null
    private val mItems: MutableList<StorageItem> =
        ArrayList()
    var currentStorageIndex = -1
        private set

    /**
     * Observes status of connected media and retrieves list of available external storages.
     */
    fun startExternalStorageWatching(
        activity: Activity?,
        storagesChangedListener: OnStorageListChangedListener?,
        moveFilesListener: MoveFilesListener?
    ) {
        mActivity = activity
        mStoragesChangedListener = storagesChangedListener
        mMoveFilesListener = moveFilesListener
        mInternalReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                updateExternalStorages()
                if (mStoragesChangedListener != null) mStoragesChangedListener!!.onStorageListChanged(
                    mItems,
                    currentStorageIndex
                )
            }
        }
        mActivity!!.registerReceiver(
            mInternalReceiver,
            mediaChangesIntentFilter
        )
        updateExternalStorages()
    }

    fun stopExternalStorageWatching() {
        if (mInternalReceiver != null) {
            mActivity!!.unregisterReceiver(mInternalReceiver)
            mInternalReceiver = null
            mStoragesChangedListener = null
        }
    }

    fun hasMoreThanOneStorage(): Boolean {
        return mItems.size > 1
    }

    val storageItems: List<StorageItem>
        get() = mItems

    private fun updateExternalStorages() {
        updateExternalStorages(writableDirRoot)
    }

    private fun updateExternalStorages(writableDir: String) {
        val pathsFromConfig: MutableSet<String> =
            HashSet()
        if (SDK_INT >= KITKAT) parseKitkatStorages(
            pathsFromConfig
        ) else parseStorages(pathsFromConfig)
        mItems.clear()
        val currentStorage = buildStorageItem(writableDir)
        addStorageItem(currentStorage)
        addStorageItem(buildStorageItem(Environment.getExternalStorageDirectory().absolutePath))
        for (path in pathsFromConfig) addStorageItem(
            buildStorageItem(
                path
            )
        )
        currentStorageIndex = mItems.indexOf(currentStorage)
        if (currentStorageIndex == -1) {
            LOGGER.w(
                TAG,
                "Unrecognized current path : $currentStorage"
            )
            LOGGER.w(TAG, "Parsed paths : ")
            for (item in mItems) LOGGER.w(
                TAG,
                item.toString()
            )
        }
    }

    private fun addStorageItem(item: StorageItem?) {
        if (item != null && !mItems.contains(item)) mItems.add(item)
    }

    fun changeStorage(newIndex: Int) {
        val oldItem =
            if (currentStorageIndex != -1) mItems[currentStorageIndex] else null
        val item = mItems[newIndex]
        val path = item.fullPath
        val f = File(path)
        if (!f.exists() && !f.mkdirs()) {
            LOGGER.e(
                TAG,
                "Can't create directory: $path"
            )
            return
        }
        AlertDialog.Builder(mActivity!!)
            .setCancelable(false)
            .setTitle(R.string.move_maps)
            .setPositiveButton(R.string.ok) { dlg, which ->
                setStoragePath(mActivity!!, object : MoveFilesListener {
                    override fun moveFilesFinished(newPath: String?) {
                        updateExternalStorages()
                        if (mMoveFilesListener != null) mMoveFilesListener!!.moveFilesFinished(
                            newPath
                        )
                    }

                    override fun moveFilesFailed(errorCode: Int) {
                        updateExternalStorages()
                        if (mMoveFilesListener != null) mMoveFilesListener!!.moveFilesFailed(
                            errorCode
                        )
                    }
                }, item, oldItem, R.string.wait_several_minutes)
            }
            .setNegativeButton(R.string.cancel) { dlg, which -> dlg.dismiss() }.create().show()
    }

    /**
     * Checks whether current directory is actually writable on Kitkat devices. On earlier versions of android ( < 4.4 ) the root of external
     * storages was writable, but on Kitkat it isn't, so we should move our maps to other directory.
     * http://www.doubleencore.com/2014/03/android-external-storage/ check that link for explanations
     *
     *
     * TODO : use SAF framework to allow custom sdcard folder selections on Lollipop+ devices.
     * https://developer.android.com/guide/topics/providers/document-provider.html#client
     * https://code.google.com/p/android/issues/detail?id=103249
     */
    fun checkKitkatMigration(activity: Activity) {
        if (SDK_INT < KITKAT ||
            Config.isKitKatMigrationComplete()
        ) return
        checkExternalStoragePathOnKitkat(activity, object : MoveFilesListener {
            override fun moveFilesFinished(newPath: String?) {
                Config.setKitKatMigrationComplete()
                showAlertDialog(activity, R.string.kitkat_migrate_ok)
            }

            override fun moveFilesFailed(errorCode: Int) {
                showAlertDialog(activity, R.string.kitkat_migrate_failed)
            }
        })
    }

    fun findMapsMeStorage(settingsPath: String): String {
        updateExternalStorages(settingsPath)
        val items = storageItems
        for (item in items) {
            if (containsMapData(item.mPath)) return item.mPath!!
        }
        return settingsPath
    }

    private fun checkExternalStoragePathOnKitkat(
        context: Activity,
        listener: MoveFilesListener
    ) {
        val settingsDir = Framework.nativeGetSettingsDir()
        val writableDir = Framework.nativeGetWritableDir()
        if (settingsDir == writableDir || StorageUtils.isDirWritable(
                writableDir
            )
        ) return
        val size = StorageUtils.writableDirSize
        updateExternalStorages()
        for (item in mItems) {
            if (item.mFreeSize > size) {
                setStoragePath(
                    context,
                    listener,
                    item,
                    StorageItem(StorageUtils.writableDirRoot, 0),
                    R.string.kitkat_optimization_in_progress
                )
                return
            }
        }
        listener.moveFilesFailed(UNKNOWN_KITKAT_ERROR)
    }

    private fun setStoragePath(
        context: Activity,
        listener: MoveFilesListener,
        newStorage: StorageItem,
        oldStorage: StorageItem?, messageId: Int
    ) {
        val dialog = createModalProgressDialog(context, messageId)
        dialog.show()
        ThreadPool.storage
            .execute {
                val result =
                    changeStorage(newStorage, oldStorage)
                UiThread.run {
                    if (dialog.isShowing) dialog.dismiss()
                    if (result == NO_ERROR) listener.moveFilesFinished(
                        newStorage.mPath
                    ) else listener.moveFilesFailed(result)
                    updateExternalStorages()
                }
            }
    }

    companion object {
        private val LOGGER =
            LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.STORAGE)
        private val MOVABLE_EXTS =
            Framework.nativeGetMovableFilesExts()
        val MOVABLE_FILES_FILTER = FilenameFilter { dir, filename ->

            if (MOVABLE_EXTS.isNullOrEmpty()) false
            else {
                for (ext in MOVABLE_EXTS) if (filename.endsWith(
                        ext
                    )
                ) return@FilenameFilter true
                false
            }


        }
        const val NO_ERROR = 0
        const val UNKNOWN_LITE_PRO_ERROR = 1
        const val IOEXCEPTION_ERROR = 2
        const val NULL_ERROR = 4
        const val NOT_A_DIR_ERROR = 5
        const val UNKNOWN_KITKAT_ERROR = 6
        val TAG = StoragePathManager::class.java.name
        private val mediaChangesIntentFilter: IntentFilter
            private get() {
                val filter = IntentFilter()
                filter.addAction(Intent.ACTION_MEDIA_MOUNTED)
                filter.addAction(Intent.ACTION_MEDIA_REMOVED)
                filter.addAction(Intent.ACTION_MEDIA_EJECT)
                filter.addAction(Intent.ACTION_MEDIA_SHARED)
                filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
                filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE)
                filter.addAction(Intent.ACTION_MEDIA_CHECKING)
                filter.addAction(Intent.ACTION_MEDIA_NOFS)
                filter.addDataScheme(ContentResolver.SCHEME_FILE)
                return filter
            }

        private fun buildStorageItem(path: String?): StorageItem? {
            try {
                val f = File("$path/")
                if (f.exists() && f.isDirectory && f.canWrite() && StorageUtils.isDirWritable(
                        path
                    )
                ) {
                    val freeSize =
                        StorageUtils.getFreeBytesAtPath(path)
                    if (freeSize > 0) {
                        LOGGER.i(
                            TAG,
                            "Storage found : $path, size : $freeSize"
                        )
                        return StorageItem(path, freeSize)
                    }
                }
            } catch (ex: IllegalArgumentException) {
                LOGGER.e(
                    TAG,
                    "Can't build storage for path : $path",
                    ex
                )
            }
            return null
        }

        /**
         * Dumb way to determine whether the storage contains Maps.me data.
         *
         * The algorithm is quite simple:
         *
         *  * Find all writable storages;
         *  * For each storage list sub-dirs under "MapsWithMe" dir;
         *  * If there is a directory with version-like name (e.g. "160602")…
         *  * …and it is not empty…
         *  * …we got it!
         *
         */
        private fun containsMapData(storagePath: String?): Boolean {
            val path = File(storagePath)
            val candidates =
                path.listFiles(FileFilter { pathname ->
                    if (!pathname.isDirectory) return@FileFilter false
                    try {
                        val name = pathname.name
                        if (name.length != 6) return@FileFilter false
                        val version = Integer.valueOf(name)
                        return@FileFilter version > 120000 && version <= 999999
                    } catch (ignored: NumberFormatException) {
                    }
                    false
                })
            return candidates != null && candidates.size > 0 && candidates[0].list().size > 0
        }

        private fun changeStorage(newStorage: StorageItem, oldStorage: StorageItem?): Int {
            val fullNewPath = newStorage.fullPath
            // According to changeStorage code above, oldStorage can be null.
            if (oldStorage == null) {
                LOGGER.w(
                    TAG,
                    "Old storage path is null. New path is: $fullNewPath"
                )
                return NULL_ERROR
            }
            val oldDir = File(oldStorage.fullPath)
            val newDir = File(fullNewPath)
            if (!newDir.exists()) newDir.mkdir()
            if (BuildConfig.DEBUG) {
                check(newDir.isDirectory) { "Cannot move maps. New path is not a directory. New path : $newDir" }
                check(oldDir.isDirectory) { "Cannot move maps. Old path is not a directory. Old path : $oldDir" }
                check(StorageUtils.isDirWritable(fullNewPath)) { "Cannot move maps. New path is not writable. New path : $fullNewPath" }
            }
            val relPaths = ArrayList<String>()
            StorageUtils.listFilesRecursively(
                oldDir,
                "",
                MOVABLE_FILES_FILTER,
                relPaths
            )
            val oldFiles = arrayOfNulls<File>(relPaths.size)
            val newFiles = arrayOfNulls<File>(relPaths.size)
            for (i in relPaths.indices) {
                oldFiles[i] =
                    File(oldDir.absolutePath + File.separator + relPaths[i])
                newFiles[i] =
                    File(newDir.absolutePath + File.separator + relPaths[i])
            }
            try {
                for (i in oldFiles.indices) {
                    if (nativeMoveFile(
                            oldFiles[i]!!.absolutePath,
                            newFiles[i]!!.absolutePath
                        )
                    ) { // No need to delete oldFiles[i] because it was moved to newFiles[i].
                        oldFiles[i] = null
                    } else {
                        val parent = newFiles[i]!!.parentFile
                        parent?.mkdirs()
                        StorageUtils.copyFile(
                            oldFiles[i],
                            newFiles[i]
                        )
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                // In the case of failure delete all new files.  Old files will
// be lost if new files were just moved from old locations.
                StorageUtils.removeFilesInDirectory(newDir, newFiles)
                return IOEXCEPTION_ERROR
            }
            UiThread.run { Framework.nativeSetWritableDir(fullNewPath) }
            // Delete old files because new files were successfully created.
            StorageUtils.removeFilesInDirectory(oldDir, oldFiles)
            return NO_ERROR
        }
    }
}