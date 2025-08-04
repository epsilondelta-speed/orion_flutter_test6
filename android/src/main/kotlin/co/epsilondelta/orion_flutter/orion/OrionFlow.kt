package co.epsilondelta.orion_flutter.orion

import co.epsilondelta.orion_flutter.orion.event.ActivityEvent
import co.epsilondelta.orion_flutter.orion.metrics.AppMetrics
import co.epsilondelta.orion_flutter.orion.util.CommonFunctions
import co.epsilondelta.orion_flutter.orion.util.OrionLogger
import co.epsilondelta.orion_flutter.orion.util.SendData

class OrionFlow {

    fun startActiviyCalculation(activityName: String, activityEvent: String) {
        if (activityEvent == ActivityEvent.CREATE) {
            // Log the start of activity creation for debugging
            OrionLogger.debug("Starting metrics calculation for activity: $activityName")

            // Retrieve app and runtime metrics
            val appMetricJson = AppMetrics.getAppMetrics()
            val runtimeMetrics = EdOrion.getInstance().getRuntimeMetrics()

            // Merge the metrics
            val objCommonFunctions = CommonFunctions()
            val finalJson = objCommonFunctions.mergeJSONObjects(appMetricJson, runtimeMetrics)
            finalJson.accumulate("activityName", activityName)
            finalJson.accumulate("epoch", System.currentTimeMillis())

            // Log the merged metrics for debugging
            //OrionLogger.debug("Final Metrics for $activityName: $finalJson")

            // Send the merged metrics to the server
            //commented beacuase we are now using diffrent lifecycle
            //val sendObj = SendData()
            //sendObj.coronaGo(finalJson)
        }
    }
}
