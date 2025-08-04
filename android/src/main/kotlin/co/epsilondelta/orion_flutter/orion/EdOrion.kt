package co.epsilondelta.orion_flutter.orion

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import co.epsilondelta.orion_flutter.orion.anr.ANRMonitor
import co.epsilondelta.orion_flutter.orion.crash.AppCrashAnalyzer
import co.epsilondelta.orion_flutter.orion.metrics.AppMetrics
import co.epsilondelta.orion_flutter.orion.metrics.AppRuntimeMetrics
import co.epsilondelta.orion_flutter.orion.metrics.FrameMetricsTracker
import co.epsilondelta.orion_flutter.orion.startup.StartupMetricsTracker
import co.epsilondelta.orion_flutter.orion.util.OrionConfig
import co.epsilondelta.orion_flutter.orion.util.OrionLogger
import co.epsilondelta.orion_flutter.orion.util.SamplingManager
import co.epsilondelta.orion_flutter.orion.util.SendData
import co.epsilondelta.orion_flutter.orion.util.SessionManager
import co.epsilondelta.orion_flutter.orion.util.StartTypeTracker
import co.epsilondelta.orion_flutter.orion.util.CurrentActivityTracker
import co.epsilondelta.orion_flutter.orion.network.NetworkRequestTracker
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.Response
//import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class EdOrion private constructor(private val builder: Builder) {

    private var isLogEnabled: Boolean = false
    private var cid: String = ""
    private var pid: String = ""
    private var application: Application? = null
    private var isAnrMonitoringEnabled: Boolean = false
    private var currentActivity: String? = null // Track the current activity
    private val appRuntimeMetrics: AppRuntimeMetrics = AppRuntimeMetrics()
    private var networkTrackingEnabled = true

    private var activeActivityCount = 0


    init {

        OrionLogger.debug( "init......")
        application = builder.application
        isLogEnabled = builder.isLogEnabled
        cid = builder.cid
        pid = builder.pid
        isAnrMonitoringEnabled = builder.isAnrMonitoringEnabled

        OrionConfig.let {
            it.cid = cid
            it.pid = pid
            it.appContext = application
            OrionLogger.debug( "set config  orion $cid")
        }



        // Initialize AppRuntimeMetrics with the Application context
        appRuntimeMetrics.initialize(application!!)

        // Initialize Startup Metrics Tracking
        StartupMetricsTracker.initialize(application!!)

        //Initialize SessionManager with application context
        SessionManager.initialize(application!!)

        // Initialize Network Tracking globally
        //NetworkTracker.initializeGlobalTracking()

        //sampling for beacon data send
        SamplingManager.initialize(cid)

        //to Track hot /cold startup
        StartTypeTracker.initialize(application!!);

        // Track the current activity and save metrics
        application?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                currentActivity = activity.localClassName // Update the current activity
                CurrentActivityTracker.setCurrentActivity(activity)
                CurrentActivityTracker.appMovedToForeground()
                activeActivityCount++
                if (activeActivityCount == 1) {
                    // App moved to the foreground
                    OrionLogger.debug("App moved to foreground.")
                    SessionManager.updateSessionTimestamp()
                }

                appRuntimeMetrics.recordUserInteraction("Activity Started: $currentActivity")
                StartupMetricsTracker.startActivityTracking() // Start TTID/TTFD tracking
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FrameMetricsTracker.startTracking(activity) // Start frame metrics tracking
                }

            }

            override fun onActivityResumed(activity: Activity) {
                appRuntimeMetrics.recordUserInteraction("Activity Resumed: ${activity.localClassName}")
                CurrentActivityTracker.setCurrentActivity(activity)
                StartupMetricsTracker.logTTID() // Log TTID (Time to Initial Display)
                StartupMetricsTracker.trackTTFD() // Start TTFD tracking
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                currentActivity = activity.localClassName
                appRuntimeMetrics.recordUserInteraction("Activity Stopped: ${currentActivity}")
                var frameMetrics = mapOf(
                    "jankyFrames" to 0,
                    "frozenFrames" to 0
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    frameMetrics =
                        FrameMetricsTracker.stopTracking(activity) // Stop frame metrics tracking
                }

                // End the session if the app is no longer visible
                activeActivityCount--
                if (activeActivityCount == 0) {
                    // App moved to the background
                    CurrentActivityTracker.appMovedToBackground()
                    OrionLogger.debug("App moved to background.")
                }


                // Merge metrics
                val staticMetrics = AppMetrics.getAppMetrics()
                val runtimeMetrics = appRuntimeMetrics.getRuntimeMetrics()
                val startupMetrics = StartupMetricsTracker.getStartupMetrics()

                val finalMetrics = JSONObject().apply {
                    put("activityName", activity.localClassName)
                    put("jankyFrames", frameMetrics["jankyFrames"])
                    put("frozenFrames", frameMetrics["frozenFrames"])
                    put("ttid", startupMetrics.getLong("ttid"))
                    put("ttfd", startupMetrics.getLong("ttfd"))
                    //put("network", networkData)
                    staticMetrics.keys().forEach { key -> put(key, staticMetrics[key]) }
                    runtimeMetrics.keys().forEach { key -> put(key, runtimeMetrics[key]) }
                }

                val networkRequests = NetworkRequestTracker.consumeRequestsForActivity(activity.localClassName)

                val networkArray = org.json.JSONArray()
                networkRequests.forEach {
                    val obj = org.json.JSONObject().apply {
                        put("url", it.url)
                        put("method", it.method)
                        put("statusCode", it.statusCode)
                        put("startTime", it.startTime)
                        put("endTime", it.endTime)
                        put("duration", it.duration)
                        it.payloadSize?.let { size -> put("payloadSize", size) }
                        it.errorMessage?.let { msg -> put("errorMessage", msg) }
                    }
                    networkArray.put(obj)
                }

                finalMetrics.put("network", networkArray)

                val fallbackNetworkRequests = NetworkRequestTracker.consumeFallbackRequests()

                val fallbackNetworkArray = org.json.JSONArray()

                fallbackNetworkRequests.forEach { (source, requests) ->
                    requests.forEach {
                        val obj = org.json.JSONObject().apply {
                            put("activity", source) // include AppLaunched or AppBackground
                            put("url", it.url)
                            put("method", it.method)
                            put("statusCode", it.statusCode)
                            put("startTime", it.startTime)
                            put("endTime", it.endTime)
                            put("duration", it.duration)
                            it.payloadSize?.let { size -> put("payloadSize", size) }
                            it.errorMessage?.let { msg -> put("errorMessage", msg) }
                        }
                        fallbackNetworkArray.put(obj)
                    }
                }

                finalMetrics.put("fallbackNetwork", fallbackNetworkArray)

                //Log.d("Orion", "Final Metrics for Activity ${activity.localClassName}: $finalMetrics")


                // Send the metrics to the server
                SendData().coronaGo(finalMetrics)

                StartupMetricsTracker.saveExitTimestamp(application!!)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity == activity.localClassName) {
                    currentActivity = null // Clear the activity if destroyed
                    CurrentActivityTracker.setCurrentActivity(null)
                }
            }
        })


        INSTANCE = this

        val handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val crashAnalyzer = AppCrashAnalyzer(throwable, currentActivity ?: "UnknownActivity")
            crashAnalyzer.sendCrashBeacon()
            handler?.let {
                try {
                    TimeUnit.MILLISECONDS.sleep(2000) // Ensure crash beacon is sent before app terminates
                } catch (e: InterruptedException) {
                    Log.d("Orion", "InterruptedException")
                }
                it.uncaughtException(thread, throwable)
            }
        }

    }

    private fun isAppInBackground(): Boolean {
        return activeActivityCount == 0
    }


    private fun trackDisplayMetrics(view: View) {
        val viewTreeObserver = view.viewTreeObserver

        val onDrawListener = object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                // Capture metrics or perform your tracking logic here

                // Safely remove the OnDrawListener using a Handler
                Handler(Looper.getMainLooper()).post {
                    if (viewTreeObserver.isAlive) {
                        viewTreeObserver.removeOnDrawListener(this)
                    }
                }
            }
        }

        if (viewTreeObserver.isAlive) {
            viewTreeObserver.addOnDrawListener(onDrawListener)
        }
    }

    fun getApplication(): Application? {
        return application
    }

    class Builder(internal val application: Application) {

        internal var isLogEnabled: Boolean = false
        internal var cid: String = ""
        internal var pid: String = ""
        internal var isAnrMonitoringEnabled: Boolean = false

        fun setConfig(cid: String, pid: String): Builder {
            this.cid = cid
            this.pid = pid
            return this
        }

        fun setLogEnable(isLogEnabled: Boolean): Builder {
            this.isLogEnabled = isLogEnabled
            return this
        }

        fun enableAnrMonitoring(enable: Boolean): Builder {
            this.isAnrMonitoringEnabled = enable
            return this
        }

        fun build() = EdOrion(this)
    }

    fun startListening() {
        val startTime = System.currentTimeMillis()

        AppMetrics.setAppMetrics(this.builder.application)

        val duration = System.currentTimeMillis() - startTime
        if (duration > 16) {
            OrionLogger.debug("Initializing Orion startListening blocked main for $duration ms")
        }

        // Start ANR monitoring if enabled
        if (isAnrMonitoringEnabled) {
            val anrMonitor = ANRMonitor(application!!) { jsonPayload ->
                OrionLogger.debug("ANR Beacon Sent") // Keep this for debug purposes
            }
            anrMonitor.startMonitoring()
        }
    }

    /**
     * Check if the current Android version is supported (API 24+ required)
     */
    private fun isSupportedVersion(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }

    fun getRuntimeMetrics(): JSONObject {
        return appRuntimeMetrics.getRuntimeMetrics()
    }

    companion object {
        @JvmStatic
        @Volatile
        private var INSTANCE: EdOrion? = null

        @Synchronized
        fun getInstance(): EdOrion = INSTANCE
            ?: throw RuntimeException(Constant.INSTANCE_MESSAGE)

        @Synchronized
        fun getDefaultInstance(application: Application): EdOrion {
            if (INSTANCE == null) {
                INSTANCE = Builder(application).build()
            }
            return INSTANCE!!
        }

        fun isNetworkTrackingEnabled(): Boolean = true

    }

    private object Constant {
        internal const val INSTANCE_MESSAGE =
            "EdOrion isn't initialized yet. Please use EdOrion.Builder(application)!"
    }
}