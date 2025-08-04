package co.epsilondelta.orion_flutter.orion.metrics

import android.app.Application
import android.provider.Settings
import android.content.Context
import android.os.BatteryManager
import android.os.Environment
import co.epsilondelta.orion_flutter.orion.util.OrionLogger
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AppRuntimeMetrics {

    @Volatile
    private var lastUserInteraction: String? = null
    private var appContext: Context? = null
    private val lock = ReentrantLock()

    /**
     * Initialize runtime metrics with application context.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Collect and return runtime metrics as JSON.
     */
    fun getRuntimeMetrics(): JSONObject {
        val context = appContext
        if (context == null) {
           // OrionLogger.debug("Context is null, unable to retrieve runtime metrics.")
            return JSONObject().apply { put("error", "Context unavailable") }
        }

        val deviceDimensions = getDeviceDimensions()
        return JSONObject().apply {
            put("memoryUsage", getMemoryUsagePercentage())
            put("batteryPercentage", getBatteryPercentage())
            put("diskSpaceUsage", getDiskSpaceUsagePercentage())
            put("userSessionId", getAndroidId())
            put("locale", getLocale())
            put("isDeviceRooted", isDeviceRooted())
            put("lastUserInteraction", getLastUserInteraction())
            put("screenResolution", getScreenResolution())
            put("DeviceDimensions", deviceDimensions)
        }
    }

    private fun getMemoryUsagePercentage(): Int {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return if (runtime.totalMemory() > 0) ((usedMemory * 100) / runtime.totalMemory()).toInt() else 0
    }

    private fun getBatteryPercentage(): Int {
        val context = appContext ?: return -1
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun getDiskSpaceUsagePercentage(): Int {
        return try {
            val stat = android.os.StatFs(Environment.getDataDirectory().path)
            val totalSpace = stat.totalBytes
            val availableSpace = stat.availableBytes
            val usedSpace = totalSpace - availableSpace
            ((usedSpace.toDouble() / totalSpace) * 100).toInt()
        } catch (e: Exception) {
            OrionLogger.debug("Error calculating disk space: ${e.message}")
            -1
        }
    }

    private fun getUserSessionId(): String {
        val context = appContext ?: return "unknown"
        val prefs = context.getSharedPreferences("EdOrionPrefs", Context.MODE_PRIVATE)
        return prefs.getString("userSessionId", UUID.randomUUID().toString()).also {
            prefs.edit().putString("userSessionId", it).apply()
        } ?: "unknown"
    }

    private fun getAndroidId(): String {
        val context = appContext ?: return "unknown"
        val prefs = context.getSharedPreferences("EdOrionPrefs", Context.MODE_PRIVATE)

        // Check if Android ID is already saved in shared preferences
        return prefs.getString("androidId", null) ?: run {
            // Retrieve the Android ID from the system
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

            // Save the Android ID to shared preferences gh
            prefs.edit().putString("androidId", androidId).apply()
            androidId
        }
    }

    private fun getLocale(): String {
        return Locale.getDefault().toString()
    }

    private fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    fun getLastUserInteraction(): String {
        return lock.withLock { lastUserInteraction ?: "unknown" }
    }

    fun recordUserInteraction(interaction: String) {
        lock.withLock {
            lastUserInteraction = interaction
        }
    }

    private fun getScreenResolution(): String {
        val context = appContext ?: return "unknown"
        val metrics = context.resources.displayMetrics
        return "${metrics.widthPixels}x${metrics.heightPixels}"
    }

    private fun getDeviceDimensions(): JSONObject {
        val context = appContext ?: return JSONObject().apply { put("error", "Context unavailable") }

        val metrics = context.resources.displayMetrics

        val deviceWidth = metrics.widthPixels // Device width in pixels
        val deviceHeight = metrics.heightPixels // Device height in pixels

        val viewportWidth = deviceWidth / metrics.density // Width in dp
        val viewportHeight = deviceHeight / metrics.density // Height in dp

        return JSONObject().apply {
            put("deviceWidth", deviceWidth) // Physical width in pixels
            put("deviceHeight", deviceHeight) // Physical height in pixels
            put("viewportWidth", viewportWidth.toDouble()) // Logical width in dp
            put("viewportHeight", viewportHeight.toDouble()) // Logical height in dp
            put("densityDpi", metrics.densityDpi) // DPI
            put("density", metrics.density.toDouble()) // Pixel scaling factor
        }
    }
}