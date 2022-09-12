package com.stefanosiano.powerful_libraries.sama.utils

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
import androidx.databinding.ObservableShort
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.stefanosiano.powerful_libraries.sama.get
import com.stefanosiano.powerful_libraries.sama.logException
import com.stefanosiano.powerful_libraries.sama.logVerbose
import com.stefanosiano.powerful_libraries.sama.onAnyChange
import com.stefanosiano.powerful_libraries.sama.onPropertyChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/** Interface that allows a component to observe variables and call methods when they change.
 * The main methods to call are [initObserver], [destroyObserver], [stopObserver] and [startObserver]. */
interface SamaObserver {
    /**
     * Initializes the observer with the current coroutine,
     * used to handle delays on multi-variables observe and to observe liveData on UI.
     */
    fun initObserver(coroutineScope: CoroutineScope)

    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R, T> observe(o: Flow<T>, vararg obs: Observable, obFun: (data: T) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableInt, vararg obs: Observable, obFun: (data: Int) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableShort, vararg obs: Observable, obFun: (data: Short) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableLong, vararg obs: Observable, obFun: (data: Long) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableByte, vararg obs: Observable, obFun: (data: Byte) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableChar, vararg obs: Observable, obFun: (data: Char) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableBoolean, vararg obs: Observable, obFun: (data: Boolean) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableFloat, vararg obs: Observable, obFun: (data: Float) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableDouble, vararg obs: Observable, obFun: (data: Double) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R, T> observe(o: ObservableField<T>, vararg obs: Observable, obFun: (data: T) -> R): ObservableField<R>

    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     */
    fun <T> observe(o: ObservableList<T>, vararg obs: Observable, obFun: (data: List<T>) -> Unit)

    /**
     * Observes a liveData until this object is destroyed into an observable field.
     * Does not update the observable if the value of the liveData is null.
     */
    fun <T> observeAsOf(liveData: LiveData<T>): ObservableField<T>

    /** Observes a liveData until this object is destroyed, using a custom observer. */
    fun <T> observe(liveData: LiveData<T>, vararg obs: Observable, observerFunction: (data: T) -> Unit): LiveData<T>

    /**
     * Run [f] to get a [LiveData] every time any of [o] or [obs] changes, removing the old one.
     * It return a [LiveData] of the same type as [f].
     */
    fun <T> observeAndReloadLiveData(o: ObservableField<*>, vararg obs: Observable, f: () -> LiveData<T>?): LiveData<T>

    /** Start observing the variables and call the methods. */
    fun startObserver()

    /** Stop observing the variables. */
    fun stopObserver()

    /** Clear all references to observed variables and methods, stopping and detaching them. */
    fun destroyObserver()
}


/**
 * Class that implements [SamaObserver].
 * The main methods to call are [initObserver], [destroyObserver], [stopObserver] and [startObserver].
 */
class SamaObserverImpl: SamaObserver {

    var coroutineScope: CoroutineScope? = null

    private val observableMap: HashMap<Int, SamaObservableHelper> = HashMap()
    private val flowMap: HashMap<Int, SamaFlowHelper> = HashMap()
    private val observablesId: AtomicInteger = AtomicInteger()

    private var isPaused: Boolean = true

    /** List of observable callbacks that will be observed until the viewModel is destroyed. */
    private val observables: ArrayList<SamaInnerObservable> = ArrayList()

    /** List of flows that will be observed until the viewModel is destroyed. */
    private val flows: ArrayList<SamaInnerFlowObservable> = ArrayList()

    /** List of observable lists callbacks that will be observed until the viewModel is destroyed. */
    private val listObservables: ArrayList<SamaInnerListObservable> = ArrayList()

    /** List of liveData that will be observed until the viewModel is destroyed. */
    private val customObservedLiveData = ArrayList<Pair<LiveData<Any?>, Observer<Any?>>>()

