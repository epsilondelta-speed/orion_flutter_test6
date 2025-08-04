package co.epsilondelta.orion_flutter.orion.util

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object SessionManager {

    private const val PREFS_NAME = "EdOrionSessionPrefs"
    private const val SESSION_ID_KEY = "sessionId"
    private const val LAST_UPDATED_KEY = "lastUpdated"
    private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000 // 30 minutes

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSessionId(): String {
        val now = System.currentTimeMillis()
        val lastUpdated = prefs.getLong(LAST_UPDATED_KEY, 0L)
        val sessionId = prefs.getString(SESSION_ID_KEY, null)

        return if (sessionId == null || now - lastUpdated > SESSION_TIMEOUT_MS) {
            // Create a new session ID if none exists or session expired
            val newSessionId = UUID.randomUUID().toString()
            saveSessionId(newSessionId)
            newSessionId
        } else {
            sessionId
        }
    }

    private fun saveSessionId(sessionId: String) {
        prefs.edit()
            .putString(SESSION_ID_KEY, sessionId)
            .putLong(LAST_UPDATED_KEY, System.currentTimeMillis())
            .apply()
    }

    fun updateSessionTimestamp() {
        prefs.edit().putLong(LAST_UPDATED_KEY, System.currentTimeMillis()).apply()
    }
}