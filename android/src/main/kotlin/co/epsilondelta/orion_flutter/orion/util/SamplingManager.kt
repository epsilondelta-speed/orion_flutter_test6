package co.epsilondelta.orion_flutter.orion.util

import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

object SamplingManager {
    private const val CDN_URL = "https://cdn.epsilondelta.co/orion/conf.json"
    private var samplingValue: Int = 100
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(cid: String) {
        val companyId = cid.ifEmpty { "default" }
        fetchSamplingValueAsync(companyId)
        scope.launch {
            while (isActive) {
                delay(TimeUnit.MINUTES.toMillis(15))
                fetchSamplingValueAsync(companyId)
            }
        }
    }

    private fun fetchSamplingValueAsync(cid: String) {
        scope.launch {
            try {
                val request = Request.Builder().url(CDN_URL).build()
                val response = withContext(Dispatchers.IO) {
                    OkHttpClient().newCall(request).execute()
                }
                response.body?.string()?.let { responseBody ->
                    val jsonObject = JSONObject(responseBody)
                    samplingValue = jsonObject.optInt(cid, 100)
                } ?: run { samplingValue = 100 }
            } catch (e: Exception) {
                samplingValue = 100 // Default on failure
            }
        }
    }

    fun shouldSendSample(): Boolean {
        val randomValue = ThreadLocalRandom.current().nextInt(100) + 1
        return randomValue <= samplingValue
    }

    fun getSamplingValue(): Int = samplingValue
}