    /**
     * Initializes the observer with the current coroutine,
     * used to handle delays on multi-variables observe and to observe liveData on UI.
     */
    override fun initObserver(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
    }

    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    override fun <R> observe(o: ObservableInt, vararg obs: Observable, obFun: (data: Int) -> R): ObservableField<R> =
        ObservableField<R>().also { toRet -> observePrivate<Int>(o, { toRet.set(obFun(it)) }, *obs) }


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    override fun <R> observe(
        o: ObservableShort,
        vararg obs: Observable,
        obFun: (data: Short) -> R
    ): ObservableField<R> =
        ObservableField<R>().also { toRet -> observePrivate<Short>(o, { toRet.set(obFun(it)) }, *obs) }


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    override fun <R> observe(o: ObservableLong, vararg obs: Observable, obFun: (data: Long) -> R): ObservableField<R> =
        ObservableField<R>().also { toRet -> observePrivate<Long>(o, { toRet.set(obFun(it)) }, *obs) }


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    override fun <R> observe(o: ObservableByte, vararg obs: Observable, obFun: (data: Byte) -> R): ObservableField<R> =
        ObservableField<R>().also { toRet -> observePrivate<Byte>(o, { toRet.set(obFun(it)) }, *obs) }


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    override fun <R> observe(o: ObservableChar, vararg obs: Observable, obFun: (data: Char) -> R): ObservableField<R> =
        ObservableField<R>().also { toRet -> observePrivate<Char>(o, { toRet.set(obFun(it)) }, *obs) }


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    override fun <R> observe(
        o: ObservableBoolean,
        vararg obs: Observable,
        obFun: (data: Boolean) -> R
    ): ObservableField<R> =
        ObservableField<R>().also { toRet -> observePrivate<Boolean>(o, { toRet.set(obFun(it)) }, *obs) }


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    override fun <R> observe(
        o: ObservableFloat,
        vararg obs: Observable,
        obFun: (data: Float) -> R
    ): ObservableField<R> =
        ObservableField<R>().also { toRet -> observePrivate<Float>(o, { toRet.set(obFun(it)) }, *obs) }


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    override fun <R> observe(
        o: ObservableDouble,
        vararg obs: Observable,
        obFun: (data: Double) -> R
    ): ObservableField<R> =
        ObservableField<R>().also { toRet -> observePrivate<Double>(o, { toRet.set(obFun(it)) }, *obs) }


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    override fun <R, T> observe(
        o: ObservableField<T>,
        vararg obs: Observable,
        obFun: (data: T) -> R
    ): ObservableField<R> =
        ObservableField<R>().also { toRet -> observePrivate<T>(o, { toRet.set(obFun(it)) }, *obs) }


    /**
     * Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background).
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o].
     * Does nothing if the value of [o] is null or already changed.
     */
    private fun <T> observePrivate(o: Observable, obFun: (data: T) -> Unit, vararg obs: Observable) {
        val obsId = observablesId.incrementAndGet()
        val helper = SamaObservableHelper(obsId, null, null)
        synchronized(observableMap) { observableMap[obsId] = helper }

        val f: () -> Unit = {
            helper.job?.cancel()
            helper.job = coroutineScope?.launch {
                if (obs.isNotEmpty()) delay(50L)
                if (isPaused) return@launch
                if (!isActive) return@launch
                (o.get() as? T?)?.let { logVerbose(it.toString()); obFun(it) }
                helper.onStart = null
            }
        }

        synchronized(observables) {
            observables.addAll(obs.map { SamaInnerObservable(it, it.onPropertyChanged { helper.onStart = f; f() }) })
            //sets the function to call when using an observable and runs it now
            observables.add(SamaInnerObservable(o, o.onPropertyChanged { helper.onStart = f; f() }))
        }
        helper.onStart = f
        f()
    }


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> observe(o: ObservableList<T>, vararg obs: Observable, obFun: (data: List<T>) -> Unit) {
        val obsId = observablesId.incrementAndGet()
        val helper = SamaObservableHelper(obsId, null, null)
        synchronized(observableMap) { observableMap[obsId] = helper }

        val f: () -> Unit = {
            helper.job?.cancel()
            helper.job = coroutineScope?.launch {
                if (obs.isNotEmpty()) delay(50L)
                if (isPaused) return@launch
                if (!isActive) return@launch
                o.toList().let { logVerbose(it.toString()); obFun(it) }
                helper.onStart = null
            }
        }

        synchronized(observables) {
            observables.addAll(obs.map { SamaInnerObservable(it, it.onPropertyChanged { helper.onStart = f; f() }) })

            val c = o.onAnyChange { helper.onStart = f; f() }
            listObservables.add(
                SamaInnerListObservable(
                    o as ObservableList<Any>,
                    c as ObservableList.OnListChangedCallback<ObservableList<Any>>
                )
            )
        }
        helper.onStart = f
        f()
    }


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <R, T> observe(o: Flow<T>, vararg obs: Observable, obFun: (data: T) -> R): ObservableField<R> {
        val toRet = ObservableField<R>()
        val obsId = observablesId.incrementAndGet()
        val helper = SamaFlowHelper(obsId, null, null, null)
        synchronized(flowMap) { flowMap[obsId] = helper }

        val f: (t: T) -> Unit = { t ->
            helper.job?.cancel()
            helper.lastValue = t
            helper.job = coroutineScope?.launch {
                if (obs.isNotEmpty()) delay(50L)
                if (isPaused) return@launch
                if (!isActive) return@launch
                logVerbose(t.toString())
                toRet.set(obFun(t))
                helper.onStart = null
            }
        }

        coroutineScope?.launch {
            o.filter { it != null }.catch { logException(it) }.collect {
                helper.onStart = f as (Any) -> Unit
                f(it)
            }
        }
        synchronized(flows) {
            flows.addAll(obs.map {
                SamaInnerFlowObservable(
                    it,
                    it.onPropertyChanged {
                        helper.onStart = f as (Any) -> Unit
                        helper.lastValue?.let { f(it) }
                    }
                )
            })
        }
        helper.onStart = f as (Any) -> Unit
        return toRet
    }


