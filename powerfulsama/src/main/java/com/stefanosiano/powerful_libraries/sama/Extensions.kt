package com.stefanosiano.powerful_libraries.sama

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.databinding.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama.logger
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext


internal val mainThreadHandler by lazy { Handler(Looper.getMainLooper()) }

class LiveDataExtensions




/** Returns a liveData that will be updated with the values of the liveData returned by [f] (executed through [launch] and delayed by [millis] (just the first time)) */
inline fun <T> CoroutineScope.liveData(millis: Long = 0, crossinline f: suspend () -> LiveData<T>): LiveData<T> =
    MediatorLiveData<T>().also { mld -> this.launch { delay(millis); f().let { ld -> mld.addSourceLd(ld) { mld.postValue(it) } } } }

/** Runs [f] only when [data] value is not null */
suspend fun <T> CoroutineScope.delayUntilNotNull(data: ObservableField<T>, f: suspend (data: T) -> Unit) { while(data.get() == null) delay(100); return f(data.get()!!) }

/** Delays for [millis] milliseconds the current coroutine until [f] returns true */
suspend fun CoroutineScope.delayUntil(millis: Long = 100, f: () -> Boolean) { while(!f()) delay(millis) }


/** Returns a liveData that will be updated with the values of the liveData returned by [f].
 * If [ob] has a null value, the liveData will be empty until it's set with a non-null value */
suspend fun <T> CoroutineScope.waitFor(ob: ObservableField<out Any>, f: suspend () -> LiveData<T>): LiveData<T> {
    if(ob.get() != null) return f()

    val mediatorLiveData = MediatorLiveData<T>()
    val cb = ob.addOnChangedAndNow(this) {
        if(ob.get() == null) return@addOnChangedAndNow
        f().let { ld -> mediatorLiveData.addSourceLd(ld) { mediatorLiveData.postValue(it) } }
    }
    mediatorLiveData.addSourceLd(mediatorLiveData) { ob.removeOnPropertyChangedCallback(cb); mediatorLiveData.removeSourceLd(mediatorLiveData) }
    return mediatorLiveData
}


/** Calls addSource on main thread. useful when using background threads/coroutines: Calling it in the background throws an exception! */
fun <T, S> MediatorLiveData<T>.addSourceLd(liveData: LiveData<S>, source: (S) -> Unit) where S: Any? = runOnUi { this.addSource(liveData, source) }

/** Calls addSource on main thread. useful when using background threads/coroutines: Calling it in the background throws an exception! */
fun <T, S> MediatorLiveData<T>.addSourceLd(liveData: LiveData<S>, source: Observer<S>) where S: Any? = runOnUi { this.addSource(liveData, source) }

/** Calls removeSource on main thread. useful when using background threads/coroutines: Calling it in the background throws an exception! */
fun <T, S> MediatorLiveData<T>.removeSourceLd(liveData: LiveData<S>) where S: Any? = runOnUi { this.removeSource(liveData) }

/** Observes a live data using a lambda function instead of an Observer (use this only if you don't need a reference to the observer */
internal inline fun <T> LiveData<T>.observeLd(lifecycleOwner: LifecycleOwner, crossinline observerFunction: (data: T) -> Unit) = runOnUi { this.observe(lifecycleOwner, Observer { observerFunction(it) }) }

/** Returns a liveData which returns values only when they change. You can optionally pass a CoroutineContext [context] to execute it in the background */
fun <T> LiveData<T>.getDistinct(context: CoroutineScope? = null): LiveData<T> = getDistinctBy(context) { it as Any }

/** Returns a liveData which returns values only when they change. You can optionally pass a CoroutineContext [context] to execute it in the background */
inline fun <T> LiveData<T>.getDistinctBy(context: CoroutineScope? = null, crossinline function: suspend (T) -> Any): LiveData<T> {
    val distinctLiveData = MediatorLiveData<T>()

    distinctLiveData.addSourceLd(this, object : Observer<T> {
        private var lastObj: T? = null

        override fun onChanged(obj: T?) {
            launchOrNow(context) {
                if (lastObj != null && obj != null && function(lastObj!!) == function(obj)) return@launchOrNow

                lastObj = obj
                distinctLiveData.postValue(lastObj)
            }
        }
    })
    return distinctLiveData
}

