package co.epsilondelta.orion_flutter.orion.util

import android.app.Activity
import java.lang.ref.WeakReference

object CurrentActivityTracker {
    private var currentActivityRef: WeakReference<Activity>? = null
    private var appState: AppState = AppState.LAUNCHING

    //App just launched, no activity yet - "AppLaunched"
    //App in background (no visible activity) - "AppBackground"

    enum class AppState {
        LAUNCHING,
        FOREGROUND,
        BACKGROUND
    }

    fun setCurrentActivity(activity: Activity?) {
        currentActivityRef = if (activity != null) WeakReference(activity) else null
        appState = if (activity != null) AppState.FOREGROUND else AppState.BACKGROUND
    }

    fun appMovedToBackground() {
        appState = AppState.BACKGROUND
    }

    fun appMovedToForeground() {
        appState = AppState.FOREGROUND
    }

    fun getCurrentActivityName(): String {
        return when {
            currentActivityRef?.get() != null -> currentActivityRef?.get()?.javaClass?.simpleName ?: "Unknown"
            appState == AppState.LAUNCHING -> "AppLaunched"
            appState == AppState.BACKGROUND -> "AppBackground"
            else -> "Unknown"
        }
    }
}