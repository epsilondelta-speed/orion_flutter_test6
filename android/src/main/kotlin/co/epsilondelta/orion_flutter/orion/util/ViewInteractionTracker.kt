package co.epsilondelta.orion_flutter.orion.util

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import co.epsilondelta.orion_flutter.orion.metrics.AppRuntimeMetrics
import co.epsilondelta.orion_flutter.orion.util.OrionLogger

object ViewInteractionTracker {

    private const val TAG = "ViewInteractionTracker"
    private var appRuntimeMetrics: AppRuntimeMetrics? = null

    fun initialize(runtimeMetrics: AppRuntimeMetrics) {
        appRuntimeMetrics = runtimeMetrics
    }

    /**
     * Recursively wraps all interactive view listeners in the given root view.
     *
     * @param rootView The root view to start tracking.
     */
    fun wrapAllViewListeners(rootView: View?) {
        if (rootView == null) return

        if (rootView is ViewGroup) {
            for (i in 0 until rootView.childCount) {
                val child = rootView.getChildAt(i)
                wrapAllViewListeners(child)
            }
        }

        // Wrap specific interactive views
        when (rootView) {
            is Button -> wrapButtonListener(rootView)
            is ScrollView -> wrapScrollViewListener(rootView)
            else -> wrapGenericViewListener(rootView)
        }
    }

    /**
     * Wraps click listeners for buttons.
     */
    private fun wrapButtonListener(button: Button) {
        val originalListener = getOnClickListener(button)
        button.setOnClickListener { view ->
            OrionLogger.debug( "Button clicked: ${view.resources.getResourceEntryName(view.id)}")
            appRuntimeMetrics?.recordUserInteraction("Button Click: ${view.resources.getResourceEntryName(view.id)}")
            originalListener?.onClick(view)
        }
    }

    /**
     * Wraps scroll listeners for scroll views.
     */
    private fun wrapScrollViewListener(scrollView: ScrollView) {
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            OrionLogger.debug( "ScrollView scrolled: ${scrollView.resources.getResourceEntryName(scrollView.id)}")
            appRuntimeMetrics?.recordUserInteraction("ScrollView Scroll: ${scrollView.resources.getResourceEntryName(scrollView.id)}")
        }
    }

    /**
     * Wraps generic view listeners if applicable.
     */
    private fun wrapGenericViewListener(view: View) {
        val originalListener = getOnClickListener(view)
        view.setOnClickListener { v ->
            OrionLogger.debug( "View clicked: ${v.id}")
            appRuntimeMetrics?.recordUserInteraction("View Click: ${v.id}")
            originalListener?.onClick(v) // Invoke the original listener if it exists
        }
    }

    /**
     * Uses reflection to get the original click listener of a view.
     */
    private fun getOnClickListener(view: View): View.OnClickListener? {
        return try {
            val listenerInfoField = View::class.java.getDeclaredField("mListenerInfo")
            listenerInfoField.isAccessible = true
            val listenerInfo = listenerInfoField.get(view) ?: return null // Check for null

            val onClickListenerField = listenerInfo.javaClass.getDeclaredField("mOnClickListener")
            onClickListenerField.isAccessible = true
            onClickListenerField.get(listenerInfo) as? View.OnClickListener
        } catch (e: Exception) {
            OrionLogger.debug(  "Error getting OnClickListener: ${e.message}")
            null
        }
    }
}