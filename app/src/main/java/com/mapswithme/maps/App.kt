package com.mapswithme.maps

import android.app.Application

class App : Application() {

    lateinit var jniString: String

    override fun onCreate() {
        super.onCreate()

        jniString = stringFromJNI()
    }


    val hello: String
        get() = jniString

    companion object {

        /**
         * A native method that is implemented by the 'native-lib' native library,
         * which is packaged with this application.
         */
        @JvmStatic
        private external fun stringFromJNI(): String

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("mapswithme")
        }
    }

}