package co.epsilondelta.orion_flutter.orion.util

import android.util.Log
import co.epsilondelta.orion_flutter.BuildConfig

public object OrionLogger {

    private const val TAG = "OrionFlutter"

    fun debug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun warn(message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, message)
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message, throwable)
        }
    }
}