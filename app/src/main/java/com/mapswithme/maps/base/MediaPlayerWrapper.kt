package com.mapswithme.maps.base

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.os.AsyncTask
import androidx.annotation.RawRes
import com.mapswithme.maps.MwmApplication

class MediaPlayerWrapper(private val app: Application) {
    private var mPlayer: MediaPlayer? = null
    private var mCompletionListener: OnCompletionListener? = null
    private var mStreamResId = UNDEFINED_SOUND_STREAM
    private fun isCurrentSoundStream(@RawRes streamResId: Int): Boolean {
        return mStreamResId == streamResId
    }

    private fun onInitializationCompleted(initializationResult: InitializationResult) {
        releaseInternal()
        mStreamResId = initializationResult.streamResId
        mPlayer = initializationResult.player
        if (mPlayer == null) return
        mPlayer!!.setOnCompletionListener(mCompletionListener)
        mPlayer!!.start()
    }

    private fun releaseInternal() {
        if (mPlayer == null) return
        stop()
        mPlayer!!.release()
        mPlayer = null
        mStreamResId = UNDEFINED_SOUND_STREAM
    }

    fun release() {
        releaseInternal()
        mCompletionListener = null
    }

    fun playback(
        @RawRes streamResId: Int,
        completionListener: OnCompletionListener?
    ) {
        if (isCurrentSoundStream(streamResId) && mPlayer == null) return
        if (isCurrentSoundStream(streamResId) && mPlayer != null) {
            mPlayer!!.start()
            return
        }
        mCompletionListener = completionListener
        mStreamResId = streamResId
        val task =
            makeInitTask(this)
        task.execute(streamResId)
    }

    fun stop() {
        if (mPlayer == null) return
        mPlayer!!.stop()
    }

    val isPlaying: Boolean
        get() = mPlayer != null && mPlayer!!.isPlaying

    private class InitPlayerTask internal constructor(private val mWrapper: MediaPlayerWrapper) :
        AsyncTask<Int, Void, InitializationResult>() {

        override fun onPostExecute(initializationResult: InitializationResult) {
            super.onPostExecute(initializationResult)
            mWrapper.onInitializationCompleted(initializationResult)
        }

        override fun doInBackground(vararg params: Int?): InitializationResult {
            require(params.isNotEmpty()) { "Params not found" }
            val resId = params[0]
            val player = MediaPlayer.create(mWrapper.app, resId!!)
            return InitializationResult(player, resId)
        }

    }

    private class InitializationResult(
        val player: MediaPlayer?, @RawRes val streamResId: Int
    )

    companion object {
        private const val UNDEFINED_SOUND_STREAM = -1
        private fun makeInitTask(wrapper: MediaPlayerWrapper): AsyncTask<Int, Void, InitializationResult> {
            return InitPlayerTask(wrapper)
        }

        @JvmStatic
        fun from(context: Context): MediaPlayerWrapper {
            val app = context.applicationContext as MwmApplication
            return app.mediaPlayer
        }
    }

}