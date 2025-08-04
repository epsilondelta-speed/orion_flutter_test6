package co.epsilondelta.orion_flutter.orion.event


import androidx.annotation.StringDef
import co.epsilondelta.orion_flutter.orion.event.ActivityEvent.Companion.CREATE
import co.epsilondelta.orion_flutter.orion.event.ActivityEvent.Companion.DESTROY
import co.epsilondelta.orion_flutter.orion.event.ActivityEvent.Companion.PAUSE
import co.epsilondelta.orion_flutter.orion.event.ActivityEvent.Companion.RESUME
import co.epsilondelta.orion_flutter.orion.event.ActivityEvent.Companion.SAVE_INSTANCE_STATE
import co.epsilondelta.orion_flutter.orion.event.ActivityEvent.Companion.START
import co.epsilondelta.orion_flutter.orion.event.ActivityEvent.Companion.STOP

@Retention(AnnotationRetention.SOURCE)
@StringDef(
        CREATE,
        START,
        RESUME,
        PAUSE,
        STOP,
        SAVE_INSTANCE_STATE,
        DESTROY
)
annotation class ActivityEvent {
    companion object {
        const val CREATE = "CREATE"
        const val START = "START"
        const val RESUME = "RESUME"
        const val PAUSE = "PAUSE"
        const val STOP = "STOP"
        const val SAVE_INSTANCE_STATE = "SAVE_INSTANCE_STATE"
        const val DESTROY = "DESTROY"
    }
}
