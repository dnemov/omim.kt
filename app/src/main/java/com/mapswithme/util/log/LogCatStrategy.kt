package com.mapswithme.util.log

import android.util.Log
import com.mapswithme.maps.BuildConfig
import net.jcip.annotations.Immutable

@Immutable
internal class LogCatStrategy : LoggerStrategy {
    override fun v(tag: String?, msg: String?) {
        if (canLog()) Log.v(tag, msg)
    }

    override fun v(tag: String?, msg: String?, tr: Throwable?) {
        if (canLog()) Log.v(
            tag,
            msg,
            tr
        )
    }

    override fun d(tag: String?, msg: String?) {
        if (canLog()) Log.d(tag, msg)
    }

    override fun d(tag: String?, msg: String?, tr: Throwable?) {
        if (canLog()) Log.d(
            tag,
            msg,
            tr
        )
    }

    override fun i(tag: String?, msg: String?) {
        Log.i(tag, msg)
    }

    override fun i(tag: String?, msg: String?, tr: Throwable?) {
        Log.i(tag, msg, tr)
    }

    override fun w(tag: String?, msg: String?) {
        Log.w(tag, msg)
    }

    override fun w(tag: String?, msg: String?, tr: Throwable?) {
        Log.w(tag, msg, tr)
    }

    override fun w(tag: String?, tr: Throwable?) {
        Log.w(tag, tr)
    }

    override fun e(tag: String?, msg: String?) {
        Log.e(tag, msg)
    }

    override fun e(tag: String?, msg: String?, tr: Throwable?) {
        Log.e(tag, msg, tr)
    }

    companion object {
        private fun canLog(): Boolean {
            return BuildConfig.DEBUG
        }
    }
}