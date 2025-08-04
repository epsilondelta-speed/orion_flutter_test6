package co.epsilondelta.orion_flutter.orion.util
import co.epsilondelta.orion_flutter.orion.metrics.AppMetrics
import co.epsilondelta.orion_flutter.orion.util.OrionLogger
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class FlutterSendData {

    fun sendFlutterScreenMetrics(
        screenName: String,
        ttid: Int,
        ttfd: Int,
        jankyFrames: Int,
        frozenFrames: Int,
        networkRequests: List<Map<String, Any?>>,
        runtimeMetrics: String?
    ) {
        try {
            val networkArray = JSONArray()

            for (req in networkRequests) {
                val obj = JSONObject().apply {
                    put("url", req["url"] as String)
                    put("method", req["method"] as String)
                    put("statusCode", (req["statusCode"] as Number).toInt())
                    put("startTime", (req["startTime"] as Number).toLong())
                    put("endTime", (req["endTime"] as Number).toLong())
                    put("duration", (req["duration"] as Number).toInt())
                    req["payloadSize"]?.let { put("payloadSize", (it as Number).toInt()) }
                    req["errorMessage"]?.let { put("errorMessage", it as String) }
                }
                networkArray.put(obj)
            }

            val staticMetrics = AppMetrics.getAppMetrics()

            val finalMetrics = JSONObject().apply {
                put("flutter", 1)
                put("screen", screenName)
                put("activityName", screenName)
                put("ttid", ttid)
                put("ttfd", ttfd)
                put("jankyFrames", jankyFrames)
                put("frozenFrames", frozenFrames)
                put("network", networkArray)
                if (runtimeMetrics != null) {
                    try {
                        val parsed = JSONObject(runtimeMetrics)
                        val keys = parsed.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            put(key, parsed.get(key))
                        }
                    } catch (e: Exception) {
                        OrionLogger.debug( "Failed to flatten runtimeMetrics: ${e.message}")
                    }
                }

                staticMetrics.keys().forEach { key -> put(key, staticMetrics[key]) }
            }

            OrionLogger.debug(  "📤 Sending Flutter screen metrics: $finalMetrics")

            SendData().coronaGo(finalMetrics)

        } catch (e: Exception) {
            OrionLogger.error(  "❌ Failed to send Flutter screen metrics: ${e.message}", e)
        }
    }
}