package com.stefanosiano.powerful_libraries.sama.view

import androidx.databinding.*
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Ignore
import com.stefanosiano.powerful_libraries.sama.*
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.logVerbose
import com.stefanosiano.powerful_libraries.sama.utils.ObservableF
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

    @Ignore internal var onPostAction : (suspend (SamaListItemAction?, SamaListItem, Any?) -> Unit)? = null
    @Ignore internal var isLazyInit = false

    @Ignore internal var updateJobs = HashMap<String, Job>()

    /** current position given by the [SamaRvAdapter] (0 at beginning). Use it only in [onBind] and [onBindInBackground].
     * It's not reliable out of these methods! To get the item position call [SamaRvAdapter.getItemPosition] */
    @Ignore var adapterPosition: Int = 0
        internal set

    /** current adapter size given by the [SamaRvAdapter] (0 at beginning). It may be inaccurate on item list reload. Surely set in [onBind] and [onBindInBackground] */
    @Ignore var adapterSize: Int = 0
        internal set



    /** Calls the listener set to the [SamaRvAdapter] through [SamaRvAdapter.observe] after [millis] milliseconds, optionally passing an [action].
     * If called again before [millis] milliseconds are passed, previous call is cancelled */
    protected fun <T> postAction(action: T? = null, millis: Long = 0, data: Any? = null) where T: SamaListItemAction, T: Enum<SamaListItemAction> {
        updateJobs[action?.name ?: ""]?.cancel()
        updateJobs[action?.name ?: ""] = launch { delay(millis); if(isActive) onPostAction?.invoke(action, this@SamaListItem, data) }
    }

    /** Sets a listener through [SamaRvAdapter] to be called by the item */
    internal fun setPostActionListener(f: suspend (SamaListItemAction?, SamaListItem, Any?) -> Unit) { onPostAction = f }

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
        synchronized(observables) { observables.asSequence().forEach { tryOrPrint { it.first.addOnPropertyChangedCallback(it.second) } } }
        synchronized(listObservables) { listObservables.forEach { tryOrPrint { it.first.addOnListChangedCallback(it.second) } } }
    }

    /** Called when the view is detached from the recyclerview or the adapter is detached. Use it if you need to stop some heavy computation. By default it stops all [observe] methods */
    open fun onStop() {
        synchronized(observables) { observables.forEach { tryOrPrint { it.first.removeOnPropertyChangedCallback(it.second) } } }
        synchronized(listObservables) { listObservables.forEach { tryOrPrint { it.first.removeOnListChangedCallback(it.second) } } }
    }

    /** Called when it's removed from the recyclerview, or its view was recycled or the recyclerView no longer observes the adapter.
     * [canBeReused] determines whether the item may be reused later (the view was recycled, but the item is still present in the adapter)
     * Use it to completely clear any resource. Its coroutines are cancelled here */
    open fun onDestroy() {
        onStop()
        observables.forEach { tryOrPrint { it.first.removeOnPropertyChangedCallback(it.second) } }
        observables.clear()
        listObservables.forEach { tryOrPrint { it.first.removeOnListChangedCallback(it.second) } }
        listObservables.clear()
        coroutineContext.cancelChildren()
//        if(canBeReused) coroutineContext.cancelChildren()
//        else coroutineContext.cancel()
    }

    /** Return if it was lazy initialized. Use it with [onLazyInit] */
    internal fun isLazyInitialized(): Boolean { return isLazyInit }

    /** Called in background only once when it should be initialized (it's about to be shown or the initialization background thread reaches it).
     * When using pagedLists this method will be called every time a list update occurs. You may want to avoid it when using pagedLists that may change often */
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



    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableByte, f: (Byte) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableInt, f: (Int) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableShort, f: (Short) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableLong, f: (Long) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableFloat, f: (Float) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableDouble, f: (Double) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableF] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableBoolean, f: (Boolean) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <T, R> observeInto(ob: ObservableField<T>, f: (T?) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableF] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <T, R> observeInto(ob: ObservableF<T>, f: (T) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableByte, f: (Byte) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableInt, f: (Int) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableShort, f: (Short) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableLong, f: (Long) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableFloat, f: (Float) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableDouble, f: (Double) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableF] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableBoolean, f: (Boolean) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <T, R> observeIntoN(ob: ObservableField<T>, f: (T?) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableF] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <T, R> observeIntoN(ob: ObservableF<T>, f: (T) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }


    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now (if [skipFirst] is not set) and whenever [o] or any of [obs] change
     * passing the current value of [o]. Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableByte, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Byte) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now and whenever [o] or any of [obs] change passing the current value of [o].
     * Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableByte, vararg obs: Observable, obFun: suspend (data: Byte) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, false, *obs)


    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now (if [skipFirst] is not set) and whenever [o] or any of [obs] change
     * passing the current value of [o]. Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableInt, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Int) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now and whenever [o] or any of [obs] change passing the current value of [o].
     * Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableInt, vararg obs: Observable, obFun: suspend (data: Int) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, false, *obs)


    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now (if [skipFirst] is not set) and whenever [o] or any of [obs] change
     * passing the current value of [o]. Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableShort, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Short) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now and whenever [o] or any of [obs] change passing the current value of [o].
     * Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableShort, vararg obs: Observable, obFun: suspend (data: Short) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, false, *obs)


    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now (if [skipFirst] is not set) and whenever [o] or any of [obs] change
     * passing the current value of [o]. Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableLong, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Long) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now and whenever [o] or any of [obs] change passing the current value of [o].
     * Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableLong, vararg obs: Observable, obFun: suspend (data: Long) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, false, *obs)


    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now (if [skipFirst] is not set) and whenever [o] or any of [obs] change
     * passing the current value of [o]. Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableFloat, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Float) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now and whenever [o] or any of [obs] change passing the current value of [o].
     * Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableFloat, vararg obs: Observable, obFun: suspend (data: Float) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, false, *obs)


    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now (if [skipFirst] is not set) and whenever [o] or any of [obs] change
     * passing the current value of [o]. Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableDouble, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Double) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now and whenever [o] or any of [obs] change passing the current value of [o].
     * Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableDouble, vararg obs: Observable, obFun: suspend (data: Double) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, false, *obs)


    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now (if [skipFirst] is not set) and whenever [o] or any of [obs] change
     * passing the current value of [o]. Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableBoolean, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Boolean) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now and whenever [o] or any of [obs] change passing the current value of [o].
     * Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableBoolean, vararg obs: Observable, obFun: suspend (data: Boolean) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, false, *obs)


    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now (if [skipFirst] is not set) and whenever [o] or any of [obs] change
     * passing the current value of [o]. Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun <T> observe(o: ObservableField<T>, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: T) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, calling [obFun] (in the background) now and whenever [o] or any of [obs] change passing the current value of [o].
     * Does nothing if [o] is null or already changed. If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun <T> observe(o: ObservableField<T>, vararg obs: Observable, obFun: suspend (data: T) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, false, *obs)


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
                    obValue()?.let { logVerbose("$adapterPosition - $it"); obFun(it) }
                    //clear value of observablesMap[obsId] -> everyone can run this function
                    observablesMap[obsId]?.set(0)
                }))
            }
        }

        synchronized(observables) {
            //sets the function to call when using an observable: it sets the observablesMap[obsId] to 2 (it won't be called by obs), run obFun and finally set observablesMap[obsId] to 0 (callable by everyone)
            observables.add(Pair(o, o.addOnChangedAndNowBase (this, skipFirst) {
                observablesMap[obsId]?.set(2)
                obValue()?.let { data -> if (data == it) { logVerbose("$adapterPosition - $data"); obFun(data) } }
                observablesMap[obsId]?.set(0)
            }))
        }
    }

}