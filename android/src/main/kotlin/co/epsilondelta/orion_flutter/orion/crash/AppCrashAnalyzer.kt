package co.epsilondelta.orion_flutter.orion.crash

import co.epsilondelta.orion_flutter.orion.EdOrion
import android.content.Context
import co.epsilondelta.orion_flutter.orion.metrics.AppMetrics
import co.epsilondelta.orion_flutter.orion.metrics.AppRuntimeMetrics
import co.epsilondelta.orion_flutter.orion.util.CommonFunctions
import co.epsilondelta.orion_flutter.orion.util.OrionLogger
import co.epsilondelta.orion_flutter.orion.util.SendData
import org.json.JSONObject
import java.util.*
import java.io.IOException
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppCrashAnalyzer(
    private val throwable: Throwable,
    private val activityName: String? = "UnknownActivity"
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)


    fun sendCrashBeacon() {
        coroutineScope.launch {
            val crashJsonObj = withContext(Dispatchers.Default) { getAnalysis() }
            OrionLogger.debug(crashJsonObj.toString())

            val appMetricJson = withContext(Dispatchers.Default) { AppMetrics.getAppMetrics() }
            val runtimeMetrics = withContext(Dispatchers.Default) { EdOrion.getInstance().getRuntimeMetrics() }
            val objCommonFunctions = CommonFunctions()

            val metricJson = objCommonFunctions.mergeJSONObjects(appMetricJson, runtimeMetrics)
            val finalJson = objCommonFunctions.mergeJSONObjects(metricJson, crashJsonObj)
            finalJson.accumulate("epoch", System.currentTimeMillis())

            withContext(Dispatchers.IO) {
                val sendObj = SendData()
                sendObj.coronaGo(finalJson)
            }
        }
    }


    private fun getAnalysis(): JSONObject {
        val stackBuilder = StringBuilder()
        val jsonObject = JSONObject()

        val locationOfCrash: String
        stackBuilder.append(throwable.localizedMessage)
        stackBuilder.append("\n")
        stackBuilder.append(stackTrace(throwable.stackTrace))
        stackBuilder.append("\n")

        if (throwable.cause != null) {
            stackBuilder.append("Caused By: ")
            val stackTrace = throwable.cause!!.stackTrace
            locationOfCrash = getCrashOriginatingClass(stackTrace)
            stackBuilder.append(stackTrace(stackTrace))
        } else {
            locationOfCrash = getCrashOriginatingClass(throwable.stackTrace)
        }

        // Enrich the crash report
        jsonObject.accumulate("activity", activityName)
        jsonObject.accumulate("crashType", throwable::class.java.simpleName)
        jsonObject.accumulate("crashLocation", locationOfCrash)
        jsonObject.accumulate("localizedMessage", throwable.localizedMessage)
        jsonObject.accumulate("stackTrace", stackBuilder.toString())
        jsonObject.accumulate("threadInfo", getThreadInformation())
        jsonObject.accumulate("screenContext", getScreenContext())
        jsonObject.accumulate("action", generateActionableInsight(throwable))
        jsonObject.accumulate("environment", getEnvironmentVariables())
        val context = EdOrion.getInstance().getApplication()?.applicationContext
        if (context != null) {
            jsonObject.put("networkState", getNetworkState(context))
        } else {
            jsonObject.put("networkState", "NA") // Fallback if context is unavailable
        }
        jsonObject.accumulate("lastUserInteraction", AppRuntimeMetrics().getLastUserInteraction())
        return jsonObject
    }

    private fun getCrashOriginatingClass(stackTraceElements: Array<StackTraceElement>): String {
        if (stackTraceElements.isNotEmpty()) {
            val stackTraceElement = stackTraceElements[0]
            return "${stackTraceElement.className}:${stackTraceElement.lineNumber}"
        }
        return "Unknown"
    }

    private fun stackTrace(stackTrace: Array<StackTraceElement>): String {
        return stackTrace.joinToString("\n") { "at $it" }
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

    private fun getScreenContext(): String {
        return try {
            "Activity: $activityName"
        } catch (e: Exception) {
            OrionLogger.debug("Error getting screen context: ${e.message}")
            "Unknown"
        }
    }

    private fun generateActionableInsight(throwable: Throwable): String {
        return when (throwable) {
            is NullPointerException -> "Check if the object is initialized before use. Add null checks as needed."
            is IndexOutOfBoundsException -> "Ensure indices are within bounds. Validate list size before accessing it."
            is IllegalArgumentException -> "Verify the arguments passed to the method. Refer to the API documentation."
            is ClassNotFoundException -> "Ensure the class is included in the build and properly referenced."
            is ArithmeticException -> "Check for division by zero or invalid arithmetic conditions."
            is NumberFormatException -> "Validate input data before parsing it into a number format."
            is IOException -> "Check file paths, permissions, or network connectivity."
            is OutOfMemoryError -> "Optimize memory usage or use smaller data structures where applicable."
            is SecurityException -> "Verify the app's permissions or security policies."
            is IllegalStateException -> "Review the state of the object before calling the method."
            else -> "Refer to the stack trace and logs for more details about the issue."
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
            put("mem", memoryPercentage) // Replace freeMemory and totalMemory

        }
    }

    fun getNetworkState(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // ✅ API 23+
            val activeNetwork = connectivityManager.activeNetwork ?: return "NA"
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "NA"

            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "data"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "eth"
                else -> "Other"
            }
        } else {
            // ✅ API 21-22 (fallback)
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