package com.mapswithme.util.log

import net.jcip.annotations.GuardedBy
import net.jcip.annotations.ThreadSafe

@ThreadSafe
internal class BaseLogger(strategy: LoggerStrategy) : Logger {
    @GuardedBy("this")
    private var mStrategy: LoggerStrategy

    @Synchronized
    fun setStrategy(strategy: LoggerStrategy) {
        mStrategy = strategy
    }

    @Synchronized
    override fun v(tag: String?, msg: String?) {
        mStrategy.v(tag, msg)
    }

    @Synchronized
    override fun v(tag: String?, msg: String?, tr: Throwable?) {
        mStrategy.v(tag, msg, tr)
    }

    @Synchronized
    override fun d(tag: String?, msg: String?) {
        mStrategy.d(tag, msg)
    }

    @Synchronized
    override fun d(tag: String?, msg: String?, tr: Throwable?) {
        mStrategy.d(tag, msg, tr)
    }

    @Synchronized
    override fun i(tag: String?, msg: String?) {
        mStrategy.i(tag, msg)
    }

    @Synchronized
    override fun i(tag: String?, msg: String?, tr: Throwable?) {
        mStrategy.i(tag, msg, tr)
    }

    @Synchronized
    override fun w(tag: String?, msg: String?) {
        mStrategy.w(tag, msg)
    }

    @Synchronized
    override fun w(tag: String?, msg: String?, tr: Throwable?) {
        mStrategy.w(tag, msg, tr)
    }

    @Synchronized
    override fun w(tag: String?, tr: Throwable?) {
        mStrategy.w(tag, tr)
    }

    @Synchronized
    override fun e(tag: String?, msg: String?) {
        mStrategy.e(tag, msg)
    }

    @Synchronized
    override fun e(tag: String?, msg: String?, tr: Throwable?) {
        mStrategy.e(tag, msg, tr)
    }

    init {
        mStrategy = strategy
    }
}