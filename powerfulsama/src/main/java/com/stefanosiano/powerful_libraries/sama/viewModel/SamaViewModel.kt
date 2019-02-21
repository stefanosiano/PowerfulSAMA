package com.stefanosiano.powerful_libraries.sama.viewModel

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.databinding.*
import androidx.lifecycle.*
import com.stefanosiano.powerful_libraries.sama.addOnChangedAndNow
import com.stefanosiano.powerful_libraries.sama.launchIfActiveOrNull
import com.stefanosiano.powerful_libraries.sama.mainThreadHandler
import com.stefanosiano.powerful_libraries.sama.observeLd
import kotlinx.coroutines.*


/**
 * Base class for ViewModels.
 * It will contain the fields used by the databinding and all the logic of the data contained into the layouts.
 *
 * @param <A> Enum extending [VmResponse.VmAction]. It indicates the action of the response the activity/fragment should handle.
 * @param <E> Enum extending [VmResponse.VmError]. It indicates the error of the response the activity/fragment should handle.
</E></A>
 */
open class SamaViewModel<A, E>
/** Initializes the LiveData of the response */
protected constructor() : ViewModel(), CoroutineScope where A : VmResponse.VmAction, E : VmResponse.VmError {
    private val loggingExceptionHandler = CoroutineExceptionHandler { _, t -> t.printStackTrace() }
    override val coroutineContext = SupervisorJob() + loggingExceptionHandler

    /** Last action sent to the activity. Used to avoid sending multiple actions together (like pressing on 2 buttons) */
    private var lastSentAction: A? = null

    /** LiveData of the response the ViewModel sends to the observer (activity/fragment) */
    private var liveResponse: MediatorLiveData<VmResponse<A, E, Any>> = MediatorLiveData()

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
    protected fun postVmResponse(actionId: A, error: E? = null, data: Any? = null) = liveResponse.postValue(VmResponse(actionId, error, error == null, data))

    /** Sends the response to the active observer */
    protected fun postVmResponse(vmResponse: VmResponse<A, E, Any>) = liveResponse.postValue(vmResponse)

    /**
     * Observes a liveData until the ViewModel is destroyed, using a custom observer
     * Useful when liveData is not used in a lifecycleOwner
     */
    protected fun <T> observeLd(liveData: LiveData<T>): LiveData<T> {
        observedLiveData.add(liveData)
        mainThreadHandler.post{ liveData.observeForever(persistentObserver) }
        return liveData
    }

    /**
     * Observes a liveData until the ViewModel is destroyed, using a custom observer
     * If [forceOnCurrentThread] is not set, [observerFunction] will run in a background coroutine
     */
    @Suppress("unchecked_cast")
    protected fun <T> observeLd(liveData: LiveData<T>, forceOnCurrentThread: Boolean = false, observerFunction: suspend (data: T) -> Unit): LiveData<T> {
        val observer: Observer<Any?> = Observer { launchIfActiveOrNull(if(forceOnCurrentThread) null else this) { observerFunction.invoke(it as? T ?: return@launchIfActiveOrNull) } }
        customObservedLiveData.add(Pair(liveData as LiveData<Any?>, observer))
        mainThreadHandler.post{ liveData.observeForever(observer) }
        return liveData
    }


    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or already changed
     */
    protected fun observeOf(obs: ObservableInt, forceOnCurrentThread: Boolean = false, obFun: suspend (data: Int) -> Unit) =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(obs.get() == it) obFun.invoke(it) }) )


    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or already changed
     */
    protected fun observeOf(obs: ObservableShort, forceOnCurrentThread: Boolean = false, obFun: suspend (data: Short) -> Unit) =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(obs.get() == it) obFun.invoke(it) }) )


    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or already changed
     */
    protected fun observeOf(obs: ObservableLong, forceOnCurrentThread: Boolean = false, obFun: suspend (data: Long) -> Unit) =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(obs.get() == it) obFun.invoke(it) }) )


    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or already changed
     */
    protected fun observeOf(obs: ObservableFloat, forceOnCurrentThread: Boolean = false, obFun: suspend (data: Float) -> Unit) =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(obs.get() == it) obFun.invoke(it) }) )


    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or already changed
     */
    protected fun observeOf(obs: ObservableDouble, forceOnCurrentThread: Boolean = false, obFun: suspend (data: Double) -> Unit) =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(obs.get() == it) obFun.invoke(it) }) )


    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or already changed
     */
    protected fun observeOf(obs: ObservableBoolean, forceOnCurrentThread: Boolean = false, obFun: suspend (data: Boolean) -> Unit)  =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(obs.get() == it) obFun.invoke(it) }) )

    /**
     * Observes an observableField until the ViewModel is destroyed, using a custom observer.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine.
     * It also calls [obFun] (in the background). Does nothing if the value of the observable is null or, if [forceReload] is set, already changed
     */
    protected fun <T> observeOf(obs: ObservableField<T>, forceOnCurrentThread: Boolean = false, forceReload: Boolean = false, obFun: suspend (data: T) -> Unit) =
        observables.add( Pair(obs, obs.addOnChangedAndNow(if(forceOnCurrentThread) null else this) { if(!forceReload && obs.get() == it) obFun.invoke(it ?: return@addOnChangedAndNow) }) )

