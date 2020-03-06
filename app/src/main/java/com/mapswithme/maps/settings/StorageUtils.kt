package com.mapswithme.maps.settings

import android.annotation.TargetApi
import android.os.Build
import android.text.TextUtils
import com.mapswithme.maps.BuildConfig
import com.mapswithme.maps.Framework
import com.mapswithme.maps.MwmApplication
import com.mapswithme.util.Constants
import com.mapswithme.util.Utils
import com.mapswithme.util.log.LoggerFactory
import java.io.*
import java.nio.channels.FileChannel
import java.util.*

internal object StorageUtils {
    private val LOGGER =
        LoggerFactory.INSTANCE.getLogger(LoggerFactory.Type.STORAGE)
    private val TAG = StorageUtils::class.java.simpleName
    private const val VOLD_MODE = 1
    private const val MOUNTS_MODE = 2
    /**
     * Check if directory is writable. On some devices with KitKat (eg, Samsung S4) simple File.canWrite() returns
     * true for some actually read only directories on sdcard.
     * see https://code.google.com/p/android/issues/detail?id=66369 for details
     *
     * @param path path to ckeck
     * @return result
     */
    fun isDirWritable(path: String?): Boolean {
        val f = File(path, "mapsme_test_dir")
        f.mkdir()
        if (!f.exists()) return false
        f.delete()
        return true
    }

    /**
     * Returns path, where maps and other files are stored.
     * @return pat (or empty string, if framework wasn't created yet)
     */
    val writableDirRoot: String
        get() {
            var writableDir = Framework.nativeGetWritableDir()
            val index = writableDir?.lastIndexOf(Constants.MWM_DIR_POSTFIX)
            if (index != -1) writableDir = writableDir?.substring(0, index!!)
            return writableDir!!
        }

    fun getFreeBytesAtPath(path: String?): Long {
        var size: Long = 0
        try {
            size = File(path).freeSpace
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
        return size
    }

    // http://stackoverflow.com/questions/8151779/find-sd-card-volume-label-on-android
// http://stackoverflow.com/questions/5694933/find-an-external-sd-card-location
// http://stackoverflow.com/questions/14212969/file-canwrite-returns-false-on-some-devices-although-write-external-storage-pe
    private fun parseMountFile(
        file: String,
        mode: Int,
        paths: MutableSet<String>
    ) {
        LOGGER.i(
            StoragePathManager.Companion.TAG,
            "Parsing $file"
        )
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(FileReader(file))
            while (true) {
                var line = reader.readLine() ?: return
                line = line.trim { it <= ' ' }
                if (TextUtils.isEmpty(line) || line.startsWith("#")) continue
                // standard regexp for all possible whitespaces (space, tab, etc)
                val parts = line.split("\\s+").toTypedArray()
                if (parts.size <= 3) continue
                if (mode == VOLD_MODE) {
                    if (parts[0].startsWith("dev_mount")) paths.add(parts[2])
                    continue
                }
                for (s in arrayOf(
                    "/dev/block/vold",
                    "/dev/fuse",
                    "/mnt/media_rw"
                )) {
                    if (parts[0].startsWith(s)) {
                        paths.add(parts[1])
                        break
                    }
                }
            }
        } catch (e: IOException) {
            LOGGER.w(
                TAG,
                "Can't read file: $file",
                e
            )
        } finally {
            if (reader != null) Utils.closeSafely(reader)
        }
    }

