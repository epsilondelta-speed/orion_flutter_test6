package co.epsilondelta.orion_flutter.orion



import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import co.epsilondelta.orion_flutter.orion.event.ActivityEvent
import co.epsilondelta.orion_flutter.orion.event.FragmentEvent
import co.epsilondelta.orion_flutter.orion.util.LifecycleListener
import co.epsilondelta.orion_flutter.orion.util.OrionLogger

class OrionLifecycle private constructor(private val builder: Builder) {

    private var listener: Listener? = null
    private var activityEvents: List<String> = listOf()
    private var fragmentEvents: List<String> = listOf()
    private var application: Application? = null

    init {
        application = builder.application
        listener = builder.listener
        activityEvents = builder.activityEvents
        fragmentEvents = builder.fragmentEvents


        INSTANCE = this
    }

    class Builder(internal val application: Application) {

        internal var listener: Listener? = null
        internal var activityEvents = listOf(
                ActivityEvent.CREATE,
                ActivityEvent.START,
                ActivityEvent.RESUME,
                ActivityEvent.PAUSE,
                ActivityEvent.STOP,
                ActivityEvent.SAVE_INSTANCE_STATE,
                ActivityEvent.DESTROY
        )
        internal var fragmentEvents = listOf(
                FragmentEvent.PRE_ATTACH,
                FragmentEvent.ATTACH,
                FragmentEvent.CREATE,
                FragmentEvent.ACTIVITY_CREATE,
                FragmentEvent.PRE_CREATE,
                FragmentEvent.VIEW_CREATE,
                FragmentEvent.START,
                FragmentEvent.RESUME,
                FragmentEvent.PAUSE,
                FragmentEvent.STOP,
                FragmentEvent.SAVE_INSTANCE_STATE,
                FragmentEvent.DESTROY,
                FragmentEvent.VIEW_DESTROY,
                FragmentEvent.DETACH
        )



        fun setOrionListener(listener: Listener): Builder {
            this.listener = listener
            return this
        }

        fun setActivityEventFilter(events: List<String>): Builder {
            this.activityEvents = events
            return this
        }

        fun setFragmentEventFilter(events: List<String>): Builder {
            this.fragmentEvents = events
            return this
        }

        fun buildLifecycle() = OrionLifecycle(this)
    }

    fun setJotterListener(listener: Listener) {
        this.listener = listener
    }

    fun startLifecycleListening() {
        LifecycleListener.register(
                application = this.builder.application,
                listener = this.listener,
                activityFilter = this.activityEvents,
                fragmentFilter = this.fragmentEvents
        )
        if (this.listener == null) {
            OrionLogger.debug(Constant.LISTENER_MESSAGE)
        }
    }

    interface Listener {
        fun onReceiveActivityEvent(
                activity: Activity,
                @ActivityEvent event: String,
                bundle: Bundle? = null
        )

        fun onReceiveFragmentEvent(
                fragment: Fragment,
                context: Context? = null,
                @FragmentEvent event: String,
                bundle: Bundle? = null
        )
    }

    companion object {
        @JvmStatic
        @Volatile
        private var INSTANCE: OrionLifecycle? = null

        @Synchronized
        fun getInstance(): OrionLifecycle = INSTANCE
                ?: throw RuntimeException(Constant.INSTANCE_MESSAGE)

        @Synchronized
        fun getDefaultInstance(application: Application): OrionLifecycle {
            if (INSTANCE == null) {
                INSTANCE = Builder(application).buildLifecycle()
            }
            return INSTANCE!!
        }

        private const val TAG = "OrionLifeCycle"
    }

    private object Constant {
        internal const val INSTANCE_MESSAGE =
                "OrionLifecycle isn't initialized yet. Please use OrionLifecycle.Builder(application)!"
        internal const val LISTENER_MESSAGE =
                "Listener1111 not found, you can't receive callbacks, please set it first via Builder or setOrionLifeCycleListener in util  API!"
    }
}
