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
import kotlinx.coroutines.*
import java.lang.ref.WeakReference


internal val mainThreadHandler by lazy { Handler(Looper.getMainLooper()) }

class LiveDataExtensions




/** Returns a liveData that will be updated with the values of the liveData returned by [f] (executed through [launch]) */
inline fun <T> CoroutineScope.liveData(crossinline f: suspend () -> LiveData<T>): LiveData<T> =
    MediatorLiveData<T>().also { mld -> this.launch { f().let { ld -> mld.addSourceLd(ld) { mld.postValue(it) } } } }



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
inline fun <T> runOnUiAndWait(crossinline f: suspend () -> T): T? {
    var ret: T? = null
    var finished = false
    runBlocking { runOnUi { ret = f(); finished = true }; while (!finished) delay(10) }
    return ret
}

/** Run [f] on ui thread */
inline fun runOnUi(crossinline f: suspend () -> Unit) { GlobalScope.launch(Dispatchers.Main) { f() } }



class ObservableFieldExtensions

/** Called by an Observable whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun <T> ObservableField<T>.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (T?) -> Unit) = onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableBoolean.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Boolean) -> Unit ) = onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableInt.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Int) -> Unit ) = onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableShort.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Short) -> Unit ) = onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableLong.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Long) -> Unit ) = onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableFloat.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Float) -> Unit ) = onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableDouble.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Double) -> Unit ) = onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/** Calls [f] whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun Observable.onChangeAndNow(c: CoroutineScope? = null, crossinline f: suspend () -> Unit) = onChange(c, f).also { launchOrNow(c) { f() } }

/** Calls [f] whenever an observable property changes. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun Observable.onChange(c: CoroutineScope? = null, crossinline f: suspend () -> Unit) =
    object : Observable.OnPropertyChangedCallback() { override fun onPropertyChanged(o: Observable?, id: Int) { launchOrNow(c) { f() } } }.also { addOnPropertyChangedCallback(it) }





class AndroidExtensions

/** Returns the drawable associated with the id */
fun Context.compatDrawable(drawableId: Int) = AppCompatResources.getDrawable(this, drawableId)

/** Returns the color associated with the id */
fun Context.compatColor(colorId: Int) = ContextCompat.getColor(this, colorId)

/** Returns the dimension associated with the id in dp */
fun Context.dimensInDp(dimenId: Int) = resources.getDimension(dimenId)/resources.displayMetrics.density

/** Returns the dimension associated with the id in px */
fun Context.dimensInPx(dimenId: Int) = resources.getDimension(dimenId)



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
inline fun <T> tryOrPrint(default: T? = null, toTry: () -> T): T? { return try { toTry() } catch (e: Exception) { e.printStackTrace(); default } }

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

