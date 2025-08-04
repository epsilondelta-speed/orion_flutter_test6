package co.epsilondelta.orion_flutter.orion.network

import co.epsilondelta.orion_flutter.orion.EdOrion

object NetworkRequestTracker {
    private val requestMap = mutableMapOf<String, MutableList<NetworkRequestInfo>>()

    fun track(activityName: String, info: NetworkRequestInfo) {
        if (!EdOrion.isNetworkTrackingEnabled()) return
        requestMap.getOrPut(activityName) { mutableListOf() }.add(info)
    }

    fun consumeRequestsForActivity(activityName: String): List<NetworkRequestInfo> {
        return requestMap.remove(activityName) ?: emptyList()
    }

    /**
     * Fallback handler to collect any leftover requests for AppLaunched or AppBackground
     * Should be called after normal consumeRequestsForActivity() in ActivityLifecycle.
     */
    fun consumeFallbackRequests(): Map<String, List<NetworkRequestInfo>> {
        val fallbackKeys = listOf("AppLaunched", "AppBackground")
        val result = mutableMapOf<String, List<NetworkRequestInfo>>()

        for (key in fallbackKeys) {
            requestMap.remove(key)?.let { requests ->
                if (requests.isNotEmpty()) {
                    result[key] = requests
                }
            }
        }

        return result
    }


}