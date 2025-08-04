package co.epsilondelta.orion_flutter.orion.util

import android.app.Activity
import android.os.Bundle
import co.epsilondelta.orion_flutter.orion.OrionFlow
import co.epsilondelta.orion_flutter.orion.OrionLifecycle
import co.epsilondelta.orion_flutter.orion.metrics.FrameMetricsTracker

class OrionLifecycleListener : OrionLifecycle.Listener {

    private val orionFlow = OrionFlow() // Singleton instance of OrionFlow

    override fun onReceiveActivityEvent(activity: Activity, event: String, bundle: Bundle?) {
        val activityName = activity.localClassName
        val timestamp = System.currentTimeMillis()

       // OrionLogger.debug("ACTIVITY: $activityName >>> EVENT: $event at $timestamp")

        // Handle activity lifecycle events for frame metrics
        when (event) {
            "RESUME" -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    FrameMetricsTracker.startTracking(activity)
                }
            }
            "STOP" -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    val metrics = FrameMetricsTracker.stopTracking(activity)
                    // Log or process metrics
                    //OrionLogger.debug("Frame Metrics for $activityName: $metrics")
                }
            }
        }

        // Process activity-specific calculations
        orionFlow.startActiviyCalculation(activityName, event)
    }

    override fun onReceiveFragmentEvent(fragment: androidx.fragment.app.Fragment, context: android.content.Context?, event: String, bundle: Bundle?) {
        // Intentionally left empty as fragment-level tracking is not required
    }
}