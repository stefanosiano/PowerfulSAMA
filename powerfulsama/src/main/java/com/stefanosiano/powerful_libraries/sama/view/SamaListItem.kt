package com.stefanosiano.powerful_libraries.sama.view

import android.util.SparseArray
import androidx.databinding.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
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

    @Ignore internal var onItemUpdated : (suspend (SamaListItem, SamaListItemAction?) -> Unit)? = null
    @Ignore internal var isLazyInit = false
    @Ignore internal var isStarted = false

    @Ignore internal var updateJob: Job? = null



    /** Calls the listener set to the [SamaRvAdapter] through [SamaRvAdapter.observe] after [millis] milliseconds, optionally passing an [action].
     * If called again before [millis] milliseconds are passed, previous call is cancelled */
    protected fun onItemUpdated(millis: Long = 0, action: SamaListItemAction?) { updateJob?.cancel(); updateJob = launch { delay(millis); if(isActive) onItemUpdated?.invoke(this@SamaListItem, action) } }

    /** Sets a listener through [SamaRvAdapter] to be called by the item */
    internal fun onItemUpdatedListenerSet(f: suspend (SamaListItem, SamaListItemAction?) -> Unit) { onItemUpdated = f }

    /** Returns the unique id of the item (defaults to [RecyclerView.NO_ID]). Overrides [getStableIdString] if specified */
    open fun getStableId(): Long = RecyclerView.NO_ID

    /** Returns the unique id (as a string) of the item (defaults to an empty string). It's converted to a long in the adapter. Is overridden by [getStableId] if specified */
    open fun getStableIdString(): String = ""

    /** Returns the viewType of the item. Use it to provide different layouDefaults to -1 */
    open fun getViewType() = -1

    /** Called when it's bound to the view */
    open fun onBind(initObjects: Map<String, Any>) {  }

    /** Called when it's bound to the view, in background after [onBind] */
    open suspend fun onBindInBackground(initObjects: Map<String, Any>) {}

    /** Compares this to another item to decide if they are the same when the list is reloaded. By default it calls == */
    open fun contentEquals(other: SamaListItem) = this == other

    /** Called when it's removed from the recyclerview or the recyclerView no longer observes the adapter. Always called after [onStop]. Use it to completely clear any resource. Its coroutines are cancelled here */
    open fun onDestroy() { coroutineContext.cancel() }

    /** Called when it's reattached to the recyclerview after being detached. Use it if you need to reuse resources freed in [onStop] */
    open fun onStart() { isStarted = true }

    /** Called when it's removed from the recyclerview, or its view was recycled or the recyclerView no longer observes the adapter. Use it to clear resources, keeping in mind the item may be reused later on */
    open fun onStop() {
        observables.forEach { it.first.removeOnPropertyChangedCallback(it.second) }
        observables.clear()
        listObservables.forEach { it.first.removeOnListChangedCallback(it.second) }
        listObservables.clear()
        isStarted = false
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
            observables.add(Pair(ob, ob.onChange(this) {
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
        listObservables.add(Pair(o as ObservableList<Any>, c as ObservableList.OnListChangedCallback<ObservableList<Any>>))
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
            observables.add(Pair(ob, ob.onChange(this) {
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
                observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableShort -> {
                observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableLong -> {
                observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableFloat -> {
                observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableDouble -> {
                observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableBoolean -> {
                observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableByte -> {
                observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableField<*> -> {
                observables.add(Pair(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
        }
    }

}