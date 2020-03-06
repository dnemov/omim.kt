package com.mapswithme.util.concurrency

import com.mapswithme.util.concurrency.UiThread
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ThreadPool private constructor() {
    private val mStorage: ThreadPoolExecutor
    private val mWorker: ThreadPoolExecutor

    companion object {
        private val sInstance =
            ThreadPool()

        private fun create(
            poolSize: Int,
            allowedTime: Int
        ): ThreadPoolExecutor {
            val res =
                ThreadPoolExecutor(
                    poolSize, poolSize, allowedTime.toLong(), TimeUnit.SECONDS,
                    LinkedBlockingQueue()
                )
            res.allowCoreThreadTimeOut(true)
            return res
        }

        /**
         * Returns single thread for file operations.
         */
        val storage: ExecutorService
            get() = sInstance.mStorage

        /**
         * Returns single thread for various background tasks.
         */
        val worker: ExecutorService
            get() = sInstance.mWorker
    }

    init {
        mStorage = create(1, 500)
        mWorker = create(1, 250)
    }
}