    fun parseStorages(paths: MutableSet<String>) {
        parseMountFile(
            "/etc/vold.conf",
            VOLD_MODE,
            paths
        )
        parseMountFile(
            "/etc/vold.fstab",
            VOLD_MODE,
            paths
        )
        parseMountFile(
            "/system/etc/vold.fstab",
            VOLD_MODE,
            paths
        )
        parseMountFile(
            "/proc/mounts",
            MOUNTS_MODE,
            paths
        )
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun parseKitkatStorages(paths: MutableSet<String>) {
        val primaryStorage = MwmApplication.get().getExternalFilesDir(null)
        val storages = MwmApplication.get().getExternalFilesDirs(null)
        if (storages != null) {
            for (f in storages) { // add only secondary dirs
                if (f != null && f != primaryStorage) {
                    LOGGER.i(
                        StoragePathManager.Companion.TAG,
                        "Additional storage path: " + f.path
                    )
                    paths.add(f.path)
                }
            }
        }
        val testStorages: MutableSet<String> =
            HashSet()
        parseStorages(testStorages)
        val suffix = String.format(
            Constants.STORAGE_PATH,
            BuildConfig.APPLICATION_ID,
            Constants.FILES_DIR
        )
        for (testStorage in testStorages) {
            LOGGER.i(
                StoragePathManager.Companion.TAG,
                "Test storage from config files : $testStorage"
            )
            if (isDirWritable(testStorage)) {
                LOGGER.i(
                    StoragePathManager.Companion.TAG,
                    "Found writable storage : $testStorage"
                )
                paths.add(testStorage)
            } else {
                var testTmpStorage = testStorage
                testTmpStorage += suffix
                val file = File(testTmpStorage)
                if (!file.exists()) // create directory for our package if it isn't created by any reason
                {
                    LOGGER.i(
                        StoragePathManager.Companion.TAG,
                        "Try to create MWM path"
                    )
                    if (file.mkdirs()) LOGGER.i(
                        StoragePathManager.Companion.TAG,
                        "Created!"
                    )
                }
                if (isDirWritable(testTmpStorage)) {
                    LOGGER.i(
                        StoragePathManager.Companion.TAG,
                        "Found writable storage : $testTmpStorage"
                    )
                    paths.add(testTmpStorage)
                }
            }
        }
    }

    @Throws(IOException::class)
    fun copyFile(source: File?, dest: File?) {
        val maxChunkSize =
            10 * Constants.MB // move file by smaller chunks to avoid OOM.
        var inputChannel: FileChannel? = null
        var outputChannel: FileChannel? = null
        try {
            inputChannel = FileInputStream(source).channel
            outputChannel = FileOutputStream(dest).channel
            val totalSize = inputChannel.size()
            var currentPosition: Long = 0
            while (currentPosition < totalSize) {
                outputChannel.position(currentPosition)
                outputChannel.transferFrom(inputChannel, currentPosition, maxChunkSize.toLong())
                currentPosition += maxChunkSize.toLong()
            }
        } finally {
            Utils.closeSafely(inputChannel!!)
            Utils.closeSafely(outputChannel!!)
        }
    }

    private fun getDirSizeRecursively(
        file: File,
        fileFilter: FilenameFilter
    ): Long {
        if (file.isDirectory) {
            var dirSize: Long = 0
            for (child in file.listFiles()) dirSize += getDirSizeRecursively(
                child,
                fileFilter
            )
            return dirSize
        }
        return if (fileFilter.accept(file.parentFile, file.name)) file.length() else 0
    }

    val writableDirSize: Long
        get() {
            val writableDir = File(Framework.nativeGetWritableDir())
            if (BuildConfig.DEBUG) {
                check(writableDir.exists()) { "Writable directory doesn't exits, can't get size." }
                check(writableDir.isDirectory) { "Writable directory isn't a directory, can't get size." }
            }
            return getDirSizeRecursively(
                writableDir,
                StoragePathManager.Companion.MOVABLE_FILES_FILTER
            )
        }

    /**
     * Recursively lists all movable files in the directory.
     */
    fun listFilesRecursively(
        dir: File,
        prefix: String,
        filter: FilenameFilter,
        relPaths: ArrayList<String>
    ) {
        val list = dir.listFiles() ?: return
        for (file in list) {
            if (file.isDirectory) {
                listFilesRecursively(
                    file,
                    prefix + file.name + File.separator,
                    filter,
                    relPaths
                )
                continue
            }
            val name = file.name
            if (filter.accept(dir, name)) relPaths.add(prefix + name)
        }
    }

    private fun removeEmptyDirectories(dir: File) {
        for (file in dir.listFiles()) {
            if (!file.isDirectory) continue
            removeEmptyDirectories(file)
            file.delete()
        }
    }

    fun removeFilesInDirectory(
        dir: File,
        files: Array<File?>
    ): Boolean {
        return try {
            for (file in files) {
                file?.delete()
            }
            removeEmptyDirectories(dir)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}