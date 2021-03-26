package com.stefanosiano.powerful_libraries.sama.viewModel

import androidx.databinding.*
import androidx.lifecycle.*
import com.stefanosiano.powerful_libraries.sama.*
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserver
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserverImpl
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Base class for ViewModels.
 * It will contain the fields used by the databinding and all the logic of the data contained into the layouts.
 *
 * @param <A> Enum extending [VmResponse.VmAction]. It indicates the action of the response the activity/fragment should handle.
</E></A>
 */
open class SamaViewModel<A>
/** Initializes the LiveData of the response */
protected constructor() : ViewModel(), CoroutineScope where A : VmResponse.VmAction {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** Object that takes care of observing liveData and observableFields */
    private val samaObserver: SamaObserver = SamaObserverImpl()

    /** Last action sent to the activity. Used to avoid sending multiple actions together (like pressing on 2 buttons) */
    private var lastSentAction: A? = null

    /** LiveData of the response the ViewModel sends to the observer (activity/fragment) */
    private var liveResponse: MediatorLiveData<VmResponse<A>> = MediatorLiveData()

    /** Flag to understand whether multiple actions can be pushed at once (e.g. multiple buttons clicked at the same time) */
    private var allowConcurrentActions = false

    /** Flag to understand whether multiple same actions can be pushed at once (e.g. same button clicked multiple times at the same time) */
    private var allowConcurrentSameActions = false

    /** Flag to understand whether this [SamaViewModel] is already initialized. Used to check if [onFirtstTime] should be called */
    internal var isInitialized = AtomicBoolean(false)


    /** Clears the LiveData of the response to avoid the observer receives it over and over on configuration changes */
    fun clearVmResponse() = liveResponse.postValue(null)

    /** Sends the [actionId] to the active observer with a nullable [data] */
    protected fun postAction(actionId: A, data: Any? = null) = postAction(VmResponse(actionId, data))

    /** Sends the action to the active observer */
    protected fun postAction(vmResponse: VmResponse<A>) = liveResponse.postValue(vmResponse)

    init {
        samaObserver.initObserver(this)
    }

    /** Clear the liveData observer (if any) */
    override fun onCleared() {
        super.onCleared()
        logVerbose("onCleared")
        samaObserver.destroyObserver()
        coroutineContext.cancel()
    }


    /** Clear the liveData observer (if any) */
    internal fun stopObserving() {
        logVerbose("stopObserving")
        samaObserver.stopObserver()
        liveResponse.postValue(null)
    }


    /** Clear the liveData observer (if any) */
    internal fun restartObserving() {
        logVerbose("restartObserving")
        samaObserver.startObserver()
    }


    /**
     * Observes the VmResponse of the ViewModel.
     * The observer will never receive a null value.
     *
     * @param lifecycleOwner LifecycleOwner of the observer.
     * @param observer
     *      Observes changes of the VmResponse, in case everything went alright.
     *
     *      @param vmAction Action sent from the ViewModel. It will never be null.
     *      @param vmData Data sent from the ViewModel. It can be null.
     *      @return True to clear the response after being sent to the observer. False to retain it.
     *      If false, the response should be cleared using [clearVmResponse][SamaViewModel.clearVmResponse] method.
     */
    fun observeVmResponse(lifecycleOwner: LifecycleOwner, observer: (suspend (vmAction: A, vmData: Any?) -> Boolean)? = null) {
        liveResponse.observeLd(lifecycleOwner) {

            synchronized(this) {
                if(!isActive) return@observeLd

                //If i clear the response, I clear the lastSentAction, too
                if (it == null) {
                    lastSentAction = null
                    return@observeLd
                }


                if(lastSentAction != null) {
                    //If the lastSentAction is different from the action of this response, it means the user pressed on 2 buttons together, so I block it
                    if ( (lastSentAction != it.action && !allowConcurrentActions) || (lastSentAction == it.action && !allowConcurrentSameActions) ) {
                        logError("VmResponse blocked! Should clear previous response: ${lastSentAction.toString()} \nbefore sending: $it")
                        return@observeLd
                    }
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

    /** Set whether multiple same actions at once are allowed (e.g. same button clicked multiple times at the same time). Defaults to [false] */
    fun allowConcurrentSameActions(allow: Boolean) { allowConcurrentSameActions = allow }

    /** Set whether multiple actions at once are allowed (e.g. multiple buttons clicked at the same time). Defaults to [false] */
    fun allowConcurrentActions(allow: Boolean) { allowConcurrentActions = allow }




















    /** Observes a liveData until this object is destroyed into an observable field. Does not update the observable if the value of the liveData is null */
    protected fun <T> observeAsOf(liveData: LiveData<T>): ObservableField<T> = samaObserver.observeAsOf(liveData)

    /** Observes a liveData until this object is destroyed, using a custom observer */
    protected fun <T> observe(liveData: LiveData<T>, observerFunction: (data: T) -> Unit): LiveData<T> = samaObserver.observe(liveData, observerFunction)

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed */
    protected fun <T> observe(o: ObservableList<T>, vararg obs: Observable, obFun: (data: List<T>) -> Unit): Unit where T: Any = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableInt, vararg obs: Observable, obFun: (data: Int) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableLong, vararg obs: Observable, obFun: (data: Long) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableByte, vararg obs: Observable, obFun: (data: Byte) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableChar, vararg obs: Observable, obFun: (data: Char) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableBoolean, vararg obs: Observable, obFun: (data: Boolean) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableFloat, vararg obs: Observable, obFun: (data: Float) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableDouble, vararg obs: Observable, obFun: (data: Double) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R, T> observe(o: ObservableField<T>, vararg obs: Observable, obFun: (data: T) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }
}

/** Executes [f] only once. If this is called multiple times, it will have no effect */
@Synchronized fun <T> T.onFirstTime(f: T.() -> Unit) where T : SamaViewModel<*> {
    if(!isInitialized.getAndSet(true)) this.f()
}

/** Class containing action and data sent from the ViewModel to its observers */
open class VmResponse<A> (
    /** Specifies what the response is about  */
    val action: A,
    /** Optional data provided by the action  */
    val data: Any?) where A : VmResponse.VmAction {

    override fun toString() = "VmResponse{ action= $action, data=$data }"

    /** Interface that indicates the action of the VmResponse sent by the ViewModel */
    interface VmAction
}

