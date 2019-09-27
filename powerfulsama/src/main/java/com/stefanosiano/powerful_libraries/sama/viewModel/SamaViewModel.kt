package com.stefanosiano.powerful_libraries.sama.viewModel

import android.util.Log
import androidx.databinding.*
import androidx.lifecycle.*
import com.stefanosiano.powerful_libraries.sama.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


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

    /** Last action sent to the activity. Used to avoid sending multiple actions together (like pressing on 2 buttons) */
    private var lastSentAction: A? = null

    /** LiveData of the response the ViewModel sends to the observer (activity/fragment) */
    private var liveResponse: MediatorLiveData<VmResponse<A>> = MediatorLiveData()

    /** List of liveData that will be observed until the viewModel is destroyed */
    private val observedLiveData = ArrayList<LiveData<out Any?>>()

    /** List of observable callbacks that will be observed until the viewModel is destroyed */
    private val observables = ArrayList<Pair<Observable, Observable.OnPropertyChangedCallback>>()

    /** List of liveData that will be observed until the viewModel is destroyed */
    private val customObservedLiveData = ArrayList<Pair<LiveData<Any?>, Observer<Any?>>>()

    /** Empty Observer that will receive all liveData updates */
    private val persistentObserver = Observer<Any?>{}

    private val observablesMap = ConcurrentHashMap<Long, Int>()
    private val observablesId = AtomicLong(0)

    /** Flag to understand whether multiple actions can be pushed at once (e.g. multiple buttons clicked at the same time) */
    private var allowConcurrentActions = false

    /** Flag to understand whether multiple same actions can be pushed at once (e.g. same button clicked multiple times at the same time) */
    private var allowConcurrentSameActions = false

    /** Clears the LiveData of the response to avoid the observer receives it over and over on configuration changes */
    fun clearVmResponse() = liveResponse.postValue(null)


    /**
     * Sends the response to the active observer
     * @param actionId Id of the action to send
     * @param error Id of the error to send (default null). If null, it means no error is sent to the observer
     * @param data Data to send (default null). Can be null
     */
    protected fun postVmResponse(actionId: A, data: Any? = null) = postVmResponse(VmResponse(actionId, data))

    /** Sends the response to the active observer */
    protected fun postVmResponse(vmResponse: VmResponse<A>) = liveResponse.postValue(vmResponse)

    /**
     * Observes a liveData until the ViewModel is destroyed, using a custom observer
     * Useful when liveData is not used in a lifecycleOwner
     */
    protected fun <T> observe(liveData: LiveData<T>): LiveData<T> {
        observedLiveData.add(liveData)
        runOnUi { liveData.observeForever(persistentObserver) }
        return liveData
    }

    /** Observes a liveData until the ViewModel is destroyed, using a custom observer */
    @Suppress("unchecked_cast")
    protected fun <T> observe(liveData: LiveData<T>, observerFunction: suspend (data: T) -> Unit): LiveData<T> {
        val observer: Observer<Any?> = Observer { launchOrNow(this) { observerFunction(it as? T ?: return@launchOrNow) } }
        customObservedLiveData.add(Pair(liveData as LiveData<Any?>, observer))
        runOnUi { liveData.observeForever(observer) }
        return liveData
    }

    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableByte, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Byte) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableInt, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Int) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableShort, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Short) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableLong, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Long) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableFloat, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Float) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableDouble, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Double) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)


    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableBoolean, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Boolean) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)


    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun <T> observe(o: ObservableField<T>, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: T) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes a liveData until the ViewModel is destroyed and transforms it into an observable field.
     * Does not update the observable if the value of the liveData is null */
    protected fun <T> observeAsOf(liveData: LiveData<T>, defaultValue: T? = null): ObservableField<T> {
        val observable = ObservableField<T>()
        observe(liveData) { observable.set(it ?: return@observe) }
        observable.set(liveData.value ?: defaultValue ?: return observable)
        return observable
    }


    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing if the value of [o] is null or already changed */
    private fun <T> observePrivate(o: Observable, obValue: () -> T?, obFun: suspend (data: T) -> Unit, skipFirst: Boolean, vararg obs: Observable) {
        val obsId = observablesId.incrementAndGet()

        obs.forEach { ob ->
            observablesMap[obsId] = 0
            observables.add(Pair(ob, ob.onChange(this) {
                //increment value of observablesMap[obsId] -> only first call can run this function
                observablesMap[obsId] = observablesMap[obsId]?.plus(1) ?: 1
                if(observablesMap[obsId] != 1) return@onChange
                obValue()?.let { logVerbose(it.toString()); obFun(it) }
                //clear value of observablesMap[obsId] -> everyone can run this function
                observablesMap[obsId] = 0
            }))
        }
        //sets the function to call when using an observable: it sets the observablesMap[obsId] to 2 (it won't be called by obs), run obFun and finally set observablesMap[obsId] to 0 (callable by everyone)
        when(o) {
            is ObservableInt -> observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) { observablesMap[obsId] = 2; obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }; observablesMap[obsId] = 0 }))
            is ObservableShort -> observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) { observablesMap[obsId] = 2; obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }; observablesMap[obsId] = 0 }))
            is ObservableLong -> observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) { observablesMap[obsId] = 2; obValue()?.let { data -> if (data == it)  { logVerbose(data.toString()); obFun(data) } }; observablesMap[obsId] = 0 }))
            is ObservableFloat -> observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) { observablesMap[obsId] = 2; obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }; observablesMap[obsId] = 0 }))
            is ObservableDouble -> observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) { observablesMap[obsId] = 2; obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }; observablesMap[obsId] = 0 }))
            is ObservableBoolean -> observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) { observablesMap[obsId] = 2; obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }; observablesMap[obsId] = 0 }))
            is ObservableByte -> observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) { observablesMap[obsId] = 2; obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }; observablesMap[obsId] = 0 }))
            is ObservableField<*> -> observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) { observablesMap[obsId] = 2; obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }; observablesMap[obsId] = 0 }))
        }
    }


