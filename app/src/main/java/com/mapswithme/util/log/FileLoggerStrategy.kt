package com.mapswithme.util.log

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import com.mapswithme.maps.BuildConfig
import com.mapswithme.util.StorageUtils.ensureLogsFolderExistence
import com.mapswithme.util.Utils.fullDeviceModel
import com.mapswithme.util.Utils.installationId
import net.jcip.annotations.Immutable
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

@Immutable
internal class FileLoggerStrategy(
    private val mApplication: Application, private val mFilePath: String,
    private val mExecutor: Executor
) : LoggerStrategy {
    override fun v(tag: String?, msg: String?) {
        write("V/$tag: $msg")
    }

    override fun v(tag: String?, msg: String?, tr: Throwable?) {
        write("V/" + tag + ": " + msg + "\n" + Log.getStackTraceString(tr))
    }

    override fun d(tag: String?, msg: String?) {
        write("D/$tag: $msg")
    }

    override fun d(tag: String?, msg: String?, tr: Throwable?) {
        write("D/" + tag + ": " + msg + "\n" + Log.getStackTraceString(tr))
    }

    override fun i(tag: String?, msg: String?) {
        write("I/$tag: $msg")
    }

    override fun i(tag: String?, msg: String?, tr: Throwable?) {
        write("I/" + tag + ": " + msg + "\n" + Log.getStackTraceString(tr))
    }

    override fun w(tag: String?, msg: String?) {
        write("W/$tag: $msg")
    }

    override fun w(tag: String?, msg: String?, tr: Throwable?) {
        write("W/" + tag + ": " + msg + "\n" + Log.getStackTraceString(tr))
    }

    override fun w(tag: String?, tr: Throwable?) {
        write("D/" + tag + ": " + Log.getStackTraceString(tr))
    }

    override fun e(tag: String?, msg: String?) {
        write("E/$tag: $msg")
    }

    override fun e(tag: String?, msg: String?, tr: Throwable?) {
        write("E/" + tag + ": " + msg + "\n" + Log.getStackTraceString(tr))
    }

    private fun write(data: String) {
        mExecutor.execute(
            WriteTask(
                mApplication,
                mFilePath,
                data,
                Thread.currentThread().name
            )
        )
    }

    internal class WriteTask(
        private val mApplication: Application, private val mFilePath: String,
        private val mData: String, private val mCallingThread: String
    ) : Runnable {
        override fun run() {
            var fw: FileWriter? = null
            try {
                val file = File(mFilePath)
                if (!file.exists() || file.length() > MAX_SIZE) {
                    fw = FileWriter(file, false)
                    writeSystemInformation(
                        mApplication,
                        fw
                    )
                } else {
                    fw = FileWriter(mFilePath, true)
                }
                val formatter: DateFormat =
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                fw.write(formatter.format(Date()) + " " + mCallingThread + ": " + mData + "\n")
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "Failed to write the string: $mData",
                    e
                )
                Log.i(
                    TAG,
                    "Logs folder exists: " + ensureLogsFolderExistence(mApplication)
                )
            } finally {
                if (fw != null) try {
                    fw.close()
                } catch (e: IOException) {
                    Log.e(
                        TAG,
                        "Failed to close file: $mData",
                        e
                    )
                }
            }
        }

        companion object {
            private const val MAX_SIZE = 3000000
            @Throws(IOException::class)
            fun writeSystemInformation(
                application: Application,
                fw: FileWriter
            ) {
                fw.write("Android version: " + Build.VERSION.SDK_INT + "\n")
                fw.write("Device: $fullDeviceModel\n")
                fw.write("App version: " + BuildConfig.APPLICATION_ID + " " + BuildConfig.VERSION_NAME + "\n")
                fw.write("Installation ID: $installationId\n")
                fw.write("Locale : " + Locale.getDefault())
                fw.write("\nNetworks : ")
                val manager =
                    application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                for (info in manager.allNetworkInfo) fw.write(info.toString())
                fw.write("\nLocation providers: ")
                val locMngr =
                    application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                for (provider in locMngr.getProviders(true)) fw.write("$provider ")
                fw.write("\n\n")
            }
        }

    }

    companion object {
        private val TAG =
            FileLoggerStrategy::class.java.simpleName
    }

}