package com.stefanosiano.powerful_libraries.sama.viewModel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.logVerbose
import com.stefanosiano.powerful_libraries.sama.tryOrPrint
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserver
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserverImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base class for ViewModels.
 * It will contain the fields used by the databinding and the logic of the data in the layouts.
 */
open class SamaViewModel<A : VmAction>
/** Initializes the LiveData of the response. */
protected constructor() : ViewModel(), CoroutineScope, SamaObserver by SamaObserverImpl() {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** Flag to check if actions were stopped and should not be sent to the activity. */
    private var actionsStopped = false

    /** Last [VmAction.VmActionSticky] sent to the activity. */
    private var lastStickyAction: A? = null

    /** LiveData of the response the ViewModel sends to the observer (activity/fragment). */
    private var liveAction: MediatorLiveData<A> = MediatorLiveData()

    /** Flag to know if this [SamaViewModel] is initialized. Used to check if [onFirtstTime] should be called. */
    internal var isInitialized = AtomicBoolean(false)

    init {
        initObserver(this)
    }

    /** Sends the action to the active observer. */
    protected fun sendAction(action: A) = liveAction.postValue(action)

    /** Clear the liveData observer (if any). */
    override fun onCleared() {
        super.onCleared()
        logVerbose("onCleared")
        destroyObserver()
        coroutineContext.cancel()
    }

    /** Clear the liveData observer (if any). */
    internal fun stopObserving() {
        logVerbose("stopObserving")
        stopVmActions()
        stopObserver()
        liveAction.postValue(null)
    }

    /** Clear the liveData observer (if any). */
    internal fun restartObserving() {
        logVerbose("restartObserving")
        startVmActions()
        startObserver()
    }

    /** Observes the action of the ViewModel. Call it on Ui thread. */
    fun onVmAction(lifecycleOwner: LifecycleOwner, observer: (vmAction: A) -> Unit) {
        liveAction.observe(lifecycleOwner) {
            if (!isActive) return@observe
            if (actionsStopped) return@observe
            if (it == null) {
                lastStickyAction = null
                return@observe
            }
            logVerbose("Sending to activity: $it")
            tryOrPrint { observer(it) }
            if (it is VmAction.VmActionSticky) {
                lastStickyAction = it
            }
            liveAction.postValue(null)
        }
        lastStickyAction?.let { observer(it) }
    }

    /** Stop sending actions to the observing activity. To resume them call [startVmActions]. */
    fun stopVmActions() { actionsStopped = true }

    /** Start sending actions to the observing activity. To stop them call [stopVmActions]. */
    fun startVmActions() { actionsStopped = false }

    /** Clear any retained [VmAction.VmActionSticky]. */
    fun clearStickyAction() { lastStickyAction = null }
}

/** Executes [f] only once. If this is called multiple times, it will have no effect. */
@Synchronized fun <T : SamaViewModel<*>> T.onFirstTime(f: T.() -> Unit) {
    if (!isInitialized.getAndSet(true)) this.f()
}

/** Class containing action and data sent from the ViewModel to its observers. */
open class VmResponse<A : VmAction> (
    /** Specifies what the response is about . */
    val action: A,
    /** Optional data provided by the action . */
    val data: Any?
) {
    override fun toString() = "VmResponse{ action= $action, data=$data }"
}

/** Interface that indicates the action sent by the ViewModel. */
interface VmAction {
    /**
     * Indicates that this [VmAction] should be retained when activity is recreated.
     * Any new [VmActionSticky] overrides the previous one, so that only 1 action will be retained.
     * If an action is sent and the device is rotated,
     *  when the activity restarts it needs to get that action again.
     */
    interface VmActionSticky
}
