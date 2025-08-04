package co.epsilondelta.orion_flutter.orion.metrics

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import co.epsilondelta.orion_flutter.orion.util.OrionConfig
import co.epsilondelta.orion_flutter.orion.util.OrionLogger
import org.json.JSONObject

object AppMetrics {

    private const val TAG = "AppMetrics"

    @Volatile
    private var initialized = false

    private var appVersion = "unknown"
    private var appPackageName = "unknown"
    private var androidSdkVersion = 0
    private var androidReleaseName = "unknown"
    private var model = "unknown"
    private var brand = "unknown"
    private var manufacture = "unknown"

    init {
        OrionLogger.debug( "AppMetrics class invoked")
    }

    @Synchronized
    fun setAppMetrics(context: Context) {
        if (initialized) {
            OrionLogger.debug( "AppMetrics is already initialized")
            return
        }
        OrionLogger.debug( "Initializing static metrics")
        try {
            getAppInfo(context)
            getAndroidDetails()
            initialized = true
        } catch (e: Exception) {
            OrionLogger.error( "Error initializing AppMetrics: ${e.message}")
        }
    }

    fun getAppMetrics(): JSONObject {
        return JSONObject().apply {
            val companyId = OrionConfig.getCompanyId() ?: "unknown"
            put("cid", companyId)
            if (companyId == "unknown") Log.w(TAG, "Company ID is unknown")

            val productId = OrionConfig.getProductId() ?: "unknown"
            put("pid", productId)
            if (productId == "unknown") Log.w(TAG, "Product ID is unknown")

            put("appVer", appVersion)
            put("appPkgName", appPackageName)
            put("sdkVer", androidSdkVersion)
            put("releaseName", androidReleaseName)
            put("model", model)
            put("brand", brand)
            put("manufacture", manufacture)
        }
    }


    private fun getAppInfo(context: Context) {
        try {
            val info = context.packageManager?.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            appVersion = info?.versionName ?: "unknown"
            appPackageName = info?.packageName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            OrionLogger.error( "Error fetching app info: ${e.message}")
        }
    }

    private fun getAndroidDetails() {
        try {
            androidReleaseName = Build.VERSION.RELEASE ?: "unknown"
            androidSdkVersion = Build.VERSION.SDK_INT
            manufacture = Build.MANUFACTURER ?: "unknown"
            brand = Build.BRAND ?: "unknown"
            model = Build.MODEL ?: "unknown"
        } catch (e: Exception) {
            OrionLogger.error( "Error fetching Android details: ${e.message}")
        }
    }
}