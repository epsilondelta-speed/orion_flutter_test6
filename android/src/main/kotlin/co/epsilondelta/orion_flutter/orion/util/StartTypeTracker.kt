package co.epsilondelta.orion_flutter.orion.util

import android.content.Context
import android.content.SharedPreferences


object StartTypeTracker {
    private const val PREFS_NAME = "OrionPrefs"
    private const val KEY_LAST_LAUNCH = "lastLaunchTime"
    private const val KEY_START_TYPE = "startType"
    private var startType: String = "cold"

    /**
     * Initializes the tracker to determine if the app started with a hot or cold launch.
     */
    fun initialize(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastLaunchTime = prefs.getLong(KEY_LAST_LAUNCH, 0)

        // Determine start type based on last recorded launch time
        startType = if (lastLaunchTime == 0L) "cold" else "hot"

        // Store current launch time for future comparisons
        prefs.edit().putLong(KEY_LAST_LAUNCH, System.currentTimeMillis()).apply()
    }

    /**
     * Retrieves the type of start (hot or cold) for the current session.
     */
    fun getStartType(): String = startType
}
