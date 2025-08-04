package co.epsilondelta.orion_flutter.orion.startup

import android.app.Application
import android.util.Log
import android.view.Choreographer
import co.epsilondelta.orion_flutter.orion.util.OrionLogger
import org.json.JSONObject

object StartupMetricsTracker {

    private const val TAG = "Orion"

    private var appStartTime: Long = 0L
    private var currentActivityStartTime: Long = 0L
    private var ttidLogged = false
    private var ttfDisplayLogged = false
    private var ttid: Long = 0L
    private var ttfDisplay: Long = 0L

    /**
     * Initialize application-level tracking.
     */
    fun initialize(application: Application) {
        appStartTime = System.currentTimeMillis()
        //Log.d(TAG, "Application start time recorded: $appStartTime")
    }

    /**
     * Start tracking for a new activity.
     */
    fun startActivityTracking() {
        currentActivityStartTime = System.currentTimeMillis()
        ttidLogged = false
        ttfDisplayLogged = false
        ttid = 0L
        ttfDisplay = 0L

        //OrionLogger.debug( "Activity start time recorded: $currentActivityStartTime")
    }

    /**
     * Log TTID (Time to Initial Display).
     */
    fun logTTID() {
        if (!ttidLogged) {
            ttid = System.currentTimeMillis() - currentActivityStartTime
            ttidLogged = true
            //Log.d(TAG, "TTID (Time to Initial Display): $ttid ms")
        }
    }

    /**
     * Schedule TTFD (Time to Full Display) tracking using Choreographer.
     */
    fun trackTTFD() {
        Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!ttfDisplayLogged) {

                    ttfDisplay = System.currentTimeMillis() - currentActivityStartTime
                    ttfDisplayLogged = true
                    //OrionLogger.debug(  "TTFD (Time to Full Display): $ttfDisplay ms")
                }
            }
        })
    }

    /**
     * Get the metrics for the current activity.
     */
    fun getStartupMetrics(): JSONObject {
        return JSONObject().apply {
            put("ttid", ttid)
            put("ttfd", ttfDisplay)
        }
    }

    fun saveExitTimestamp(application: Application) {
        val sharedPrefs = application.getSharedPreferences("StartupMetrics", Application.MODE_PRIVATE)
        val exitTimestamp = System.currentTimeMillis()
        sharedPrefs.edit().putLong("last_exit_time", exitTimestamp).apply()
        //Log.d(TAG, "Exit timestamp saved: $exitTimestamp")
    }
}