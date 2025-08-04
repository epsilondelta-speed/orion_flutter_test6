package co.epsilondelta.orion_flutter.orion.util

import kotlin.math.roundToInt

object TrafficSampling {


    private var samplingPercent:Int = 100;

    fun getSamplingValue():Int
    {
        return this.samplingPercent;
    }

    fun setSamplingValue(newSamplingVal:Int)
    {
        val sampl:Int = this.samplingPercent ;
       // OrionLogger.debug("existing sampling rate {$sampl}")
        //OrionLogger.debug("new sampling rate $newSamplingVal")
        this.samplingPercent = newSamplingVal;
        val sampl1:Int = this.samplingPercent ;
        //OrionLogger.debug("after update {$sampl1}")
    }

    fun isSampleValid():Boolean
    {

        return if(this.samplingPercent > 99 )
        {
            true
        }
        else {
            val currentTimestamp = System.currentTimeMillis()
            val op = currentTimestamp / this.samplingPercent - (currentTimestamp / this.samplingPercent).toDouble().roundToInt();
            val j= (currentTimestamp % samplingPercent).toInt()
            j !== 0
        }


    }


}