/** Returns a liveData which returns values only when they change. You can optionally pass a CoroutineContext [context] to execute it in the background */
fun <T> LiveData<List<T>>.getListDistinct(context: CoroutineScope? = null): LiveData<List<T>> = this.getListDistinctBy(context) { it as Any }

/** Returns a liveData which returns values only when they change. You can optionally pass a CoroutineContext [context] to execute it in the background */
fun <T> LiveData<List<T>>.getListDistinctBy(context: CoroutineScope? = null, function: suspend (T) -> Any): LiveData<List<T>> {
    val distinctLiveData = MediatorLiveData<List<T>>()

    distinctLiveData.addSource(this, object : Observer<List<T>> {
        private var lastObj: List<T>? = null

        override fun onChanged(obj: List<T>?) {
            launchOrNow(context) {
                if (lastObj != null &&
                    obj?.size == lastObj?.size &&
                    compareListsContent(obj ?: ArrayList(), lastObj ?: ArrayList(), function)
                ) return@launchOrNow

                lastObj = obj
                distinctLiveData.postValue(lastObj)
            }
        }

        private suspend inline fun compareListsContent(list1: List<T>, list2: List<T>, crossinline compare: suspend (T) -> Any): Boolean {
            for(i in 0 until list1.size) tryOrNull { if(compare( list1[i] ) != compare( list2[i] )) return false } ?: return false; return true
        }
    })
    return distinctLiveData
}

/** Returns a live data that prints its values to log and then returns itself. Useful for debugging (remove it if not needed!). You can optionally pass a CoroutineScope [c] to execute it in the background */
fun <T> LiveData<List<T>>.print(c: CoroutineScope? = null): LiveData<List<T>> =
    MediatorLiveData<List<T>>().also { ld -> ld.addSourceLd(this) { launchOrNow(c) { it.forEach { ob -> Log.d("LiveData", ob.toString()) }; ld.postValue(it) } } }

/** Returns a list containing only elements matching the given [filterBy]. You can optionally pass a CoroutineContext [c] to execute it in the background */
inline fun <T> LiveData<List<T>>.filter(c: CoroutineScope? = null, crossinline filterBy: suspend (t: T) -> Boolean): LiveData<List<T>> =
    MediatorLiveData<List<T>>().also { ld -> ld.addSourceLd(this) { launchOrNow(c) { ld.postValue(it.filter { ob -> filterBy(ob) }) } } }

/** Calls [f] with [launch] using passed scope, if it's active. If no scope is passed (it's null), [f] is called directly through [runBlocking] */
inline fun launchOrNow(c: CoroutineScope?, crossinline f: suspend () -> Unit) { if(c?.isActive == false) return; c?.launch { f() } ?: runBlocking { f() } }

/** Run [f] to get a [LiveData] every time any of [obs] changes. It return a [LiveData] of the same type as [f] */
fun <T> CoroutineScope.liveDataOnObservableAndNow(vararg obs: ObservableField<*>, f: suspend () -> LiveData<T>): LiveData<T> {
    val mediatorLiveData = MediatorLiveData<T>()
    var lastLiveData: LiveData<T>? = null

    val onChanged = suspend {
        lastLiveData?.also { mediatorLiveData.removeSourceLd(it) }
        lastLiveData = f()
        if(lastLiveData != null) mediatorLiveData.addSourceLd(lastLiveData!!) {
            mediatorLiveData.postValue(it)
        }
    }

    //the first time this function is called nothing is changed, so i force the reload manually
    var lastJob = launch { onChanged.invoke() }
    obs.forEach {
        it.onChange(this) {
            lastJob.cancel()
            lastJob = launch { if(isActive) onChanged() }
        }
    }

    return mediatorLiveData
}

