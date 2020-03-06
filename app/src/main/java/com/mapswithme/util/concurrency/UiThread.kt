package com.mapswithme.util.concurrency

import android.os.Handler
import android.os.Looper

object UiThread {
    private val sUiHandler = Handler(Looper.getMainLooper())
    val isUiThread: Boolean
        get() = Looper.getMainLooper().thread === Thread.currentThread()

    /**
     * Executes something on UI thread. If called from UI thread then given task will be executed synchronously.
     *
     * @param task the code that must be executed on UI thread.
     */
    fun run(task: Runnable) {
        if (isUiThread) task.run() else sUiHandler.post(task)
    }
    /**
     * Executes something on UI thread after a given delayMillis.
     *
     * @param task  the code that must be executed on UI thread after given delayMillis.
     * @param delayMillis The delayMillis until the code will be executed.
     */
    /**
     * Executes something on UI thread after last message queued in the application's main looper.
     *
     * @param task the code that must be executed later on UI thread.
     */
    @JvmOverloads
    fun runLater(task: Runnable?, delayMillis: Long = 0) {
        sUiHandler.postDelayed(task, delayMillis)
    }

    /**
     * Cancels execution of the given task that was previously queued with [.run],
     * [.runLater] or [.runLater] if it was not started yet.
     *
     * @param task the code that must be cancelled.
     */
    fun cancelDelayedTasks(task: Runnable?) {
        sUiHandler.removeCallbacks(task)
    }
}