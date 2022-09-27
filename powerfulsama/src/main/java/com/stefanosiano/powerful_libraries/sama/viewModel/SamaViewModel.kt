package com.stefanosiano.powerful_libraries.sama.viewModel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.stefanosiano.powerful_libraries.sama.extensions.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.extensions.logError
import com.stefanosiano.powerful_libraries.sama.extensions.logVerbose
import com.stefanosiano.powerful_libraries.sama.extensions.tryOrPrint
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserver
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserverImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base class for ViewModels.
 * It will contain the fields used by the databinding and all the logic of the data contained into the layouts.
 * */
@Suppress("TooManyFunctions")
open class SamaViewModel<A: VmAction>
/** Initializes the LiveData of the response. */
protected constructor() : ViewModel(), CoroutineScope, SamaObserver by SamaObserverImpl() {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** Flag to check if actions were stopped and should not be sent to the activity. */
    private var actionsStopped = false

    /** Last action sent to the activity. Used to avoid sending multiple actions together (e.g. press on 2 buttons). */
    @Deprecated("blocking actions should be managed differently ([startVmActions] and [stopVmActions])")
    private var lastSentAction: A? = null

    /** Last [VmAction.VmActionSticky] sent to the activity. */
    private var lastStickyAction: A? = null

    @Deprecated("Use liveAction")
    /** LiveData of the response the ViewModel sends to the observer (activity/fragment). */
    private var liveResponse: MediatorLiveData<VmResponse<A>> = MediatorLiveData()

    /** LiveData of the response the ViewModel sends to the observer (activity/fragment). */
    private var liveAction: MediatorLiveData<A> = MediatorLiveData()

    /** Whether multiple actions can be pushed at once (e.g. multiple buttons clicked at the same time). */
    @Deprecated("blocking actions should be managed differently ([startVmActions] and [stopVmActions])")
    private var allowConcurrentActions = true

    /** Whether multiple same actions can be sent at once (e.g. same button clicked multiple times at the same time). */
    @Deprecated("blocking actions should be managed differently ([startVmActions] and [stopVmActions])")
    private var allowConcurrentSameActions = true

    /** Whether this is already initialized. Used to check if [onFirstTime] should be called. */
    internal var isInitialized = AtomicBoolean(false)


    init {
        initObserver(this)
    }

    /** Clears the LiveData of the response to avoid the observer receives it over and over on configuration changes. */
    @Deprecated("blocking actions should be managed differently ([startVmActions] and [stopVmActions])")
    fun clearVmResponse() = liveResponse.postValue(null)

    @Deprecated("Use sendAction")
    /** Sends the [actionId] to the active observer with a nullable [data]. */
    protected fun postAction(actionId: A, data: Any? = null) = postAction(VmResponse(actionId, data))

    @Deprecated("Use sendAction")
    /** Sends the action to the active observer. */
    protected fun postAction(vmResponse: VmResponse<A>) = liveResponse.postValue(vmResponse)

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
        liveResponse.postValue(null)
        liveAction.postValue(null)
    }


    /** Clear the liveData observer (if any). */
    internal fun restartObserving() {
        logVerbose("restartObserving")
        startVmActions()
        startObserver()
    }


    @Deprecated("Use onVmAction")
    /** Observes the action of the ViewModel. Call it on Ui thread. */
    fun observeVmResponse(owner: LifecycleOwner, observer: (suspend (vmAction: A, vmData: Any?) -> Boolean)? = null) {
        liveResponse.observe(owner) {
            synchronized(this) {
                when {
                    !isActive || actionsStopped -> {}
                    // If i clear the response, I clear the lastSentAction, too
                    it == null -> lastSentAction = null
                    else -> {
                        // If the lastSentAction is different from the action of this response,
                        // it means the user pressed on 2 buttons together, so I block it
                        val isLastActionAllowed = lastSentAction != it.action && !allowConcurrentActions
                        val isLastSameActionAllowed = lastSentAction == it.action && !allowConcurrentSameActions

                        if (lastSentAction != null && (isLastActionAllowed || isLastSameActionAllowed)) {
                            logError(
                                "VmResponse blocked! Should clear previous response: " +
                                        "${lastSentAction.toString()} \nbefore sending: $it"
                            )
                        } else {
                            lastSentAction = it.action
                            logVerbose("Sending to activity: $it")

                            launch {
                                val invoked = tryOrPrint(true) {
                                    observer?.invoke(it.action, it.data) != false
                                }
                                if (invoked) {
                                    liveResponse.postValue(null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /** Observes the action of the ViewModel. Call it on Ui thread. */
    fun onVmAction(lifecycleOwner: LifecycleOwner, observer: (vmAction: A) -> Unit) {
        liveAction.observe(lifecycleOwner) {
            when {
                !isActive || actionsStopped -> {}
                it == null -> lastStickyAction = null
                else -> {
                    logVerbose("Sending to activity: $it")
                    tryOrPrint { observer(it) }
                    if (it is VmAction.VmActionSticky) {
                        lastStickyAction = it
                    } else {
                        liveAction.postValue(null)
                    }
                    lastStickyAction?.let { observer(it) }
                }
            }
        }
    }

    /**
     * Allow sending multiple same actions at once (e.g. same button clicked multiple times at the same time).
     * Defaults to true.
     */
    @Deprecated("blocking actions should be managed differently ([startVmActions] and [stopVmActions])")
    fun allowConcurrentSameActions(allow: Boolean) { allowConcurrentSameActions = allow }

    /** Allow pushing multiple actions at once (e.g. multiple buttons clicked at the same time). Defaults to true. */
    @Deprecated("blocking actions should be managed differently ([startVmActions] and [stopVmActions])")
    fun allowConcurrentActions(allow: Boolean) { allowConcurrentActions = allow }

    /** Stop actions from being sent to the observing activity. To resume them call [startVmActions]. */
    fun stopVmActions() { actionsStopped = true }

    /** Start sending actions to the observing activity. To stop them call [stopVmActions]. */
    fun startVmActions() { actionsStopped = false }

    /** Start sending actions to the observing activity. To stop them call [stopVmActions]. */
    fun clearStickyAction() { lastStickyAction = null }

    /** Executes [f] only once. If this is called multiple times, it will have no effect. */
    @Synchronized fun onFirstTime(f: SamaViewModel<A>.() -> Unit) {
        if(!isInitialized.getAndSet(true)) this.f()
    }
}

/** Class containing action and data sent from the ViewModel to its observers. */
open class VmResponse<A: VmAction> (
    /** Specifies what the response is about. */
    val action: A,
    /** Optional data provided by the action. */
    val data: Any?) {

    override fun toString() = "VmResponse{ action= $action, data=$data }"

}

/** Interface that indicates the action sent by the ViewModel. */
interface VmAction {
    /**
     * Indicates that this [VmAction] should be retained when activity is recreated.
     * Any new [VmActionSticky] overrides previous one, so that only 1 action will be retained.
     * If an action is sent and the device is rotated, when the activity restarts it needs to get that action again.
     */
    interface VmActionSticky
}
