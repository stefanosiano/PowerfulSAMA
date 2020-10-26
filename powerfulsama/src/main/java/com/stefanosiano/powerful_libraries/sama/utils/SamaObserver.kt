package com.stefanosiano.powerful_libraries.sama.utils

import androidx.databinding.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.stefanosiano.powerful_libraries.sama.*
import com.stefanosiano.powerful_libraries.sama.logVerbose
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

/** Class that allows a component to observe variables and call methods when they change.
 * The mean methods to call are [destroyObserver], [stopObserver] and [restartObserver].
 * You can change the default [observerDelay] of 50 milliseconds to enable longer/shorter delays when variables change (useful when observing multiple variables at the same time) */
class SamaObserver(val scope: CoroutineScope) {

    private val observableMap: HashMap<Int, SamaObservableHelper> = HashMap()
    private val observablesId: AtomicInteger = AtomicInteger()

    var observerDelay: Long = 50L
    private var isPaused: Boolean = true

    /** List of observable callbacks that will be observed until the viewModel is destroyed */
    private val observables: ArrayList<SamaInnerObservable> = ArrayList()

    /** List of observable lists callbacks that will be observed until the viewModel is destroyed */
    private val listObservables: ArrayList<SamaInnerListObservable> = ArrayList()

    /** List of liveData that will be observed until the viewModel is destroyed */
    private val observedLiveData = ArrayList<LiveData<out Any?>>()

    /** List of liveData that will be observed until the viewModel is destroyed */
    private val customObservedLiveData = ArrayList<Pair<LiveData<Any?>, Observer<Any?>>>()

