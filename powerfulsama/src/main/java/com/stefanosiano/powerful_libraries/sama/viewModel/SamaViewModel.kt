package com.stefanosiano.powerful_libraries.sama.viewModel

import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableByte
import androidx.databinding.ObservableChar
import androidx.databinding.ObservableDouble
import androidx.databinding.ObservableField
import androidx.databinding.ObservableFloat
import androidx.databinding.ObservableInt
import androidx.databinding.ObservableList
import androidx.databinding.ObservableLong
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.logError
import com.stefanosiano.powerful_libraries.sama.logVerbose
import com.stefanosiano.powerful_libraries.sama.tryOrPrint
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserver
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserverImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base class for ViewModels.
 * It will contain the fields used by the databinding and all the logic of the data contained into the layouts.
 * */
open class SamaViewModel<A: VmAction>
/** Initializes the LiveData of the response. */
protected constructor() : ViewModel(), CoroutineScope {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** Object that takes care of observing liveData and observableFields. */
    private val samaObserver: SamaObserver = SamaObserverImpl()

    /** Flag to check if actions were stopped and should not be sent to the activity. */
    private var actionsStopped = false

    /** Last action sent to the activity. Used to avoid sending multiple actions together (e.g. press on 2 buttons). */
    @Deprecated("blocking actions should me managed differently ([startVmActions] and [stopVmActions])")
    private var lastSentAction: A? = null

    /** Last [VmAction.VmActionSticky] sent to the activity. */
    private var lastStickyAction: A? = null

    @Deprecated("Use liveAction")
    /** LiveData of the response the ViewModel sends to the observer (activity/fragment). */
    private var liveResponse: MediatorLiveData<VmResponse<A>> = MediatorLiveData()

    /** LiveData of the response the ViewModel sends to the observer (activity/fragment). */
    private var liveAction: MediatorLiveData<A> = MediatorLiveData()

    /** Whether multiple actions can be pushed at once (e.g. multiple buttons clicked at the same time). */
    @Deprecated("blocking actions should me managed differently ([startVmActions] and [stopVmActions])")
    private var allowConcurrentActions = true

    /** Whether multiple same actions can be sent at once (e.g. same button clicked multiple times at the same time). */
    @Deprecated("blocking actions should me managed differently ([startVmActions] and [stopVmActions])")
    private var allowConcurrentSameActions = true

    /** Whether this is already initialized. Used to check if [onFirstTime] should be called. */
    internal var isInitialized = AtomicBoolean(false)


    /** Clears the LiveData of the response to avoid the observer receives it over and over on configuration changes. */
    @Deprecated("blocking actions should me managed differently ([startVmActions] and [stopVmActions])")
    fun clearVmResponse() = liveResponse.postValue(null)

    @Deprecated("Use sendAction")
    /** Sends the [actionId] to the active observer with a nullable [data]. */
    protected fun postAction(actionId: A, data: Any? = null) = postAction(VmResponse(actionId, data))

    @Deprecated("Use sendAction")
    /** Sends the action to the active observer. */
    protected fun postAction(vmResponse: VmResponse<A>) = liveResponse.postValue(vmResponse)

    /** Sends the action to the active observer. */
    protected fun sendAction(action: A) = liveAction.postValue(action)

    init {
        samaObserver.initObserver(this)
    }

    /** Clear the liveData observer (if any). */
    override fun onCleared() {
        super.onCleared()
        logVerbose("onCleared")
        samaObserver.destroyObserver()
        coroutineContext.cancel()
    }


    /** Clear the liveData observer (if any). */
    internal fun stopObserving() {
        logVerbose("stopObserving")
        stopVmActions()
        samaObserver.stopObserver()
        liveResponse.postValue(null)
        liveAction.postValue(null)
    }


    /** Clear the liveData observer (if any). */
    internal fun restartObserving() {
        logVerbose("restartObserving")
        startVmActions()
        samaObserver.startObserver()
    }


    @Deprecated("Use onVmAction")
    /** Observes the action of the ViewModel. Call it on Ui thread. */
    fun observeVmResponse(owner: LifecycleOwner, observer: (suspend (vmAction: A, vmData: Any?) -> Boolean)? = null) {
        liveResponse.observe(owner) {

            synchronized(this) {
                if (!isActive) return@observe

                if (actionsStopped) return@observe

                // If i clear the response, I clear the lastSentAction, too
                if (it == null) {
                    lastSentAction = null
                    return@observe
                }


                // If the lastSentAction is different from the action of this response,
                // it means the user pressed on 2 buttons together, so I block it
                if (lastSentAction != null &&
                    (
                        (lastSentAction != it.action && !allowConcurrentActions)
                            || (lastSentAction == it.action && !allowConcurrentSameActions)
                    )
                ) {
                    logError("VmResponse blocked! Should clear previous response: " +
                            "${lastSentAction.toString()} \nbefore sending: $it")
                    return@observe
                }

                lastSentAction = it.action
                logVerbose("Sending to activity: $it")

                launch {
                    if (tryOrPrint(true) { observer?.invoke(it.action, it.data) != false })
                        liveResponse.postValue(null)
                }

            }
        }
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
            if (it is VmAction.VmActionSticky)
                lastStickyAction = it
            liveAction.postValue(null)
        }
        lastStickyAction?.let { observer(it) }
    }

    /**
     * Allow sending multiple same actions at once (e.g. same button clicked multiple times at the same time).
     * Defaults to true.
     */
    @Deprecated("blocking actions should me managed differently ([startVmActions] and [stopVmActions])")
    fun allowConcurrentSameActions(allow: Boolean) { allowConcurrentSameActions = allow }

    /** Allow pushing multiple actions at once (e.g. multiple buttons clicked at the same time). Defaults to true. */
    @Deprecated("blocking actions should me managed differently ([startVmActions] and [stopVmActions])")
    fun allowConcurrentActions(allow: Boolean) { allowConcurrentActions = allow }

    /** Stop actions from being sent to the observing activity. To resume them call [startVmActions]. */
    fun stopVmActions() { actionsStopped = true }

    /** Start sending actions to the observing activity. To stop them call [stopVmActions]. */
    fun startVmActions() { actionsStopped = false }

    /** Start sending actions to the observing activity. To stop them call [stopVmActions]. */
    fun clearStickyAction() { lastStickyAction = null }



















    /** Observes a liveData until this object is destroyed into an observable field. Does not update the observable if the value of the liveData is null. */
    protected fun <T> observeAsOf(liveData: LiveData<T>): ObservableField<T> = samaObserver.observeAsOf(liveData)

    /** Observes a liveData until this object is destroyed, using a custom observer. */
    protected fun <T> observe(liveData: LiveData<T>, vararg obs: Observable, observerFunction: (data: T) -> Unit): LiveData<T> = samaObserver.observe(liveData, *obs) { observerFunction(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. */
    protected fun <T> observe(o: ObservableList<T>, vararg obs: Observable, obFun: (data: List<T>) -> Unit): Unit = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null. */
    protected fun <R> observe(o: ObservableInt, vararg obs: Observable, obFun: (data: Int) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null. */
    protected fun <R> observe(o: ObservableLong, vararg obs: Observable, obFun: (data: Long) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null. */
    protected fun <R> observe(o: ObservableByte, vararg obs: Observable, obFun: (data: Byte) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null. */
    protected fun <R> observe(o: ObservableChar, vararg obs: Observable, obFun: (data: Char) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null. */
    protected fun <R> observe(o: ObservableBoolean, vararg obs: Observable, obFun: (data: Boolean) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null. */
    protected fun <R> observe(o: ObservableFloat, vararg obs: Observable, obFun: (data: Float) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null. */
    protected fun <R> observe(o: ObservableDouble, vararg obs: Observable, obFun: (data: Double) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null. */
    protected fun <R, T> observe(o: ObservableField<T>, vararg obs: Observable, obFun: (data: T) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes the flow [f] until this object is destroyed and calls [obFun] in the background, now and whenever [f] or any of [obs] change, with the current value of [f]. Does nothing if [f] is null or already changed. Returns an [ObservableField] with initial value of null. */
    protected fun <R, T> observe(f: Flow<T>, vararg obs: Observable, obFun: (data: T) -> R): ObservableField<R> = samaObserver.observe(f, *obs) { obFun(it) }

    /** Run [f] to get a [LiveData] every time any of [o] or [obs] changes, removing the old one. It return a [LiveData] of the same type as [f]. */
    protected fun <T> observeAndReloadLiveData(o: ObservableField<*>, vararg obs: Observable, f: () -> LiveData<T>?): LiveData<T> = samaObserver.observeAndReloadLiveData(o, *obs) { f() }

}

/** Executes [f] only once. If this is called multiple times, it will have no effect. */
@Synchronized fun <T: SamaViewModel<*>> T.onFirstTime(f: T.() -> Unit) {
    if(!isInitialized.getAndSet(true)) this.f()
}

/** Class containing action and data sent from the ViewModel to its observers. */
open class VmResponse<A: VmAction> (
    /** Specifies what the response is about . */
    val action: A,
    /** Optional data provided by the action . */
    val data: Any?) {

    override fun toString() = "VmResponse{ action= $action, data=$data }"

}

/** Interface that indicates the action sent by the ViewModel. */
interface VmAction {
    /** Indicates that this [VmAction] should be retained when activity is recreated. Any new [VmActionSticky] overrides previous one, so that only 1 action will be retained.
     * If an action is sent and the device is rotated, when the activity restarts it needs to get that action again. */
    interface VmActionSticky
}
