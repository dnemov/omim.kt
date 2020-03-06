package com.mapswithme.util.log

import android.app.Application
import android.util.Log
import com.mapswithme.util.StorageUtils.getLogsFolder
import com.mapswithme.util.Utils.closeSafely
import com.mapswithme.util.log.FileLoggerStrategy
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class ZipLogsTask(
    private val mApplication: Application, private val mSourcePath: String,
    private val mDestPath: String,
    onCompletedListener: LoggerFactory.OnZipCompletedListener?
) : Runnable {
    private val mOnCompletedListener: LoggerFactory.OnZipCompletedListener?
    override fun run() {
        saveSystemLogcat()
        val success = zipFileAtPath(mSourcePath, mDestPath)
        if (mOnCompletedListener != null) mOnCompletedListener.onCompleted(success)
    }

    private fun zipFileAtPath(
        sourcePath: String,
        toLocation: String
    ): Boolean {
        val sourceFile = File(sourcePath)
        try {
            FileOutputStream(toLocation, false).use { dest ->
                ZipOutputStream(BufferedOutputStream(dest)).use { out ->
                    if (sourceFile.isDirectory) {
                        zipSubFolder(out, sourceFile, sourceFile.parent.length)
                    } else {
                        try {
                            FileInputStream(sourcePath).use { fi ->
                                BufferedInputStream(
                                    fi,
                                    BUFFER_SIZE
                                ).use { origin ->
                                    val entry = ZipEntry(
                                        getLastPathComponent(
                                            sourcePath
                                        )
                                    )
                                    out.putNextEntry(entry)
                                    val data =
                                        ByteArray(BUFFER_SIZE)
                                    var count: Int
                                    while (origin.read(
                                            data,
                                            0,
                                            BUFFER_SIZE
                                        ).also { count = it } != -1
                                    ) {
                                        out.write(data, 0, count)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Failed to read zip file entry '" + sourcePath + "' to location '"
                                        + toLocation + "'",
                                e
                            )
                            return false
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to zip file '$sourcePath' to location '$toLocation'",
                e
            )
            return false
        }
        return true
    }

    @Throws(IOException::class)
    private fun zipSubFolder(
        out: ZipOutputStream, folder: File,
        basePathLength: Int
    ) {
        val fileList = folder.listFiles()
        var origin: BufferedInputStream
        for (file in fileList) {
            if (file.isDirectory) {
                zipSubFolder(out, file, basePathLength)
            } else {
                val data =
                    ByteArray(BUFFER_SIZE)
                val unmodifiedFilePath = file.path
                val relativePath = unmodifiedFilePath
                    .substring(basePathLength)
                val fi = FileInputStream(unmodifiedFilePath)
                origin = BufferedInputStream(
                    fi,
                    BUFFER_SIZE
                )
                val entry = ZipEntry(relativePath)
                out.putNextEntry(entry)
                var count: Int
                while (origin.read(
                        data,
                        0,
                        BUFFER_SIZE
                    ).also { count = it } != -1
                ) {
                    out.write(data, 0, count)
                }
                origin.close()
            }
        }
    }

    private fun saveSystemLogcat() {
        val fullName =
            getLogsFolder(mApplication) + File.separator + "logcat.log"
        val file = File(fullName)
        var reader: InputStreamReader? = null
        var writer: FileWriter? = null
        try {
            writer = FileWriter(file)
            FileLoggerStrategy.WriteTask.writeSystemInformation(mApplication, writer)
            val cmd = "logcat -d -v time"
            val process = Runtime.getRuntime().exec(cmd)
            reader = InputStreamReader(process.inputStream)
            val buffer = CharArray(10000)
            do {
                val n = reader.read(buffer, 0, buffer.size)
                if (n == -1) break
                writer.write(buffer, 0, n)
            } while (true)
        } catch (e: Throwable) {
            Log.e(
                TAG,
                "Failed to save system logcat to file: $file",
                e
            )
        } finally {
            closeSafely(writer!!)
            closeSafely(reader!!)
        }
    }

    companion object {
        private val TAG =
            ZipLogsTask::class.java.simpleName
        private const val BUFFER_SIZE = 2048
        private fun getLastPathComponent(filePath: String): String {
            val segments =
                filePath.split(File.separator.toRegex()).toTypedArray()
            return if (segments.size == 0) "" else segments[segments.size - 1]
        }
    }

    init {
        mOnCompletedListener = onCompletedListener
    }
}