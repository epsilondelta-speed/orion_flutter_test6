package co.epsilondelta.orion_flutter.orion.network

data class NetworkRequestInfo(
    val url: String,
    val method: String,
    val statusCode: Int,
    val startTime: Long,
    val endTime: Long,
    val duration: Long, // ✅ Ensure this is present
    val payloadSize: Long? = null, // ✅ Ensure this is present
    val contentType: String? = null,
    val uiImpact: Boolean = true,
    val errorMessage: String? = null
)