/*
    /** Observes a sharedPreference until the ViewModel is destroyed, using a custom live data. It also calls [obFun]. Does nothing if the value of the preference is null */
    protected fun <T> observeSp(preference: PowerfulPreference<T>, obFun: (data: T) -> Unit) { observeLd(preference.asLiveData()) { obFun.invoke(it ?: return@observeLd) }; obFun.invoke(preference.get() ?: return) }

    /** Observes a sharedPreference until the ViewModel is destroyed, using a custom live data, and transforms it into an observable field. Does not update the observable if the value of the preference is null */
    protected fun <T> observeSpAsOf(preference: PowerfulPreference<T>): ObservableField<T> {
        val observable = ObservableField<T>()
        observeLd(preference.asLiveData()) { observable.set(it ?: return@observeLd) }
        observable.set(preference.get() ?: return observable)
        return observable
    }*/

    /**
     * Observes a liveData until the ViewModel is destroyed and transforms it into an observable field.
     * If [forceOnCurrentThread] is not set, it will run in a background coroutine
     * Does not update the observable if the value of the liveData is null
     */
    protected fun <T> observeLdAsOf(liveData: LiveData<T>, defaultValue: T? = null, forceOnCurrentThread: Boolean = false): ObservableField<T> {
        val observable = ObservableField<T>()
        observeLd(liveData, forceOnCurrentThread) { observable.set(it ?: return@observeLd) }
        observable.set(liveData.value ?: defaultValue ?: return observable)
        return observable
    }


    /** Clear the liveData observer (if any) */
    override fun onCleared() {
        super.onCleared()
        observables.forEach { it.first.removeOnPropertyChangedCallback(it.second) }
        observables.clear()
        observedLiveData.forEach { mainThreadHandler.post{ it.removeObserver(persistentObserver) } }
        observedLiveData.clear()
        customObservedLiveData.forEach { mainThreadHandler.post{ it.first.removeObserver(it.second) } }
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
     *
     *
     * @param errorObserver
     *      Observes changes of the VmResponse, in case something went wrong.
     *
     *      NOTE: Here you should check the error of the response, because there was an error.
     *      It's perfectly safe to check only the error and not the action: the whole response is returned
     *      just to access more info, or to use action to group different errors together.
     *      The error and the action of the response will never be null!
     *      The data of the response can be null!
     *
     *      @param vmResponse Response sent from the ViewModel. It will never be null.
     *      @return True to clear the response after being sent to the observer. False to retain it.
     *      If false, the response should be cleared using [clearVmResponse][SamaViewModel.clearVmResponse] method.
     */
    fun observeVmResponse(lifecycleOwner: LifecycleOwner, observer: ((vmAction: A, vmData: Any?) -> Boolean)? = null, errorObserver: ((vmResponse: VmResponse<A, E, Any>) -> Boolean)? = null) {
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

            if (it.isSuccessful) {
                if (observer?.invoke(it.action, it.data) != false)
                    liveResponse.postValue(null)
            } else {
                //error is annotated as NonNull, but it can be null here. So i'm making this check, just to be sure it will never be null outside this class
                it.error ?: throw IllegalArgumentException("Vm response error cannot be null, if isSuccessful is false!")

                if (errorObserver?.invoke(it) != false)
                    liveResponse.postValue(null)
            }

        }
    }
}

