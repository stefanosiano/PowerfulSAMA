package com.stefanosiano.powerful_libraries.sama.viewModel

import android.util.Log
import androidx.databinding.*
import androidx.lifecycle.*
import com.stefanosiano.powerful_libraries.sama.*
import com.stefanosiano.powerful_libraries.sama.observeLd
import kotlinx.coroutines.*


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
    private val loggingExceptionHandler = CoroutineExceptionHandler { _, t -> t.printStackTrace() }
    override val coroutineContext = SupervisorJob() + loggingExceptionHandler

    /** Last action sent to the activity. Used to avoid sending multiple actions together (like pressing on 2 buttons) */
    private var lastSentAction: A? = null

    /** LiveData of the response the ViewModel sends to the observer (activity/fragment) */
    private var liveResponse: MediatorLiveData<VmResponse<A>> = MediatorLiveData()

    /** List of liveData that will be observed until the viewModel is destroyed */
    private val observedLiveData = ArrayList<LiveData<out Any?>>()

    /** List of observable callbacks that will be observed until the viewModel is destroyed */
    private val observables = ArrayList<Pair<BaseObservable, Observable.OnPropertyChangedCallback>>()

    /** List of liveData that will be observed until the viewModel is destroyed */
    private val customObservedLiveData = ArrayList<Pair<LiveData<Any?>, Observer<Any?>>>()

    /** Empty Observer that will receive all liveData updates */
    private val persistentObserver = Observer<Any?>{}

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
        runOnUiAndWait { liveData.observeForever(persistentObserver) }
        return liveData
    }

    /**
     * Observes a liveData until the ViewModel is destroyed, using a custom observer
     * If [forceOnCurrentThread] is not set, [observerFunction] will run in a background coroutine
     */
    @Suppress("unchecked_cast")
    protected fun <T> observe(liveData: LiveData<T>, forceOnCurrentThread: Boolean = false, observerFunction: suspend (data: T) -> Unit): LiveData<T> {
        val observer: Observer<Any?> = Observer { launchIfActiveOrNull(if(forceOnCurrentThread) null else this) { observerFunction.invoke(it as? T ?: return@launchIfActiveOrNull) } }
        customObservedLiveData.add(Pair(liveData as LiveData<Any?>, observer))
        runOnUiAndWait { liveData.observeForever(observer) }
        return liveData
    }


    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or already changed
     */
    protected fun observe(obs: ObservableInt, forceOnCurrentThread: Boolean = false, obFun: suspend (data: Int) -> Unit) =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(obs.get() == it) obFun.invoke(it) }) )


    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or already changed
     */
    protected fun observe(obs: ObservableShort, forceOnCurrentThread: Boolean = false, obFun: suspend (data: Short) -> Unit) =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(obs.get() == it) obFun.invoke(it) }) )


    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or already changed
     */
    protected fun observe(obs: ObservableLong, forceOnCurrentThread: Boolean = false, obFun: suspend (data: Long) -> Unit) =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(obs.get() == it) obFun.invoke(it) }) )


    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or already changed
     */
    protected fun observe(obs: ObservableFloat, forceOnCurrentThread: Boolean = false, obFun: suspend (data: Float) -> Unit) =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(obs.get() == it) obFun.invoke(it) }) )


    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or already changed
     */
    protected fun observe(obs: ObservableDouble, forceOnCurrentThread: Boolean = false, obFun: suspend (data: Double) -> Unit) =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(obs.get() == it) obFun.invoke(it) }) )


    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or already changed
     */
    protected fun observe(obs: ObservableBoolean, forceOnCurrentThread: Boolean = false, obFun: suspend (data: Boolean) -> Unit)  =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(obs.get() == it) obFun.invoke(it) }) )

    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or, if [forceReload] is set, already changed
     */
    protected fun <T> observe(obs: ObservableField<T>, forceOnCurrentThread: Boolean = false, forceReload: Boolean = false, obFun: suspend (data: T) -> Unit) =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(!forceReload && obs.get() == it) obFun.invoke(it ?: return@addOnChangedAndNow) }) )

/*
    /** Observes a sharedPreference until the ViewModel is destroyed, using a custom live data. It also calls [obFun]. Does nothing if the value of the preference is null */
    protected fun <T> observe(preference: PowerfulPreference<T>, obFun: (data: T) -> Unit) { observe(preference.asLiveData()) { obFun.invoke(it ?: return@observe) }; obFun.invoke(preference.get() ?: return) }

    /** Observes a sharedPreference until the ViewModel is destroyed, using a custom live data, and transforms it into an observable field. Does not update the observable if the value of the preference is null */
    protected fun <T> observeAsOf(preference: PowerfulPreference<T>): ObservableField<T> {
        val observable = ObservableField<T>()
        observe(preference.asLiveData()) { observable.set(it ?: return@observe) }
        observable.set(preference.get() ?: return observable)
        return observable
    }*/

    /**
     * Observes a liveData until the ViewModel is destroyed and transforms it into an observable field.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine
     * Does not update the observable if the value of the liveData is null
     */
    protected fun <T> observeAsOf(liveData: LiveData<T>, defaultValue: T? = null, forceOnCurrentThread: Boolean = false): ObservableField<T> {
        val observable = ObservableField<T>()
        observe(liveData, forceOnCurrentThread) { observable.set(it ?: return@observe) }
        observable.set(liveData.value ?: defaultValue ?: return observable)
        return observable
    }


    /** Clear the liveData observer (if any) */
    override fun onCleared() {
        super.onCleared()
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

            //If i clear the response, I clear the lastSentAction, too
            if (it == null) {
                lastSentAction = null
                return@observeLd
            }

            //If the lastSentAction is different from the action of this response, it means the user pressed on 2 buttons together, so I block it
            if (lastSentAction != null && lastSentAction != it.action) {
                Log.e(javaClass.simpleName, "VmResponse blocked! Should clear previous response: ${lastSentAction.toString()} \nbefore sending: $it")
                return@observeLd
            }
            lastSentAction = it.action

            Log.v(javaClass.simpleName, "Sending to activity: $it")

            if (observer?.invoke(it.action, it.data) != false)
                liveResponse.postValue(null)
        }
    }
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

