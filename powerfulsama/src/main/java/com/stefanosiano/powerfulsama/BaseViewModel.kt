package com.stefanosiano.powerfulsama

import android.util.Log
import androidx.databinding.*
import androidx.lifecycle.*
import com.stefanosiano.powerfulsharedpreferences.PowerfulPreference
import com.stefanosiano.powerfulsharedpreferences_livedata.asLiveData

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/**
 * Base class for ViewModels.
 * It will contain the fields used by the databinding and all the logic of the data contained into the layouts.
 *
 * @param <A> Enum extending [VmResponse.VmAction]. It indicates the action of the response the activity/fragment should handle.
 * @param <E> Enum extending [VmResponse.VmError]. It indicates the error of the response the activity/fragment should handle.
</E></A>
 */
open class BaseViewModel<A, E>
/** Initializes the LiveData of the response */
protected constructor() : ViewModel(), CoroutineScope where A : VmResponse.VmAction, E : VmResponse.VmError {
    private val loggingExceptionHandler = CoroutineExceptionHandler { _, t -> t.printStackTrace() }
    override val coroutineContext = Job() + loggingExceptionHandler


    /** Last action sent to the activity. Used to avoid sending multiple actions together (like pressing on 2 buttons) */
    private var lastSentAction: A? = null

    /** LiveData of the response the ViewModel sends to the observer (activity/fragment) */
    private var liveResponse: MediatorLiveData<VmResponse<A, E, Any>> = MediatorLiveData()

    /** List of liveData that will be observed until the viewModel is destroyed */
    private val observedLiveData = ArrayList<LiveData<out Any?>>()

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

    /** Observes a liveData until the ViewModel is destroyed. Useful when liveData is not used in a lifecycleOwner */
    protected fun <T> observeLd(liveData: LiveData<T>): LiveData<T> {
        observedLiveData.add(liveData)
        liveData.observeForever(persistentObserver)
        return liveData
    }

    @Suppress("unchecked_cast")
    /** Observes a liveData until the ViewModel is destroyed, using a custom observer */
    protected fun <T> observeLd(liveData: LiveData<T>, observerFunction: (data: T) -> Unit): LiveData<T> {

        val observer: Observer<Any?> = Observer { observerFunction.invoke(it as T) }
        customObservedLiveData.add(Pair(liveData as LiveData<Any?>, observer))
        liveData.observeForever(observer)
        return liveData
    }

    /** Observes an observableField until the ViewModel is destroyed, using a custom observer. It also calls [obFun]. Does nothing if the value of the observable is null */
    protected fun observeOf(observable: ObservableInt, obFun: (data: Int) -> Unit) = observable.addOnChangedAndNow { obFun.invoke(it) }

    /** Observes an observableField until the ViewModel is destroyed, using a custom observer. It also calls [obFun]. Does nothing if the value of the observable is null */
    protected fun observeOf(observable: ObservableShort, obFun: (data: Short) -> Unit) = observable.addOnChangedAndNow { obFun.invoke(it) }

    /** Observes an observableField until the ViewModel is destroyed, using a custom observer. It also calls [obFun]. Does nothing if the value of the observable is null */
    protected fun observeOf(observable: ObservableLong, obFun: (data: Long) -> Unit) = observable.addOnChangedAndNow { obFun.invoke(it) }

    /** Observes an observableField until the ViewModel is destroyed, using a custom observer. It also calls [obFun]. Does nothing if the value of the observable is null */
    protected fun observeOf(observable: ObservableFloat, obFun: (data: Float) -> Unit) = observable.addOnChangedAndNow { obFun.invoke(it) }

    /** Observes an observableField until the ViewModel is destroyed, using a custom observer. It also calls [obFun]. Does nothing if the value of the observable is null */
    protected fun observeOf(observable: ObservableDouble, obFun: (data: Double) -> Unit) = observable.addOnChangedAndNow { obFun.invoke(it) }

    /** Observes an observableField until the ViewModel is destroyed, using a custom observer. It also calls [obFun]. Does nothing if the value of the observable is null */
    protected fun observeOf(observable: ObservableBoolean, obFun: (data: Boolean) -> Unit) = observable.addOnChangedAndNow { obFun.invoke(it) }

    /** Observes an observableField until the ViewModel is destroyed, using a custom observer. It also calls [obFun]. Does nothing if the value of the observable is null */
    protected fun <T> observeOf(observable: ObservableField<T>, obFun: (data: T) -> Unit) = observable.addOnChangedAndNow { obFun.invoke(it ?: return@addOnChangedAndNow) }

    /** Observes a sharedPreference until the ViewModel is destroyed, using a custom live data. It also calls [obFun]. Does nothing if the value of the preference is null */
    protected fun <T> observeSp(preference: PowerfulPreference<T>, obFun: (data: T) -> Unit) { observeLd(preference.asLiveData()) { obFun.invoke(it ?: return@observeLd) }; obFun.invoke(preference.get() ?: return) }

    /** Observes a sharedPreference until the ViewModel is destroyed, using a custom live data, and transforms it into an observable field. Does not update the observable if the value of the preference is null */
    protected fun <T> observeSpAsOf(preference: PowerfulPreference<T>): ObservableField<T> {
        val observable = ObservableField<T>()
        observeLd(preference.asLiveData()) { observable.set(it ?: return@observeLd) }
        observable.set(preference.get() ?: return observable)
        return observable
    }

    /** Observes a liveData until the ViewModel is destroyed and transforms it into an observable field. Does not update the observable if the value of the liveData is null */
    protected fun <T> observeLdAsOf(liveData: LiveData<T>): ObservableField<T> {
        val observable = ObservableField<T>()
        observeLd(liveData) { observable.set(it ?: return@observeLd) }
        observable.set(liveData.value ?: return observable)
        return observable
    }


    /** Clear the liveData observer (if any) */
    override fun onCleared() {
        super.onCleared()
        observedLiveData.forEach { it.removeObserver(persistentObserver) }
        customObservedLiveData.forEach { it.first.removeObserver(it.second) }
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
     *      If false, the response should be cleared using [clearVmResponse][BaseViewModel.clearVmResponse] method.
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
     *      If false, the response should be cleared using [clearVmResponse][BaseViewModel.clearVmResponse] method.
     */
    fun observeVmResponse(lifecycleOwner: LifecycleOwner, observer: (vmAction: A, vmData: Any?) -> Boolean, errorObserver: (vmResponse: VmResponse<A, E, Any>) -> Boolean) {
        liveResponse.observe(lifecycleOwner) {

            //If i clear the response, I clear the lastSentAction, too
            if (it == null) {
                lastSentAction = null
                return@observe
            }

            //If the lastSentAction is different from the action of this response, it means the user pressed on 2 buttons together, so I block it
            if (lastSentAction != null && lastSentAction != it.action) {
                Log.e(javaClass.simpleName, "VmResponse blocked! Should clear previous response: ${lastSentAction.toString()} \nbefore sending: ${it.toString()}")
                return@observe
            }
            lastSentAction = it.action

            Log.v(javaClass.simpleName, "Sending to activity: ${it.toString()}")

            if (it.isSuccessful) {
                if (observer.invoke(it.action, it.data))
                    liveResponse.postValue(null)
            } else {
                //error is annotated as NonNull, but it can be null here. So i'm making this check, just to be sure it will never be null outside this class
                it.error ?: throw IllegalArgumentException("Vm response error cannot be null, if isSuccessful is false!")

                if (errorObserver.invoke(it))
                    liveResponse.postValue(null)
            }

        }
    }

}

