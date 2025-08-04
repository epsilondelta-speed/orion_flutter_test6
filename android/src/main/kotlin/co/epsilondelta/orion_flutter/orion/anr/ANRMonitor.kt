package co.epsilondelta.orion_flutter.orion.anr

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.util.Log
import co.epsilondelta.orion_flutter.orion.EdOrion
import co.epsilondelta.orion_flutter.orion.metrics.AppMetrics
import co.epsilondelta.orion_flutter.orion.util.CommonFunctions
import co.epsilondelta.orion_flutter.orion.util.OrionLogger
import co.epsilondelta.orion_flutter.orion.util.SendData
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ANRMonitor(private val context: Context, private val sendFunction: (String) -> Unit) {

    companion object {
        private const val TAG = "ANRMonitor"
        private const val PREFS_NAME = "anr_monitor_prefs"
        private const val PREV_DETECT_TIME_KEY = "PREV_DETECT_TIME_KEY"
    }

    fun startMonitoring() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            CoroutineScope(Dispatchers.IO).launch {
                monitorANRUsingExitReasons()
            }
        } else {
            OrionLogger.debug("ANR monitoring is not supported on devices below Android 11.")
        }
    }

    private suspend fun monitorANRUsingExitReasons() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val exitInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            withContext(Dispatchers.IO) {
                activityManager.getHistoricalProcessExitReasons(null, 0, 0)
            }
        } else {
            null
        }

        if (exitInfos.isNullOrEmpty()) {
            OrionLogger.debug("No historical process exit reasons available.")
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTimestamp = prefs.getLong(PREV_DETECT_TIME_KEY, 0)

        val unprocessedInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            exitInfos.filter { it.timestamp > lastTimestamp }
        } else {
            emptyList()
        }

        if (unprocessedInfos.isNotEmpty()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val maxTimestamp = unprocessedInfos.maxOf { it.timestamp }
                prefs.edit().putLong(PREV_DETECT_TIME_KEY, maxTimestamp).apply()
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            unprocessedInfos.forEach { exitInfo ->
                if (exitInfo.reason == ApplicationExitInfo.REASON_ANR) {
                    val traceString = withContext(Dispatchers.IO) {
                        exitInfo.traceInputStream?.let {
                            readStreamToString(InputStreamReader(it))
                        } ?: "No trace available"
                    }

                    processAndSendANR(exitInfo, traceString)
                }
            }
        }
    }

    private suspend fun processAndSendANR(exitInfo: ApplicationExitInfo, traceString: String) {
        val rootCause = extractRootCauseFromTrace(traceString)
        val threadInfo = getThreadInformation()
        val memoryUsage = withContext(Dispatchers.IO) {
            EdOrion.getInstance().getRuntimeMetrics().optString("memoryUsage", "unknown")
        }
        val actions = generateActionsFromTrace(traceString)

        // Default safe values
        var reasonString = "unknown"
        var reasonNum = -1
        var timestamp: Long = System.currentTimeMillis() / 1000
        var description: String = ""

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            reasonString = getReasonString(exitInfo.reason)
            reasonNum = exitInfo.reason
            timestamp = exitInfo.timestamp / 1000
            description = exitInfo.description.orEmpty()
        }

        val anrData = mapOf(
            "reason" to reasonString,
            "reasonNum" to reasonNum,
            "timestamp" to timestamp,
            "desc" to description,
            "packageName" to context.packageName,
            "trace" to traceString,
            "rootCause" to rootCause,
            "threadInfo" to threadInfo,
            "memUsage" to memoryUsage,
            "actions" to actions
        )

        val jsonPayload = convertToJson(anrData)

        withContext(Dispatchers.IO) {
            sendANRBeacon(jsonPayload)
        }
    }


    private fun generateActionsFromTrace(trace: String): List<String> {
        val actions = mutableListOf<String>()
        try {
            if (trace.contains("android.os.NetworkOnMainThreadException")) {
                actions.add("Avoid network operations on the main thread. Use AsyncTask, Coroutine, or WorkManager.")
            }
            if (trace.contains("database")) {
                actions.add("Optimize database queries. Consider indexing or optimizing the schema.")
            }
            if (trace.contains("ViewRootImpl")) {
                actions.add("Reduce UI thread workload. Avoid heavy computations or long-running tasks on the main thread.")
            }
            if (trace.contains("Choreographer")) {
                actions.add("Check for heavy UI operations causing skipped frames.")
            }
        } catch (e: Exception) {
            OrionLogger.error( "Error generating actions from trace: ${e.message}")
        }
        return actions
    }

    private fun extractRootCauseFromTrace(trace: String): String {
        return try {
            val lines = trace.split("\n")
            lines.firstOrNull { it.contains("at ") }?.trim() ?: "Root cause not found"
        } catch (e: Exception) {
            "Error analyzing root cause"
        }
    }

    private fun getThreadInformation(): JSONObject {
        val threadInfo = JSONObject()
        try {
            val thread = Thread.currentThread()
            threadInfo.put("thread_name", thread.name)
            threadInfo.put("thread_state", thread.state.name)
            threadInfo.put("thread_priority", thread.priority)
        } catch (e: Exception) {
            OrionLogger.debug( "Error gathering thread information: ${e.message}")
        }
        return threadInfo
    }

    private fun readStreamToString(reader: InputStreamReader): String {
        val stringBuilder = StringBuilder()
        try {
            reader.use { bufferedReader ->
                BufferedReader(bufferedReader).forEachLine {
                    stringBuilder.append(it).append("\n")
                }
            }
        } catch (e: Exception) {
            OrionLogger.debug("Error reading ANR trace: ${e.message}")
        }
        return stringBuilder.toString()
    }

    private fun convertToJson(data: Map<String, Any?>): String {
        return JSONObject(data).toString()
    }

    private fun getReasonString(reason: Int): String {
        return when (reason) {
            ApplicationExitInfo.REASON_ANR -> "ANR"
            ApplicationExitInfo.REASON_CRASH -> "Crash"
            ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native Crash"
            ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "Dependency Died"
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "Excessive Resource Usage"
            else -> "Unknown Reason"
        }
    }

    fun sendANRBeacon(ANRjsonPayload: String) {
        OrionLogger.debug("Sending ANR beacon: $ANRjsonPayload")
        OrionLogger.debug(ANRjsonPayload)

        // Collect additional app metrics
        val appMetricJson = AppMetrics.getAppMetrics()
        var runtimeMetrics = EdOrion.getInstance().getRuntimeMetrics()


        // Merge JSON objects
        val objCommonFunctions = CommonFunctions()
        val metricJson = objCommonFunctions.mergeJSONObjects(appMetricJson, runtimeMetrics)
        val finalJson = objCommonFunctions.mergeJSONObjects(metricJson, JSONObject(ANRjsonPayload))

        // Add timestamp
        val millis = System.currentTimeMillis()
        finalJson.accumulate("epoch", millis)

        // Send the final data
        val sendObj = SendData()
        sendObj.coronaGo(finalJson)
    }
}
