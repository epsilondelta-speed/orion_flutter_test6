package co.epsilondelta.orion_flutter.orion.util

import org.json.JSONObject

class CommonFunctions {


    fun mergeJSONObjects(Obj1: JSONObject, Obj2: JSONObject): JSONObject {
        val merged = JSONObject()
        val objs = arrayOf<JSONObject>(Obj1, Obj2)
        for (obj in objs) {
            val it: Iterator<*> = obj.keys()
            while (it.hasNext()) {
                val key = it.next() as String
                merged.put(key, obj[key])
            }
        }

        return merged;
    }
}
