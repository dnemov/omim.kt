package com.mapswithme.maps

import java.util.*

class VideoTimer {
    var m_timer: Timer? = null

    inner class VideoTimerTask : TimerTask() {
        override fun run() {
            nativeRun()
        }
    }

    var m_timerTask: VideoTimerTask? = null
    var m_interval: Int
    fun start() {
        m_timerTask = VideoTimerTask()
        m_timer = Timer("VideoTimer")
        m_timer!!.scheduleAtFixedRate(m_timerTask, 0, m_interval.toLong())
    }

    fun resume() {
        m_timerTask = VideoTimerTask()
        m_timer = Timer("VideoTimer")
        m_timer!!.scheduleAtFixedRate(m_timerTask, 0, m_interval.toLong())
    }

    fun pause() {
        m_timer!!.cancel()
    }

    fun stop() {
        m_timer!!.cancel()
    }

    companion object {
        private const val TAG = "VideoTimer"

        @JvmStatic
        private external fun nativeInit()
        @JvmStatic
        private external fun nativeRun()
    }

    init {
        m_interval = 1000 / 60
        nativeInit()
    }
}