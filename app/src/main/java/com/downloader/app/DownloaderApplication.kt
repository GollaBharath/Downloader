package com.downloader.app

import android.app.Application
import android.content.Context
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DownloaderApplication : Application() {
    companion object {
        private const val TAG = "DownloaderApplication"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val pythonClass = Class.forName("com.chaquo.python.Python")
            val isStarted = pythonClass.getMethod("isStarted").invoke(null) as Boolean
            if (!isStarted) {
                val androidPlatformClass = Class.forName("com.chaquo.python.android.AndroidPlatform")
                val platform = androidPlatformClass.getConstructor(Context::class.java)
                    .newInstance(this)
                pythonClass.getMethod("start", Class.forName("com.chaquo.python.Python\$Platform"))
                    .invoke(null, platform)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize Python runtime", t)
        }
    }
}