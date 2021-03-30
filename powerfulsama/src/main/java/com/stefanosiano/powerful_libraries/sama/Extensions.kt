package com.stefanosiano.powerful_libraries.sama

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import androidx.databinding.*
import androidx.databinding.Observable
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama.logger
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext


internal val mainThreadHandler by lazy { Handler(Looper.getMainLooper()) }

class LiveDataExtensions




/** Returns a liveData that will be updated with the values of the liveData returned by [f] (executed through [launch] and delayed by [millis] (just the first time)) */
inline fun <T> CoroutineScope.liveData(millis: Long = 0, crossinline f: suspend () -> LiveData<T>): LiveData<T> =
    MediatorLiveData<T>().also { mld -> this.launch { delay(millis); f().let { ld -> withContext(Dispatchers.Main) { mld.addSource(ld) { mld.postValue(it) } } } } }

/** Delays for [millis] milliseconds the current coroutine until [f] returns true or [timeout] milliseconds passed. A negative timeout means the delay will continue forever */
suspend inline fun delayUntil(millis: Long = 100, timeout: Long = 6000, crossinline f: () -> Boolean) {
    if(f()) return
    var passed = 0L
    while(!f() && (timeout < 0 || passed < timeout)) { delay(millis); passed += millis }
}


/** Calls [f] with [launch] using passed scope, if it's active. If no scope is passed (it's null), [f] is called directly through [runBlocking] */
inline fun launchOrNow(c: CoroutineScope?, crossinline f: suspend () -> Unit) { if(c?.isActive == false) return; c?.launch { f() } ?: runBlocking { f() } }

/** Transforms the liveData using the function [onValue] every time it changes, returning another liveData. You can optionally pass a CoroutineContext [context] to execute it in the background */
fun <T, D> LiveData<T>.transform(onValue: (t: T) -> D): LiveData<D> {
    val transformedLiveData = MediatorLiveData<D>()
    transformedLiveData.addSource(this) {
        transformedLiveData.postValue(onValue(it))
    }
    return transformedLiveData
}

/** Returns a liveData which returns values only when they change. You can optionally pass a CoroutineContext [context] to execute it in the background */
fun <T> LiveData<T>.getDistinct(): LiveData<T> = getDistinctBy { it as Any }

/** Returns a liveData which returns values only when they change. You can optionally pass a CoroutineContext [context] to execute it in the background */
fun <T> LiveData<T>.getDistinctBy(function: (T) -> Any): LiveData<T> {
    val distinctLiveData = MediatorLiveData<T>()

    distinctLiveData.addSource(this, object : Observer<T> {
        private var lastObj: T? = null

        override fun onChanged(obj: T?) {
            if (lastObj != null && obj != null && function(lastObj!!) == function(obj)) return

            lastObj = obj
            distinctLiveData.postValue(lastObj)
        }
    })
    return distinctLiveData
}

/** Returns a liveData which returns values only when they change. You can optionally pass a CoroutineContext [context] to execute it in the background */
fun <T> LiveData<List<T>>.getListDistinct(): LiveData<List<T>> = this.getListDistinctBy { it as Any }

/** Returns a liveData which returns values only when they change. You can optionally pass a CoroutineContext [context] to execute it in the background */
fun <T> LiveData<List<T>>.getListDistinctBy(function: (T) -> Any): LiveData<List<T>> {
    val distinctLiveData = MediatorLiveData<List<T>>()

    distinctLiveData.addSource(this, object : Observer<List<T>> {
        private var lastObj: List<T>? = null

        override fun onChanged(obj: List<T>?) {
                if (lastObj != null &&
                    obj?.size == lastObj?.size &&
                    compareListsContent(obj ?: ArrayList(), lastObj ?: ArrayList(), function)
                ) return

                lastObj = obj
                distinctLiveData.postValue(lastObj)
        }

        private inline fun compareListsContent(list1: List<T>, list2: List<T>, compare: (T) -> Any): Boolean {
            for(i in list1.indices) tryOrNull { if(compare( list1[i] ) != compare( list2[i] )) return false } ?: return false; return true
        }
    })
    return distinctLiveData
}


/** Run [f] on ui thread, waits for its completion and return its value */
@Deprecated("Use corountines: launch(Dispatchers.Main) { ... }")
fun <T> runOnUiAndWait(f: () -> T): T? {
    if(Looper.myLooper() == mainThreadHandler.looper) return f.invoke()
    var ret: T? = null
    var finished = false
    runBlocking { runOnUi { if(isActive) ret = f(); finished = true }; while (isActive && !finished) delay(10) }
    return ret
}

/** Run [f] on ui thread */
@Deprecated("Use corountines: launch(Dispatchers.Main) { ... }")
fun runOnUi(f: () -> Unit) { if(Looper.myLooper() == mainThreadHandler.looper) f.invoke() else mainThreadHandler.post { f.invoke() } }

