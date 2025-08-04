package co.epsilondelta.orion_flutter.orion.network

import co.epsilondelta.orion_flutter.orion.util.CurrentActivityTracker
import co.epsilondelta.orion_flutter.orion.util.OrionLogger

object NetworkTrackerUtil {
    @JvmStatic
    fun trackRequest(
        method: String,
        url: String,
        statusCode: Int,
        startTime: Long,
        endTime: Long,
        duration: Long,
        payloadSize: Long? = null,
        errorMessage: String? = null
    ) {
        val activityName = CurrentActivityTracker.getCurrentActivityName()
        OrionLogger.debug( "✅ Tracked Network Request for Activity: $activityName")
        NetworkRequestTracker.track(
            activityName,
            NetworkRequestInfo(
                method = method,
                url = url,
                statusCode = statusCode,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                payloadSize = payloadSize,
                errorMessage = errorMessage
            )
        )
    }
}