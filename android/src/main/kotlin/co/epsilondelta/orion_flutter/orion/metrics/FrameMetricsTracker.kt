package co.epsilondelta.orion_flutter.orion.metrics

import android.app.Activity
import android.os.Build
import android.util.Log
import android.util.SparseIntArray
import androidx.annotation.RequiresApi
import androidx.core.app.FrameMetricsAggregator
import co.epsilondelta.orion_flutter.orion.util.OrionLogger

@RequiresApi(24)
object FrameMetricsTracker {
    private const val TAG = "Orion"
    private val aggregator = FrameMetricsAggregator(FrameMetricsAggregator.TOTAL_DURATION)

    // ✅ Track activities that were added to avoid calling remove() on untracked ones
    private val trackedActivities = mutableSetOf<Activity>()

    @RequiresApi(Build.VERSION_CODES.N)
    fun startTracking(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //Log.d(TAG, "Start tracking frames for ${activity.localClassName}")
            aggregator.add(activity)
            trackedActivities.add(activity)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun stopTracking(activity: Activity): Map<String, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!trackedActivities.contains(activity)) {
                //Log.w(TAG, "Skipping remove — activity not tracked: ${activity.localClassName}")
                return defaultResult()
            }

            val metrics = aggregator.remove(activity)
            trackedActivities.remove(activity)

            if (metrics == null) {
                //Log.w(TAG, "No frame metrics available for ${activity.localClassName}")
                return defaultResult()
            }

            val sparseArray: SparseIntArray? = metrics[FrameMetricsAggregator.TOTAL_DURATION]
            val frameDurations = mutableListOf<Long>()
            if (sparseArray != null) {
                for (i in 0 until sparseArray.size()) {
                    val durationNs = sparseArray.keyAt(i).toLong()
                    val count = sparseArray.valueAt(i)
                    repeat(count) { frameDurations.add(durationNs) }
                }
            }

            val jankyFrames = frameDurations.count { it > 6_000_000 }  // > 6ms (for QA)
            val frozenFrames = frameDurations.count { it > 700_000_000 }  // > 700ms

           // Log.d(TAG, "Stop tracking for ${activity.localClassName}: Janky=$jankyFrames, Frozen=$frozenFrames")

            return mapOf(
                "jankyFrames" to jankyFrames,
                "frozenFrames" to frozenFrames
            )
        }

        return defaultResult()
    }

    private fun defaultResult(): Map<String, Int> {
        return mapOf(
            "jankyFrames" to 0,
            "frozenFrames" to 0
        )
    }
}