/** Return a copy of the file retrieved through the uri using Android providers into app internal cache directory, using [fileName] */
fun Uri.toFileFromProviders(context: Context, fileName: String): File? =
    tryOrNull { File(context.cacheDir, fileName).also { f -> context.contentResolver.openInputStream(this)?.use { it.into(FileOutputStream(f)) } } }
        ?: tryOrNull { File(path) }

/** Retrieves the activity this context is associated with. If no activity is found (e.g. activity destroyed, service, etc.) returns null */
fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}


class ObservableFieldExtensions

/** Called by an Observable whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun <T> ObservableField<T>.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (T?) -> Unit) = onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableBoolean.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Boolean) -> Unit ) = onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/** Called by an Observable whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun ObservableByte.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Byte) -> Unit ) = onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

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

/** Called by an Observable whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun Observable.addOnChangedAndNowBase(c: CoroutineScope? = null, crossinline f: suspend (Any?) -> Unit ) = onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/** Calls [f] whenever an observable property changes. It also runs the same function now. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun Observable.onChangeAndNow(c: CoroutineScope? = null, crossinline f: suspend () -> Unit) = onChange(c, f).also { launchOrNow(c) { f() } }

/** Calls [f] whenever an observable property changes. You can optionally pass a CoroutineScope [c] to execute it in the background */
inline fun Observable.onChange(c: CoroutineScope? = null, crossinline f: suspend () -> Unit) =
    object : Observable.OnPropertyChangedCallback() { override fun onPropertyChanged(o: Observable?, id: Int) { launchOrNow(c) { f() } } }.also { addOnPropertyChangedCallback(it) }

/** Calls [f] whenever an observable property changes */
fun Observable.onPropertyChanged(f: () -> Unit): Observable.OnPropertyChangedCallback {
    val callback = object : Observable.OnPropertyChangedCallback() { override fun onPropertyChanged(o: Observable?, id: Int) { f() } }
    addOnPropertyChangedCallback(callback)
    return callback
}

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

/** Run a function for each element in this SparseArray */
fun <T> SparseArray<T>.forEach(f: ((T) -> Unit)?) {
    for (i in 0 until size())
        f?.invoke(valueAt(i))
}

/** Gets an enum from a string through enumValueOf<T>(). Useful to use in [string?.toEnum<>() ?: default] */
inline fun <reified T : Enum<T>> String.toEnum(default: T) : T = try{ enumValueOf(this) } catch (e: Exception) { default }

/** Copies content from this [InputStream] to [output], managing open and close stream */
fun InputStream.into(output: OutputStream) = use { inp -> output.use { outp -> inp.copyTo(outp) } }

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

/** Returns `true` if all of [elements] are found in the array */
fun <T> Array<out T>.contains(elements: Collection<T>): Boolean =
    elements.map { indexOf(it) >= 0 }.firstOrNull { !it } ?: true


/** Replace all occurrences except the first one of [old] with [new].
 * Return [missingDelimeter] (which defaults to the string itself) if [old] is not present */
fun String.replaceAfterFirst(old: String, new: String, missingDelimeter: String = this): String =
    if(contains(old)) "${substringBefore(old)}$old${substringAfter(old, "").replace(old, new)}"
    else missingDelimeter

/** Returns a list containing only elements matching the given [predicate] */
suspend fun <T> Iterable<T>.filterK(predicate: suspend (T) -> Boolean): List<T> {
    val filtered = ArrayList<T>()
    this.runAndWait { if (predicate(it)) filtered.add(it) }
    return filtered
}

/** Returns a list containing the results of applying the given [transform] function to each element in the original collection */
suspend fun <T, R> Iterable<T>.mapK(transform: suspend (T) -> R): List<R> {
    val destination = ArrayList<R>()
    this.runAndWait { destination.add(transform(it)) }
    return destination
}

/** Returns a list containing the results of applying the given [transform] function to each element in the original collection */
suspend fun <T, R> Iterable<T>.mapKNotNull(transform: suspend (T) -> R?): List<R> {
    val destination = ArrayList<R>()
    this.runAndWait { transform(it)?.let { t -> destination.add(t) } }
    return destination
}

/** Groups elements of the original collection by the key returned by the given [keySelector] function
 * applied to each element and puts to a map each group key associated with a list of corresponding elements */
suspend fun <T, K> Iterable<T>.groupByK(keySelector: suspend (T) -> K): MutableMap<in K, MutableList<T>> {
    val destination = LinkedHashMap<K, MutableList<T>>()
    runAndWait {
        val key = keySelector(it)
        val list = destination.getOrPut(key) { ArrayList<T>() }
        list.add(it)
    }
    return destination
}

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