/**
 * Returns a list containing only elements matching the given [filterBy].
 * When [observableField] changes, the list is calculated again.
 * You can optionally pass a CoroutineContext [context] to execute it in the background
 */
fun <T> LiveData<List<T>>.filter(context: CoroutineScope? = null, observableField: ObservableField<*>, filterBy: suspend (t: T) -> Boolean): LiveData<List<T>> {
    var lastValue: List<T>?
    var lastFullValue: List<T>? = null
    val filterLiveData = MediatorLiveData<List<T>>()

    filterLiveData.addSourceLd(this) { obj ->
        launchOrNow(context) {
            lastFullValue = obj
            lastValue = lastFullValue?.filter { filterBy(it) } ?: ArrayList()
            filterLiveData.postValue(obj)
        }
    }
    observableField.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            launchOrNow(context) {
                lastValue = lastFullValue?.filter { filterBy(it) } ?: ArrayList()
                filterLiveData.postValue(lastValue)
            }
        }
    } )
    return filterLiveData
}

/** Returns a list of all elements sorted according to natural sort order of the value returned by specified [sortedBy] function. You can optionally pass a CoroutineContext [context] to execute it in the background */
inline fun <T, R> LiveData<List<T>>.sortedBy(context: CoroutineScope? = null, crossinline sortedBy: (t: T) -> R): LiveData<List<T>> where R:Comparable<R> =
    MediatorLiveData<List<T>>().also { ld -> ld.addSourceLd(this) { obj -> launchOrNow(context) { ld.postValue(obj.sortedBy { sortedBy(it) }) } } }


/** Transforms the liveData using the function [onValue] every time it changes, returning another liveData. You can optionally pass a CoroutineContext [context] to execute it in the background */
inline fun <T, D> LiveData<T>.map(context: CoroutineScope? = null, crossinline onValue: suspend (t: T) -> D): LiveData<D> {
    val filterLiveData = MediatorLiveData<D>()
    filterLiveData.addSourceLd(this) { obj -> launchOrNow(context) { filterLiveData.postValue(onValue(obj)) } }
    return filterLiveData
}


/** Run [f] on ui thread, waits for its completion and return its value */
inline fun <T> runOnUiAndWait(crossinline f: () -> T): T? {
    var ret: T? = null
    var finished = false
    runBlocking { runOnUi { if(isActive) ret = f(); finished = true }; while (isActive && !finished) delay(10) }
    return ret
}

/** Run [f] on ui thread */
fun runOnUi(f: () -> Unit) { if(Looper.myLooper() == mainThreadHandler.looper) f.invoke() else mainThreadHandler.post { f.invoke() } }





class ObservableFieldExtensions

