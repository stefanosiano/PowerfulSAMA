package com.stefanosiano.powerful_libraries.sama.view

import androidx.databinding.*
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Ignore
import com.stefanosiano.powerful_libraries.sama.*
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.logVerbose
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

abstract class SamaListItem : CoroutineScope {


    /** List of observable callbacks that will be observed until the viewModel is destroyed */
    @Ignore private val observables = ArrayList<Pair<Observable, Observable.OnPropertyChangedCallback>>()
    /** List of observable lists callbacks that will be observed until the viewModel is destroyed */
    @Ignore private val listObservables = ArrayList<Pair<ObservableList<Any>, ObservableList.OnListChangedCallback<ObservableList<Any>>>>()

    @Ignore private val observablesMap = ConcurrentHashMap<Long, AtomicInteger>()
    @Ignore private val observablesId = AtomicLong(0)


    @Ignore private val coroutineJob: Job = SupervisorJob()
    @Ignore override val coroutineContext = coroutineSamaHandler(coroutineJob)

    @Ignore internal var onPostAction : (suspend (SamaListItemAction?, SamaListItem) -> Unit)? = null
    @Ignore internal var isLazyInit = false

    @Ignore internal var updateJob: Job? = null

    /** current position given by the [SamaRvAdapter] (0 at beginning). Surely set in [onBind] and [onBindInBackground] */
    @Ignore var adapterPosition: Int = 0
        internal set

    /** current adapter size given by the [SamaRvAdapter] (0 at beginning). It may be inaccurate on item list reload. Surely set in [onBind] and [onBindInBackground] */
    @Ignore var adapterSize: Int = 0
        internal set



    /** Calls the listener set to the [SamaRvAdapter] through [SamaRvAdapter.observe] after [millis] milliseconds, optionally passing an [action].
     * If called again before [millis] milliseconds are passed, previous call is cancelled */
    protected fun postAction(action: SamaListItemAction? = null, millis: Long = 0) { updateJob?.cancel(); updateJob = launch { delay(millis); if(isActive) onPostAction?.invoke(action, this@SamaListItem) } }

    /** Sets a listener through [SamaRvAdapter] to be called by the item */
    internal fun setPostActionListener(f: suspend (SamaListItemAction?, SamaListItem) -> Unit) { onPostAction = f }

    /** Returns the unique id of the item (defaults to [RecyclerView.NO_ID]). Overrides [getStableIdString] if specified */
    open fun getStableId(): Long = RecyclerView.NO_ID

    /** Returns the unique id (as a string) of the item (defaults to an empty string). It's converted to a long in the adapter. Is overridden by [getStableId] if specified */
    open fun getStableIdString(): String = ""

    /** Returns the viewType of the item. Use it to provide different layouDefaults to -1 */
    open fun getViewType() = -1

    /** Called when it's bound to the view */
    open fun onBind(initObjects: Map<String, Any>) { launch { onBindInBackground(initObjects) } }

    /** Called when it's bound to the view, in background after [onBind] */
    protected open suspend fun onBindInBackground(initObjects: Map<String, Any>) {}

    /** Compares this to another item to decide if they are the same when the list is reloaded. By default it calls == */
    open fun contentEquals(other: SamaListItem) = this == other

    /** Called when the view is reattached to the recyclerview after being detached or the adapter has been reattached after being detatched. Use it if you need to reuse resources freed in [onStop]. By default restart all [observe] methods  */
    open fun onStart() {
        synchronized(observables) { observables.asSequence().forEach { tryOrNull { it.first.addOnPropertyChangedCallback(it.second) } } }
        synchronized(listObservables) { listObservables.forEach { tryOrNull { it.first.addOnListChangedCallback(it.second) } } }
    }

    /** Called when the view is detached from the recyclerview or the adapter is detached. Use it if you need to stop some heavy computation. By default it stops all [observe] methods */
    open fun onStop() {
        synchronized(observables) { observables.forEach { tryOrNull { it.first.removeOnPropertyChangedCallback(it.second) } } }
        synchronized(listObservables) { listObservables.forEach { tryOrNull { it.first.removeOnListChangedCallback(it.second) } } }
    }

    /** Called when it's removed from the recyclerview, or its view was recycled or the recyclerView no longer observes the adapter.
     * [canBeReused] determines whether the item may be reused later (the view was recycled, but the item is still present in the adapter)
     * Use it to completely clear any resource. Its coroutines are cancelled here */
    open fun onDestroy(canBeReused: Boolean) {
        onStop()
        observables.forEach { tryOrNull { it.first.removeOnPropertyChangedCallback(it.second) } }
        observables.clear()
        listObservables.forEach { tryOrNull { it.first.removeOnListChangedCallback(it.second) } }
        listObservables.clear()
        if(canBeReused) coroutineContext.cancelChildren()
        else coroutineContext.cancel()
    }

    /** Return if it was lazy initialized. Use it with [onLazyInit] */
    internal fun isLazyInitialized(): Boolean { return isLazyInit }

    /** Called in background only once when it should be initialized (it's about to be shown or the initialization background thread reaches it). [isLazyInitialized] is used to understand if the item was already lazy initialized */
    open suspend fun onLazyInit() { isLazyInit = true }


