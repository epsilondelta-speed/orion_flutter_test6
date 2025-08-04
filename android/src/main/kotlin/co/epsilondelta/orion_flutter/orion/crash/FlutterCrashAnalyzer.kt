package co.epsilondelta.orion_flutter.orion.crash

import android.content.Context
import co.epsilondelta.orion_flutter.orion.EdOrion
import co.epsilondelta.orion_flutter.orion.metrics.AppMetrics
import co.epsilondelta.orion_flutter.orion.metrics.AppRuntimeMetrics
import co.epsilondelta.orion_flutter.orion.util.CommonFunctions
import co.epsilondelta.orion_flutter.orion.util.OrionLogger
import co.epsilondelta.orion_flutter.orion.util.SendData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class FlutterCrashAnalyzer(
    private val errorJson: JSONObject,
    private val screenName: String? = "UnknownScreen",
    private val networkRaw: ArrayList<HashMap<String, Any>>? = null
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        fun sendFlutterCrash(errorJson: JSONObject, screenName: String?, networkRaw: ArrayList<HashMap<String, Any>>?) {
            val analyzer = FlutterCrashAnalyzer(errorJson, screenName, networkRaw)
            analyzer.sendCrashBeacon()
        }
    }

    fun sendCrashBeacon() {
        coroutineScope.launch {
            try {
                val crashReport = withContext(Dispatchers.Default) { buildReportFromFlutterJson() }
                //OrionLogger.debug("[FlutterCrash] Payload: ${crashReport.toString(2)}")

                withContext(Dispatchers.IO) {
                    SendData().coronaGo(crashReport)
                }
            } catch (e: Exception) {
                OrionLogger.error("Error sending Flutter crash: ${e.message}")
            }
        }
    }

    private fun buildReportFromFlutterJson(): JSONObject {
        val mergedJson = JSONObject()
        val context = EdOrion.getInstance().getApplication()?.applicationContext

        val appMetrics = AppMetrics.getAppMetrics()
        val runtimeMetrics = EdOrion.getInstance().getRuntimeMetrics()
        val common = CommonFunctions()

        val networkData = convertNetworkRawToJSONArray()

        mergedJson.put("source", "flutter")
        mergedJson.put("crashType", "FlutterError")
        mergedJson.put("activity", screenName ?: "UnknownScreen")
        mergedJson.put("stackTrace", errorJson.optString("stack"))
        mergedJson.put("localizedMessage", errorJson.optString("exception"))
        mergedJson.put("crashLocation", errorJson.optString("library"))
        mergedJson.put("screenContext", errorJson.optString("context"))
        mergedJson.put("timestamp", errorJson.optLong("timestamp", System.currentTimeMillis()))
        mergedJson.put("threadInfo", getThreadInformation())
        mergedJson.put("action", generateActionableInsight(errorJson.optString("exception")))
        mergedJson.put("environment", getEnvironmentVariables())

        if (context != null) {
            mergedJson.put("networkState", getNetworkState())
        } else {
            mergedJson.put("networkState", "NA")
        }

        mergedJson.put("lastUserInteraction", AppRuntimeMetrics().getLastUserInteraction())

        if (networkData != null) {
            mergedJson.put("network", networkData)
        }

        return common.mergeJSONObjects(common.mergeJSONObjects(appMetrics, runtimeMetrics), mergedJson)
    }

    private fun convertNetworkRawToJSONArray(): JSONArray? {
        if (networkRaw == null) return null
        val jsonArray = JSONArray()
        for (entry in networkRaw) {
            val obj = JSONObject()
            for ((key, value) in entry) {
                obj.put(key, value)
            }
            jsonArray.put(obj)
        }
        return jsonArray
    }

    private fun getThreadInformation(): JSONObject {
        return JSONObject().apply {
            try {
                val thread = Thread.currentThread()
                put("threadName", thread.name)
                put("threadState", thread.state.name)
            } catch (e: Exception) {
                OrionLogger.debug("Error getting thread information: ${e.message}")
            }
        }
    }

    private fun generateActionableInsight(message: String?): String {
        return when {
            message?.contains("null") == true -> "Check for null object usage."
            message?.contains("index") == true -> "Check for index bounds."
            message?.contains("illegal") == true -> "Check for invalid arguments or states."
            else -> "Refer to stack trace and logs for debugging."
        }
    }

    private fun getEnvironmentVariables(): JSONObject {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val memoryPercentage = if (totalMemory > 0) ((usedMemory * 100) / totalMemory).toInt() else 0

        return JSONObject().apply {
            put("cpuNum", runtime.availableProcessors())
            put("mem", memoryPercentage)
        }
    }

    fun getNetworkState(): String {
        val context = EdOrion.getInstance().getApplication()?.applicationContext
            ?: return "NA"

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return "NA"
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "NA"

            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "data"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "eth"
                else -> "Other"
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo ?: return "NA"
            when (activeNetworkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> "wifi"
                ConnectivityManager.TYPE_MOBILE -> "data"
                ConnectivityManager.TYPE_ETHERNET -> "eth"
                else -> "Other"
            }
        }
    }
}