/** Called by an Observable whenever an observable property changes. It also runs the same function now if [skipFirst] is not set. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun <T> ObservableField<T>.addOnChangedAndNow(c: CoroutineScope? = null, skipFirst: Boolean = false, crossinline f: suspend (T?) -> Unit) = onChange(c) { f(get()) }.also { if(!skipFirst) launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now if [skipFirst] is not set. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableBoolean.addOnChangedAndNow(c: CoroutineScope? = null, skipFirst: Boolean = false, crossinline f: suspend (Boolean) -> Unit ) = onChange(c) { f(get()) }.also { if(!skipFirst) launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now if [skipFirst] is not set. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableByte.addOnChangedAndNow(c: CoroutineScope? = null, skipFirst: Boolean = false, crossinline f: suspend (Byte) -> Unit ) = onChange(c) { f(get()) }.also { if(!skipFirst) launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now if [skipFirst] is not set. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableInt.addOnChangedAndNow(c: CoroutineScope? = null, skipFirst: Boolean = false, crossinline f: suspend (Int) -> Unit ) = onChange(c) { f(get()) }.also { if(!skipFirst) launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now if [skipFirst] is not set. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableShort.addOnChangedAndNow(c: CoroutineScope? = null, skipFirst: Boolean = false, crossinline f: suspend (Short) -> Unit ) = onChange(c) { f(get()) }.also { if(!skipFirst) launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now if [skipFirst] is not set. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableLong.addOnChangedAndNow(c: CoroutineScope? = null, skipFirst: Boolean = false, crossinline f: suspend (Long) -> Unit ) = onChange(c) { f(get()) }.also { if(!skipFirst) launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now if [skipFirst] is not set. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableFloat.addOnChangedAndNow(c: CoroutineScope? = null, skipFirst: Boolean = false, crossinline f: suspend (Float) -> Unit ) = onChange(c) { f(get()) }.also { if(!skipFirst) launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now if [skipFirst] is not set. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableDouble.addOnChangedAndNow(c: CoroutineScope? = null, skipFirst: Boolean = false, crossinline f: suspend (Double) -> Unit ) = onChange(c) { f(get()) }.also { if(!skipFirst) launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now if [skipFirst] is not set. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun Observable.addOnChangedAndNowBase(c: CoroutineScope? = null, skipFirst: Boolean = false, crossinline f: suspend (Any?) -> Unit ) = onChange(c) { f(get()) }.also { if(!skipFirst) launchOrNow(c) { f(get()) } }

/** Calls [f] whenever an observable property changes. It also runs the same function now if [skipFirst] is not set. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun Observable.onChangeAndNow(c: CoroutineScope? = null, skipFirst: Boolean = false, crossinline f: suspend () -> Unit) = onChange(c, f).also { if(!skipFirst) launchOrNow(c) { f() } }

/** Calls [f] whenever an observable property changes. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun Observable.onChange(c: CoroutineScope? = null, crossinline f: suspend () -> Unit) =
    object : Observable.OnPropertyChangedCallback() { override fun onPropertyChanged(o: Observable?, id: Int) { launchOrNow(c) { f() } } }.also { addOnPropertyChangedCallback(it) }


/** Calls [f] whenever anything on this list changes. To have a better management use [ObservableList.addOnListChangedCallback] with an [ObservableList.OnListChangedCallback] */
fun <T> ObservableList<T>.onAnyChange(f: (ObservableList<T>) -> Unit): ObservableList.OnListChangedCallback<ObservableList<T>> {
    val callback = object : ObservableList.OnListChangedCallback<ObservableList<T>>() {
        override fun onChanged(sender: ObservableList<T>?) { f(sender ?: return) }
        override fun onItemRangeRemoved(sender: ObservableList<T>?, positionStart: Int, itemCount: Int) { f(sender ?: return) }
        override fun onItemRangeMoved(sender: ObservableList<T>?, fromPosition: Int, toPosition: Int, itemCount: Int) { f(sender ?: return) }
        override fun onItemRangeInserted(sender: ObservableList<T>?, positionStart: Int, itemCount: Int) { f(sender ?: return) }
        override fun onItemRangeChanged(sender: ObservableList<T>?, positionStart: Int, itemCount: Int) { f(sender ?: return) }
    }
    addOnListChangedCallback(callback)
    return callback
}

/** Calls get on the right extending class (e.g. ObservableInt). If no class is found, null is returned */
fun Observable.get() = when(this) {
    is ObservableInt -> get()
    is ObservableShort -> get()
    is ObservableLong -> get()
    is ObservableFloat -> get()
    is ObservableDouble -> get()
    is ObservableBoolean -> get()
    is ObservableByte -> get()
    is ObservableField<*> -> get()
    else -> null
}



class Extensions


/** Run a function for each element and wait for the completion of all of them, using coroutines */
inline fun <T> Iterable<T>.runAndWait(crossinline run: suspend (x: T) -> Unit) = runBlocking { map { async {run(it)} }.map { it.await() } }

/** Run a function for each element, using coroutines */
inline fun <T> Iterable<T>.launch(coroutineScope: CoroutineScope, crossinline run: suspend (x: T) -> Unit) = coroutineScope.launch { map { async {run(it)} } }

/** Try to execute [toTry] in a try catch block, return null if an exception is raised */
inline fun <T> tryOrNull(toTry: () -> T): T? = tryOr(null, toTry)