/*
    /** Observes a sharedPreference until the ViewModel is destroyed, using a custom live data. It also calls [obFun]. Does nothing if the value of the preference is null */
    protected fun <T> observe(preference: PowerfulPreference<T>, obFun: (data: T) -> Unit) { observe(preference.asLiveData()) { obFun(it ?: return@observe) }; obFun(preference.get() ?: return) }

    /** Observes a sharedPreference until the ViewModel is destroyed, using a custom live data, and transforms it into an observable field. Does not update the observable if the value of the preference is null */
    protected fun <T> observeAsOf(preference: PowerfulPreference<T>): ObservableField<T> {
        val observable = ObservableField<T>()
        observe(preference.asLiveData()) { observable.set(it ?: return@observe) }
        observable.set(preference.get() ?: return observable)
        return observable
    }*/


    /** Clear the liveData observer (if any) */
    override fun onCleared() {
        super.onCleared()
        logVerbose("onCleared")
        observables.forEach { it.first.removeOnPropertyChangedCallback(it.second) }
        observables.clear()
        runOnUi { observedLiveData.forEach { it.removeObserver(persistentObserver) } }
        observedLiveData.clear()
        runOnUi { customObservedLiveData.forEach { it.first.removeObserver(it.second) } }
        customObservedLiveData.clear()
        coroutineContext.cancel()
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
    fun observeVmResponse(lifecycleOwner: LifecycleOwner, observer: ((vmAction: A, vmData: Any?) -> Boolean)? = null) {
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
                    if (observer?.invoke(it.action, it.data) != false)
                        liveResponse.postValue(null)
                }

            }
        }
    }

    /** Set whether multiple same actions at once are allowed (e.g. same button clicked multiple times at the same time). Defaults to [false] */
    fun allowConcurrentSameActions(allow: Boolean) { allowConcurrentSameActions = allow }

    /** Set whether multiple actions at once are allowed (e.g. multiple buttons clicked at the same time). Defaults to [false] */
    fun allowConcurrentActions(allow: Boolean) { allowConcurrentActions = allow }
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

