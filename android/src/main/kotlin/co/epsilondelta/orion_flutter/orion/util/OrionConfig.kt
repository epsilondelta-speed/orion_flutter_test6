package co.epsilondelta.orion_flutter.orion.util
import co.epsilondelta.orion_flutter.orion.util.OrionLogger

import android.app.Application
import android.util.Log

internal object OrionConfig {

     var cid: String = ""
     var pid: String = ""
     var appContext : Application? = null


    fun getContext():Application? {
        return appContext;
    }

    fun getCompanyId():String? {
       // OrionLogger.debug(  "get config cid $cid")
        return cid;
    }

    fun getProductId():String? {
        return pid;
    }



}