/** Try to execute [toTry] in a try catch block, return [default] if an exception is raised */
inline fun <T> tryOr(default: T, toTry: () -> T): T { return try { toTry() } catch (e: Exception) { default } }

/** Try to execute [toTry] in a try catch block, prints the exception and returns [default] if an exception is raised */
inline fun <T> tryOrPrint(default: T, toTry: () -> T): T { return try { toTry() } catch (e: Exception) { e.printStackTrace(); default } }

/** Try to execute [toTry] in a try catch block, prints the exception and returns [null] if an exception is raised */
inline fun <T> tryOrPrint(toTry: () -> T): T? { return try { toTry() } catch (e: Exception) { e.printStackTrace(); null } }

/** Gets an enum from a string through enumValueOf<T>(). Useful to use in [string?.toEnum<>() ?: default] */
inline fun <reified T : Enum<T>> String.toEnum(default: T) : T = try{ enumValueOf(this) } catch (e: Exception) { default }

/** Transforms passed [secs] into milliseconds */
fun secsToMillis(secs : Long) :Long = secs * 1000

/** Transforms passed [mins] into milliseconds */
fun minsToMillis(mins : Long) :Long = mins * 60_000

/** Transforms passed [hours] into milliseconds */
fun hoursToMillis(hours : Long) :Long = hours * 3_600_000

/** Transforms passed [days] into milliseconds */
fun daysToMillis(days : Long) :Long = days * 86_400_000

/** Transforms passed [weeks] into milliseconds */
fun weeksToMillis(weeks : Long) :Long = weeks * 7 * 86_400_000

/** Returns a weakReference to this object */
fun <T> T.toWeakReference() = WeakReference<T>(this)

/** Retrieves the key bound to the passed [value]. Returns the first key found, or null */
fun <K, V> Map<K, V>.getKey(value: V): K? = this.filterValues { it == value }.keys.firstOrNull()

/** Removes all items that satisfy [filter] predicate */
inline fun <K, V, M> M.removeWhen(filter: (Map.Entry<K, V>) -> Boolean): M where M: MutableMap<K, V> { this.keys.removeAll(this.filter { filter(it) }.keys); return this }

/** Removes all items that satisfy [filter] predicate */
inline fun <E, M> M.removeWhen(filter: (E) -> Boolean): M where M: MutableCollection<E> { this.removeAll(this.filter { filter(it) }); return this }

/** For loop that uses iterator item (useful to modify elements in a list without concurrentModification exceptions) */
inline fun <T,S> T.iterate(f: (S) -> Unit): T where T: Iterable<S> { val i = iterator(); while(i.hasNext()) { f(i.next()) }; return this }

/** For loop that uses iterator item (useful to modify elements in a list without concurrentModification exceptions) */
inline fun <T,S> T.iterateIndexed(f: (S, index: Int) -> Unit): T where T: Iterable<S> { val i = iterator(); var index = 0; while(i.hasNext()) { f(i.next(), index); index++ }; return this }

/** Creates a [CoroutineExceptionHandler] that calls [PowerfulSama.onCoroutineException] in case of error and logs the stackTrace */
internal fun CoroutineScope.coroutineSamaHandler(job: Job): CoroutineContext = job + CoroutineExceptionHandler { _, t -> logException(t) }

internal fun Any.logVerbose(m: String) { logger?.logVerbose(this::class.java, m) }
internal fun Any.logDebug(m: String) { logger?.logDebug(this::class.java, m) }
internal fun Any.logInfo(m: String) { logger?.logInfo(this::class.java, m) }
internal fun Any.logWarning(m: String) { logger?.logWarning(this::class.java, m) }
internal fun Any.logError(m: String) { logger?.logError(this::class.java, m) }
internal fun Any.logException(t: Throwable) { logger?.logException(this::class.java, t) }
internal fun Any.logExceptionWorkarounded(t: Throwable) { logger?.logExceptionWorkarounded(this::class.java, t) }
