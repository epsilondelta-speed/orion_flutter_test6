package co.epsilondelta.orion_flutter.orion.network
import co.epsilondelta.orion_flutter.orion.util.CurrentActivityTracker
import co.epsilondelta.orion_flutter.orion.util.OrionLogger

object NetworkRequestLogger {

    @JvmStatic
    fun trackRequest(
        method: String,
        url: String,
        statusCode: Int,
        startTime: Long,
        endTime: Long,
        duration: Long,
        payloadSize: Long? = null,
        contentType: String? = null,
        uiImpact: Boolean=true,
        errorMessage: String? = null
    ) {
        val payloadStr = payloadSize?.let { "$it bytes" } ?: "N/A"
        val statusStr = if (statusCode == -1) "❌ ERROR" else statusCode.toString()
        val errorLog = errorMessage?.let { " | Error: $it" } ?: ""
        val typeInfo = contentType?.let { " | Type: $it" } ?: ""

        println("📦 $method $url [$statusStr] in ${duration}ms | Size: $payloadStr$typeInfo$errorLog")

        val activityName = CurrentActivityTracker.getCurrentActivityName()
        OrionLogger.debug( "✅ Tracked Network Request for Activity via Orion Lib: $activityName")
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
                contentType = contentType,
                uiImpact=uiImpact,
                errorMessage = errorMessage
            )
        )
    }

    @JvmStatic
    fun log(
        method: String,
        url: String,
        statusCode: Int,
        startTime: Long,
        endTime: Long,
        duration: Long,
        payloadSize: Long? = null,
        errorMessage: String? = null
    ) {
        val payloadStr = payloadSize?.let { "$it bytes" } ?: "N/A"
        val statusStr = if (statusCode == -1) "❌ ERROR" else statusCode.toString()
        val errorLog = errorMessage?.let { " | Error: $it" } ?: ""

        println("📦 $method $url [$statusStr] in ${duration}ms | Size: $payloadStr$errorLog")
    }

    // Overload for convenience if full object is passed
    @JvmStatic
    fun log(info: NetworkRequestInfo) {
        log(
            method = info.method,
            url = info.url,
            statusCode = info.statusCode,
            startTime = info.startTime,
            endTime = info.endTime,
            duration = info.duration,
            payloadSize = info.payloadSize,
            errorMessage = info.errorMessage
        )
    }
}