    /** Empty Observer that will receive all liveData updates */
    private val persistentObserver = Observer<Any?>{}


    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    fun <R> observe(o: ObservableInt, defValue: R, vararg obs: Observable, obFun: suspend (data: Int) -> R): ObservableF<R> = ObservableF<R>(defValue).also { toRet -> observePrivate<Int>(o, { toRet.set(obFun(it)) }, *obs) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    fun <R> observe(o: ObservableInt, vararg obs: Observable, obFun: suspend (data: Int) -> R): ObservableField<R> = ObservableField<R>().also { toRet -> observePrivate<Int>(o, { toRet.set(obFun(it)) }, *obs) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    fun <R> observe(o: ObservableLong, defValue: R, vararg obs: Observable, obFun: suspend (data: Long) -> R): ObservableF<R> = ObservableF<R>(defValue).also { toRet -> observePrivate<Long>(o, { toRet.set(obFun(it)) }, *obs) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    fun <R> observe(o: ObservableLong, vararg obs: Observable, obFun: suspend (data: Long) -> R): ObservableField<R> = ObservableField<R>().also { toRet -> observePrivate<Long>(o, { toRet.set(obFun(it)) }, *obs) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    fun <R> observe(o: ObservableByte, defValue: R, vararg obs: Observable, obFun: suspend (data: Byte) -> R): ObservableF<R> = ObservableF<R>(defValue).also { toRet -> observePrivate<Byte>(o, { toRet.set(obFun(it)) }, *obs) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    fun <R> observe(o: ObservableByte, vararg obs: Observable, obFun: suspend (data: Byte) -> R): ObservableField<R> = ObservableField<R>().also { toRet -> observePrivate<Byte>(o, { toRet.set(obFun(it)) }, *obs) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    fun <R> observe(o: ObservableChar, defValue: R, vararg obs: Observable, obFun: suspend (data: Char) -> R): ObservableF<R> = ObservableF<R>(defValue).also { toRet -> observePrivate<Char>(o, { toRet.set(obFun(it)) }, *obs) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    fun <R> observe(o: ObservableChar, vararg obs: Observable, obFun: suspend (data: Char) -> R): ObservableField<R> = ObservableField<R>().also { toRet -> observePrivate<Char>(o, { toRet.set(obFun(it)) }, *obs) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    fun <R> observe(o: ObservableBoolean, defValue: R, vararg obs: Observable, obFun: suspend (data: Boolean) -> R): ObservableF<R> = ObservableF<R>(defValue).also { toRet -> observePrivate<Boolean>(o, { toRet.set(obFun(it)) }, *obs) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    fun <R> observe(o: ObservableBoolean, vararg obs: Observable, obFun: suspend (data: Boolean) -> R): ObservableField<R> = ObservableField<R>().also { toRet -> observePrivate<Boolean>(o, { toRet.set(obFun(it)) }, *obs) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    fun <R> observe(o: ObservableFloat, defValue: R, vararg obs: Observable, obFun: suspend (data: Float) -> R): ObservableF<R> = ObservableF<R>(defValue).also { toRet -> observePrivate<Float>(o, { toRet.set(obFun(it)) }, *obs) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    fun <R> observe(o: ObservableFloat, vararg obs: Observable, obFun: suspend (data: Float) -> R): ObservableField<R> = ObservableField<R>().also { toRet -> observePrivate<Float>(o, { toRet.set(obFun(it)) }, *obs) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    fun <R> observe(o: ObservableDouble, defValue: R, vararg obs: Observable, obFun: suspend (data: Double) -> R): ObservableF<R> = ObservableF<R>(defValue).also { toRet -> observePrivate<Double>(o, { toRet.set(obFun(it)) }, *obs) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    fun <R> observe(o: ObservableDouble, vararg obs: Observable, obFun: suspend (data: Double) -> R): ObservableField<R> = ObservableField<R>().also { toRet -> observePrivate<Double>(o, { toRet.set(obFun(it)) }, *obs) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    fun <R, T> observe(o: ObservableField<T>, defValue: R, vararg obs: Observable, obFun: suspend (data: T) -> R): ObservableF<R> = ObservableF<R>(defValue).also { toRet -> observePrivate<T>(o, { toRet.set(obFun(it)) }, *obs) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    fun <R, T> observe(o: ObservableField<T>, vararg obs: Observable, obFun: suspend (data: T) -> R): ObservableField<R> = ObservableField<R>().also { toRet -> observePrivate<T>(o, { toRet.set(obFun(it)) }, *obs) }



    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing if the value of [o] is null or already changed */
    private fun <T> observePrivate(o: Observable, obFun: suspend (data: T) -> Unit, vararg obs: Observable) {
        val obsId = observablesId.incrementAndGet()
        val helper = SamaObservableHelper(obsId, null, null)
        observableMap[obsId] = helper

        val f: suspend () -> Unit = {
            helper.job?.cancel()
            helper.job = scope.launch {
                delay(observerDelay)
                if(isPaused) return@launch
                if(!isActive) return@launch
                (o.get() as? T?)?.let { logVerbose(it.toString()); obFun(it) }
                helper.onStart = null
            }
        }

        observables.addAll( obs.map { SamaInnerObservable(it, it.onChange(scope) { helper.onStart = f; f() }) } )

        //sets the function to call when using an observable and runs it now
        observables.add( SamaInnerObservable(o, o.addOnChangedAndNowBase (scope) { helper.onStart = f; f() }) )
    }


    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed */
    @Suppress("UNCHECKED_CAST")
    fun <T> observe(o: ObservableList<T>, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: ObservableList<T>) -> Unit): Unit where T: Any {
        val obsId = observablesId.incrementAndGet()
        val helper = SamaObservableHelper(obsId, null, null)
        observableMap[obsId] = helper

        val f: suspend () -> Unit = {
            helper.job?.cancel()
            helper.job = scope.launch {
                delay(observerDelay)
                if(isPaused) return@launch
                if(!isActive) return@launch
                o.let { logVerbose(it.toString()); obFun(it) }
                helper.onStart = null
            }
        }

        observables.addAll( obs.map { SamaInnerObservable(it, it.onChange(scope) { helper.onStart = f; f() }) } )

        val c = o.onAnyChange { scope.launch { f() } }
        listObservables.add(SamaInnerListObservable(o as ObservableList<Any>, c as ObservableList.OnListChangedCallback<ObservableList<Any>>))
        if(!skipFirst)
            scope.launch { obFun(o) }
    }


    /** Observes a liveData until this object is destroyed into an observable field. Does not update the observable if the value of the liveData is null */
    fun <T> observeAsOf(liveData: LiveData<T>): ObservableField<T> = ObservableField<T>().also { ob ->
        observe(liveData) { ob.set(it ?: return@observe) }
        ob.set(liveData.value ?: return ob)
    }

    /** Observes a liveData until this object is destroyed into an observableF. Update the observable with [defaultValue] if the value of the liveData is null */
    fun <T> observeAsOf(liveData: LiveData<T>, defaultValue: T): ObservableF<T> = ObservableF<T>(defaultValue).also { ob ->
        observe(liveData) { ob.set(it ?: return@observe) }
        ob.set(liveData.value ?: defaultValue)
    }

    /** Observes a liveData until this object is destroyed, using a custom observer. Useful when liveData is not used in a lifecycleOwner */
    fun <T> observe(liveData: LiveData<T>): LiveData<T> {
        observedLiveData.add(liveData)
        runOnUi { liveData.observeForever(persistentObserver) }
        return liveData
    }

    /** Observes a liveData until this object is destroyed, using a custom observer */
    @Suppress("unchecked_cast")
    fun <T> observe(liveData: LiveData<T>, observerFunction: suspend (data: T) -> Unit): LiveData<T> {
        val observer: Observer<Any?> = Observer { launchOrNow(scope) { observerFunction(it as? T ?: return@launchOrNow) } }
        customObservedLiveData.add(Pair(liveData as LiveData<Any?>, observer))
        runOnUi { liveData.value?.let { observer.onChanged(it) }; liveData.observeForever(observer) }
        return liveData
    }



    fun restartObserver() {
        isPaused = false
        synchronized(observableMap) { observableMap.values.filterNotNull().launch(scope) { it.onStart?.invoke() } }
        synchronized(listObservables) { listObservables.filterNotNull().launch(scope) { x -> x.callback.onChanged(x.ob) } }
    }

    fun stopObserver() {
        isPaused = true
    }

    fun destroyObserver() {
        synchronized(observables) { observables.forEach { it.ob.removeOnPropertyChangedCallback(it.callback) } }
        observables.clear()
        synchronized(listObservables) { listObservables.forEach { it.ob.removeOnListChangedCallback(it.callback) } }
        listObservables.clear()
        runOnUi { observedLiveData.filterNotNull().forEach { it.removeObserver(persistentObserver) } }
        observedLiveData.clear()
        runOnUi { customObservedLiveData.filterNotNull().forEach { it.first.removeObserver(it.second) } }
        customObservedLiveData.clear()
    }

    private inner class SamaObservableHelper (val id: Int, var onStart: (suspend () -> Unit)?, var job: Job?)
    private inner class SamaInnerObservable (val ob: Observable, val callback: Observable.OnPropertyChangedCallback)
    private inner class SamaInnerListObservable (val ob: ObservableList<Any>, val callback: ObservableList.OnListChangedCallback<ObservableList<Any>>)
}