    /** Interface that indicates the action of the ListItem sent */
    interface SamaListItemAction












    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    @Suppress("UNCHECKED_CAST")
    protected fun <T> observe(o: ObservableList<T>, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: ObservableList<T>) -> Unit): Unit where T: Any {
        val obsId = observablesId.incrementAndGet()
        obs.forEach { ob ->
            observablesMap[obsId] = AtomicInteger(0)
            synchronized(observables) {
                observables.add(Pair(ob, ob.onChange(this) {
                    //increment value of observablesMap[obsId] -> only first call can run this function
                    val id = observablesMap[obsId]?.incrementAndGet() ?: 1
                    if (id != 1) return@onChange
                    o.let { logVerbose("$adapterPosition - $it"); obFun(it) }
                    //clear value of observablesMap[obsId] -> everyone can run this function
                    observablesMap[obsId]?.set(0)
                }))
            }
        }

        val c = o.onAnyChange {
            launchOrNow(this) {
                observablesMap[obsId]?.set(2)
                logVerbose("$adapterPosition - $o")
                obFun(it)
                observablesMap[obsId]?.set(0)
            }
        }
        synchronized(listObservables) { listObservables.add(Pair(o as ObservableList<Any>, c as ObservableList.OnListChangedCallback<ObservableList<Any>>)) }
        if(!skipFirst)
            launchOrNow(this) { obFun(o) }
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


    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing if the value of [o] is null or already changed */
    private fun <T> observePrivate(o: Observable, obValue: () -> T?, obFun: suspend (data: T) -> Unit, skipFirst: Boolean, vararg obs: Observable) {
        val obsId = observablesId.incrementAndGet()

        obs.forEach { ob ->
            observablesMap[obsId] = AtomicInteger(0)
            synchronized(observables) {
                observables.add(Pair(ob, ob.onChange(this) {
                    //increment value of observablesMap[obsId] -> only first call can run this function
                    val id = observablesMap[obsId]?.incrementAndGet() ?: 1
                    if (id != 1) return@onChange
                    obValue()?.let { logVerbose("${adapterPosition ?: 0} - $it"); obFun(it) }
                    //clear value of observablesMap[obsId] -> everyone can run this function
                    observablesMap[obsId]?.set(0)
                }))
            }
        }
        //sets the function to call when using an observable: it sets the observablesMap[obsId] to 2 (it won't be called by obs), run obFun and finally set observablesMap[obsId] to 0 (callable by everyone)
        when(o) {
            is ObservableInt -> {
                synchronized(observables) {
                    observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                        observablesMap[obsId]?.set(2)
                        obValue()?.let { data -> if (data == it) { logVerbose("${adapterPosition ?: 0} - $data"); obFun(data) } }
                        observablesMap[obsId]?.set(0)
                    }))
                }
            }
            is ObservableShort -> {
                synchronized(observables) {
                    observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                        observablesMap[obsId]?.set(2)
                        obValue()?.let { data -> if (data == it) { logVerbose("${adapterPosition ?: 0} - $data"); obFun(data) } }
                        observablesMap[obsId]?.set(0)
                    }))
                }
            }
            is ObservableLong -> {
                synchronized(observables) {
                    observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                        observablesMap[obsId]?.set(2)
                        obValue()?.let { data -> if (data == it) { logVerbose("${adapterPosition ?: 0} - $data"); obFun(data) } }
                        observablesMap[obsId]?.set(0)
                    }))
                }
            }
            is ObservableFloat -> {
                synchronized(observables) {
                    observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                        observablesMap[obsId]?.set(2)
                        obValue()?.let { data -> if (data == it) { logVerbose("${adapterPosition ?: 0} - $data"); obFun(data) } }
                        observablesMap[obsId]?.set(0)
                    }))
                }
            }
            is ObservableDouble -> {
                synchronized(observables) {
                    observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                        observablesMap[obsId]?.set(2)
                        obValue()?.let { data -> if (data == it) { logVerbose("${adapterPosition ?: 0} - $data"); obFun(data) } }
                        observablesMap[obsId]?.set(0)
                    }))
                }
            }
            is ObservableBoolean -> {
                synchronized(observables) {
                    observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                        observablesMap[obsId]?.set(2)
                        obValue()?.let { data -> if (data == it) { logVerbose("${adapterPosition ?: 0} - $data"); obFun(data) } }
                        observablesMap[obsId]?.set(0)
                    }))
                }
            }
            is ObservableByte -> {
                synchronized(observables) {
                    observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                        observablesMap[obsId]?.set(2)
                        obValue()?.let { data -> if (data == it) { logVerbose("${adapterPosition ?: 0} - $data"); obFun(data) } }
                        observablesMap[obsId]?.set(0)
                    }))
                }
            }
            is ObservableField<*> -> {
                synchronized(observables) {
                    observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                        observablesMap[obsId]?.set(2)
                        obValue()?.let { data -> if (data == it) { logVerbose("${adapterPosition ?: 0} - ${data.toString()}"); obFun(data) } }
                        observablesMap[obsId]?.set(0)
                    }))
                }
            }
        }
    }

}