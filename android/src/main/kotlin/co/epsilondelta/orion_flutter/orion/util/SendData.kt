package co.epsilondelta.orion_flutter.orion.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection
import co.epsilondelta.orion_flutter.orion.util.OrionLogger

class SendData {

    private val beaconUrl: String = "https://www.ed-sys.net/oriData"

    // Define a CoroutineScope tied to the lifecycle of SendData
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun clear() {
        job.cancel()
    }

    fun coronaGo(data: JSONObject) {
        val appContext = OrionConfig.getContext()



        val network = checkNetworkConnection(appContext!!)
        val networkType = getNetworkType(appContext!!)

        data.put("netType", networkType) // Add network type to JSON
        data.put("libVer", "21")

        val sessionId = SessionManager.getSessionId()
        data.put("sesId", sessionId) // Add sessionId to the payload
        val startupType = StartTypeTracker.getStartType();
        data.put("startupType", startupType)

        OrionLogger.debug(  data.toString())
        if (network != ConnectivityMode.NONE && SamplingManager.shouldSendSample()) {
            scope.launch {
                SessionManager.updateSessionTimestamp()
                val result = httpsPost(beaconUrl, data)
                OrionLogger.debug("Connected to internet $result")
            }
        } else {
            OrionLogger.debug("Not Connected to Internet or no sampling")
        }
    }

    fun getBeaconUrl(): String = beaconUrl

    //this is not in use
    private fun setSamplingData(samplingValueReceived: String) {
        val passedVal: Int = samplingValueReceived.replace("\n", "").toIntOrNull() ?: -1
        if (passedVal > -1 && TrafficSampling.getSamplingValue() != passedVal) {
            OrionLogger.debug("Setting new sampling value: $passedVal")
            TrafficSampling.setSamplingValue(passedVal)
        }
    }

    enum class ConnectivityMode {
        NONE,
        WIFI,
        MOBILE,
        OTHER
    }

    private fun checkNetworkConnection(context: Context): ConnectivityMode {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        cm?.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getNetworkCapabilities(activeNetwork)?.run {
                    return when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectivityMode.WIFI
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectivityMode.MOBILE
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectivityMode.OTHER
                        else -> ConnectivityMode.NONE
                    }
                }
            }
        }
        return ConnectivityMode.NONE
    }

    private fun getNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        cm?.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getNetworkCapabilities(activeNetwork)?.run {
                    return when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "data"
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "eth"
                        else -> "NA"
                    }
                }
            }
        }
        return "Unknown"
    }

    @Throws(IOException::class)
    private fun setPostRequestContent(conn: HttpURLConnection, jsonObject: JSONObject) {
        val os = conn.outputStream
        val gzipOutputStream = GZIPOutputStream(os)
        val writer = BufferedWriter(OutputStreamWriter(gzipOutputStream, "UTF-8"))
        writer.write(jsonObject.toString())
        writer.flush()
        writer.close()
        os.close()
    }

    @Throws(IOException::class, JSONException::class, Exception::class)
    private suspend fun httpsPost(myUrl: String, data: JSONObject): JSONObject {
        val respObject = JSONObject()
        respObject.accumulate("cid", myUrl)
        respObject.accumulate("cid", OrionConfig.getCompanyId())
        try {
            withContext(Dispatchers.IO) {
                val url = URL(myUrl)
                val conn = url.openConnection() as HttpsURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setRequestProperty("Content-Encoding", "gzip") // Specify GZIP encoding
                conn.setRequestProperty("cid", OrionConfig.getCompanyId())
                conn.doOutput = true

                // Add compressed content
                setPostRequestContent(conn, data)

                conn.connect()

                val respCode: Int = conn.responseCode
                OrionLogger.debug("Http Resp Code: $respCode")

                if (respCode == HttpsURLConnection.HTTP_OK) {
                    val stringBuilder = StringBuilder()
                    conn.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { stringBuilder.append(it).append("\n") }
                    }
                    val output = stringBuilder.toString()
                   
                    respObject.accumulate("respCode", HttpsURLConnection.HTTP_OK)
                    respObject.accumulate("val", output)
                    //setSamplingData(output)
                } else {
                    respObject.accumulate("respCode", "Fail")
                    respObject.accumulate("val", "")
                }
            }
        } catch (e: Exception) {
            OrionLogger.debug("Error: ${e.message}")
            respObject.accumulate("respCode", "Fail")
            respObject.accumulate("val", e.toString())
        }
        return respObject
    }
}