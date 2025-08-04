package co.epsilondelta.orion_flutter.orion.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

object OrionOkHttpInterceptor {

    @JvmStatic
    fun inject(builder: OkHttpClient.Builder) {
        builder.addInterceptor(OrionOkHttpInterceptorImpl())
    }

    private class OrionOkHttpInterceptorImpl : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val start = System.currentTimeMillis()
            try {
                val response = chain.proceed(request)
                val end = System.currentTimeMillis()
                val contentType = response.header("Content-Type") ?: "unknown"
                val requestType = when {
                    contentType.startsWith("image/") -> "image"
                    contentType.contains("json", ignoreCase = true) -> "json"
                    contentType.contains("html", ignoreCase = true) -> "html"
                    contentType.contains("text", ignoreCase = true) -> "text"
                    else -> "other"
                }

                    NetworkRequestLogger.trackRequest(
                        method = request.method,
                        url = request.url.toString(),
                        statusCode = response.code,
                        startTime = start,
                        endTime = end,
                        duration = end - start,
                        contentType = requestType,
                        uiImpact = isUiImpactingRequest(request, response),
                        payloadSize = response.body?.contentLength()
                    )


                return response
            } catch (e: Exception) {
                val end = System.currentTimeMillis()
                NetworkRequestLogger.log(
                    method = request.method,
                    url = request.url.toString(),
                    statusCode = -1,
                    startTime = start,
                    endTime = end,
                    duration = end - start,
                    errorMessage = e.message
                )
                throw e
            }
        }
    }

    private fun isUiImpactingRequest(request: Request, response: Response): Boolean {
        val url = request.url.toString()
        val host = request.url.host
        val contentType = response.header("Content-Type") ?: ""
        val size = response.body?.contentLength() ?: 0

        val ignoredHosts = listOf(
            "google-analytics.com",
            "firebaseinstallations.googleapis.com",
            "crashlytics",
            "sentry.io",
            "app-measurement.com"
        )

        val telemetryHeaders = listOf("X-Firebase-", "X-Sentry-", "Crashlytics", "X-Goog-")

        val userAgent = request.header("User-Agent") ?: ""

        // Logic to skip background/telemetry
        if (ignoredHosts.any { host.contains(it) }) return false
        if (telemetryHeaders.any { header -> userAgent.contains(header) }) return false
        if (contentType.contains("json") && size < 1000) return false

        // Consider UI-impacting if it's image, HTML, or large JSON
        return contentType.contains("image") ||
                contentType.contains("html") ||
                (contentType.contains("json") && size >= 1000)
    }
}