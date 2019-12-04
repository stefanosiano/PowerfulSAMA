package com.stefanosiano.powerful_libraries.sama.viewModel

import android.util.Log
import androidx.databinding.*
import androidx.lifecycle.*
import com.stefanosiano.powerful_libraries.sama.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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
    private val observables = ArrayList<SamaInnerObservable?>()
    /** List of observable lists callbacks that will be observed until the viewModel is destroyed */
    private val listObservables = ArrayList<SamaInnerListObservable?>()

    /** List of liveData that will be observed until the viewModel is destroyed */
    private val customObservedLiveData = ArrayList<Pair<LiveData<Any?>, Observer<Any?>>>()

    /** Empty Observer that will receive all liveData updates */
    private val persistentObserver = Observer<Any?>{}

    private val observablesMap = ConcurrentHashMap<Long, AtomicInteger>()
    private val observablesId = AtomicLong(0)

    /** Flag to understand whether multiple actions can be pushed at once (e.g. multiple buttons clicked at the same time) */
    private var allowConcurrentActions = false

    /** Flag to understand whether multiple same actions can be pushed at once (e.g. same button clicked multiple times at the same time) */
    private var allowConcurrentSameActions = false

    /** Flag to understand whether this [SamaViewModel] is already initialized. Used to check if [onFirtstTime] should be called */
    internal var isInitialized = AtomicBoolean(false)


    /** Clears the LiveData of the response to avoid the observer receives it over and over on configuration changes */
    fun clearVmResponse() = liveResponse.postValue(null)


    /**
     * Sends the action to the active observer
     * @param actionId Id of the action to send
     * @param data Data to send (default null). Can be null
     */
    protected fun postAction(actionId: A, data: Any? = null) = postAction(VmResponse(actionId, data))

    /** Sends the action to the active observer */
    protected fun postAction(vmResponse: VmResponse<A>) = liveResponse.postValue(vmResponse)






    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    @Suppress("UNCHECKED_CAST")
    protected fun <T> observe(o: ObservableList<T>, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: ObservableList<T>) -> Unit): Unit where T: Any {
        val obsId = observablesId.incrementAndGet()

        val f: suspend (Any?) -> Unit = { oldData ->
            val id = observablesMap[obsId]?.incrementAndGet() ?: 1
            if(id == 1 && oldData != o) {
                o.let { logVerbose(it.toString()); obFun(it) }
                observablesMap[obsId]?.set(0)
            }
        }

        obs.forEach { ob ->

            observablesMap[obsId] = AtomicInteger(0)
            observables.add(SamaInnerObservable(ob, f, ob.onChange(this) {
                //increment value of observablesMap[obsId] -> only first call can run this function
                val id = observablesMap[obsId]?.incrementAndGet() ?: 1
                if(id != 1) return@onChange
                o.let { logVerbose(it.toString()); obFun(it) }
                //clear value of observablesMap[obsId] -> everyone can run this function
                observablesMap[obsId]?.set(0)
            }))
        }

        val c = o.onAnyChange {
            launchOrNow(this) {
                observablesMap[obsId]?.set(2)
                logVerbose(o.toString())
                obFun(it)
                observablesMap[obsId]?.set(0)
            }
        }
        listObservables.add(SamaInnerListObservable(o as ObservableList<Any>, f, c as ObservableList.OnListChangedCallback<ObservableList<Any>>))
        if(!skipFirst)
            launchOrNow(this) { obFun(o) }
    }


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


        val f: suspend (Any?) -> Unit = { oldData ->
            val id = observablesMap[obsId]?.incrementAndGet() ?: 1
            val newData = obValue()
            if(id == 1 && oldData != newData) {
                newData?.let { logVerbose(it.toString()); obFun(it) }
                observablesMap[obsId]?.set(0)
            }
        }

        obs.forEach { ob ->
            observablesMap[obsId] = AtomicInteger(0)
            observables.add(SamaInnerObservable(ob, f, ob.onChange(this) {
                //increment value of observablesMap[obsId] -> only first call can run this function
                val id = observablesMap[obsId]?.incrementAndGet() ?: 1
                if(id != 1) return@onChange
                obValue()?.let { logVerbose(it.toString()); obFun(it) }
                //clear value of observablesMap[obsId] -> everyone can run this function
                observablesMap[obsId]?.set(0)
            }))
        }

        //sets the function to call when using an observable: it sets the observablesMap[obsId] to 2 (it won't be called by obs), run obFun and finally set observablesMap[obsId] to 0 (callable by everyone)
        when(o) {
            is ObservableInt -> {
                observables.add(SamaInnerObservable(o, f, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableShort -> {
                observables.add(SamaInnerObservable(o, f, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableLong -> {
                observables.add(SamaInnerObservable(o, f, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableFloat -> {
                observables.add(SamaInnerObservable(o, f, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableDouble -> {
                observables.add(SamaInnerObservable(o, f, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableBoolean -> {
                observables.add(SamaInnerObservable(o, f, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableByte -> {
                observables.add(SamaInnerObservable(o, f, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableField<*> -> {
                observables.add(SamaInnerObservable(o, f, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
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
        synchronized(observables) { observables.filterNotNull().filter { it.registered }.forEach { it.registered = false; it.ob.removeOnPropertyChangedCallback(it.callback) } }
        observables.clear()
        synchronized(listObservables) { listObservables.filterNotNull().filter { it.registered }.forEach { it.registered = false; it.ob.removeOnListChangedCallback(it.callback) } }
        listObservables.clear()
        runOnUi { observedLiveData.forEach { it.removeObserver(persistentObserver) } }
        observedLiveData.clear()
        runOnUi { customObservedLiveData.forEach { it.first.removeObserver(it.second) } }
        customObservedLiveData.clear()
        coroutineContext.cancel()
    }


    /** Clear the liveData observer (if any) */
    internal fun stopObserving() {
        logVerbose("stopObserving")
        synchronized(observables) { observables.filterNotNull().filter { it.registered }.forEach { it.registered = false; it.ob.removeOnPropertyChangedCallback(it.callback) } }
        synchronized(listObservables) { listObservables.filterNotNull().filter { it.registered }.forEach { it.registered = false; it.ob.removeOnListChangedCallback(it.callback) } }
        runOnUi { observedLiveData.forEach { it.removeObserver(persistentObserver) } }
        runOnUi { customObservedLiveData.forEach { it.first.removeObserver(it.second) } }
    }


    /** Clear the liveData observer (if any) */
    internal fun restartObserving() {
        logVerbose("stopObserving")
        synchronized(observables) { observables.filterNotNull().filter { !it.registered }.forEach {
            it.registered = true
            it.ob.addOnPropertyChangedCallback(it.callback)

            //sets the function to call when using an observable: it sets the observablesMap[obsId] to 2 (it won't be called by obs), run obFun and finally set observablesMap[obsId] to 0 (callable by everyone)
            when(it.ob) {
                is ObservableInt -> launch { it.f(it.ob.get()) }
                is ObservableShort -> launch { it.f(it.ob.get()) }
                is ObservableLong -> launch { it.f(it.ob.get()) }
                is ObservableFloat -> launch { it.f(it.ob.get()) }
                is ObservableDouble -> launch { it.f(it.ob.get()) }
                is ObservableBoolean -> launch { it.f(it.ob.get()) }
                is ObservableByte -> launch { it.f(it.ob.get()) }
                is ObservableField<*> -> launch { it.f(it.ob.get()) }
                else -> launch { it.f(null) }
            }
        } }
        synchronized(listObservables) { listObservables.filterNotNull().filter { !it.registered }.forEach { it.registered = true; it.ob.addOnListChangedCallback(it.callback); launch { it.f(it.ob) } } }
        runOnUi { observedLiveData.forEach { it.observeForever(persistentObserver); persistentObserver.onChanged(it.value) } }
        runOnUi { customObservedLiveData.forEach { it.first.observeForever(it.second); it.second.onChanged(it.first.value) } }
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


    private inner class SamaInnerObservable (val ob: Observable, val f: suspend (Any?) -> Unit, val callback: Observable.OnPropertyChangedCallback, var registered: Boolean = true)
    private inner class SamaInnerListObservable (val ob: ObservableList<Any>, val f: suspend (ObservableList<Any>) -> Unit, val callback: ObservableList.OnListChangedCallback<ObservableList<Any>>, var registered: Boolean = true)

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