    /**
     * Observes a liveData until this object is destroyed into an observable field.
     * Does not update the observable if the value of the liveData is null.
     */
    override fun <T> observeAsOf(liveData: LiveData<T>): ObservableField<T> = ObservableField<T>().also { ob ->
        observe(liveData) { ob.set(it ?: return@observe) }
        ob.set(liveData.value ?: return ob)
    }

    /** Observes a liveData until this object is destroyed, using a custom observer. */
    @Suppress("unchecked_cast")
    override fun <T> observe(
        liveData: LiveData<T>,
        vararg obs: Observable,
        observerFunction: (data: T) -> Unit
    ): LiveData<T> {

        val obsId = observablesId.incrementAndGet()
        val helper = SamaObservableHelper(obsId, null, null)
        synchronized(observableMap) { observableMap[obsId] = helper }

        val f: () -> Unit = {
            helper.job?.cancel()
            helper.job = coroutineScope?.launch {
                if (obs.isNotEmpty()) delay(50L)
                if (isPaused) return@launch
                if (!isActive) return@launch
                liveData.value?.let {
                    logVerbose(it.toString())
                    observerFunction(it)
                }
                helper.onStart = null
            }
        }

        synchronized(observables) {
            observables.addAll(obs.map {
                SamaInnerObservable(it, it.onPropertyChanged { helper.onStart = f; f() })
            })
        }
        val observer: Observer<Any?> = Observer {
            helper.onStart = f
            f()
        }
        synchronized(customObservedLiveData) { customObservedLiveData.add(Pair(liveData as LiveData<Any?>, observer)) }
        coroutineScope?.launch(Dispatchers.Main) { liveData.observeForever(observer) }

        helper.onStart = f
        f()
        return liveData
    }

    /**
     * Run [f] to get a [LiveData] every time any of [o] or [obs] changes, removing the old one.
     * It return a [LiveData] of the same type as [f].
     */
    override fun <T> observeAndReloadLiveData(
        o: ObservableField<*>,
        vararg obs: Observable,
        f: () -> LiveData<T>?
    ): LiveData<T> {
        val mediatorLiveData = MediatorLiveData<T>()
        var lastLiveData: LiveData<T>? = null
        observePrivate<T>(o, {
            coroutineScope?.launch {
                withContext(Dispatchers.Main) { lastLiveData?.let { mediatorLiveData.removeSource(it) } }
                lastLiveData = f()
                withContext(Dispatchers.Main) {
                    lastLiveData?.let { lld ->
                        mediatorLiveData.addSource(lld) {
                            mediatorLiveData.postValue(it)
                        }
                    }
                }
            }
        }, *obs)

        return mediatorLiveData
    }


    /** Start observing the variables and call the methods. */
    override fun startObserver() {
        isPaused = false
        synchronized(observableMap) {
            observableMap.values.forEach { coroutineScope?.launch { it.onStart?.invoke() } }
        }
        synchronized(flowMap) {
            flowMap.values.forEach { coroutineScope?.launch { it.lastValue?.let { v -> it.onStart?.invoke(v) } } }
        }
    }

    /** Stop observing the variables. */
    override fun stopObserver() {
        isPaused = true
    }

    /** Clear all references to observed variables and methods, stopping and detaching them. */
    override fun destroyObserver() {
        synchronized(observables) {
            observables.forEach { it.ob.removeOnPropertyChangedCallback(it.callback) }
            observables.clear()
        }
        synchronized(flows) {
            flows.forEach { it.ob.removeOnPropertyChangedCallback(it.callback) }
            flows.clear()
        }
        synchronized(listObservables) {
            listObservables.forEach { it.ob.removeOnListChangedCallback(it.callback) }
            listObservables.clear()
        }
        (coroutineScope ?: GlobalScope).launch(Dispatchers.Main) {
            synchronized(customObservedLiveData) {
                customObservedLiveData.forEach { it.first.removeObserver(it.second) }
                customObservedLiveData.clear()
            }
        }
    }

    private inner class SamaObservableHelper(val id: Int, var onStart: (() -> Unit)?, var job: Job?)
    private inner class SamaFlowHelper(
        val id: Int,
        var onStart: ((t: Any) -> Unit)?,
        var job: Job?,
        var lastValue: Any?
    )

    private inner class SamaInnerFlowObservable(val ob: Observable, val callback: Observable.OnPropertyChangedCallback)
    private inner class SamaInnerObservable(val ob: Observable, val callback: Observable.OnPropertyChangedCallback)
    private inner class SamaInnerListObservable(
        val ob: ObservableList<Any>,
        val callback: ObservableList.OnListChangedCallback<ObservableList<Any>>
    )
}
