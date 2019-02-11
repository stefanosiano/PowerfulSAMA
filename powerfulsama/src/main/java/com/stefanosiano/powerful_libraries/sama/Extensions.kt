package com.stefanosiano.powerful_libraries.sama

import android.content.Context
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


class LiveDataExtensions

/** Observes a live data using a lambda function instead of an Observer (use this only if you don't need a reference to the observer */
internal inline fun <T> LiveData<T>.observeLd(lifecycleOwner: LifecycleOwner, crossinline observerFunction: (data: T?) -> Unit) = this.observe(lifecycleOwner, Observer { observerFunction.invoke(it) })

/** Returns a liveData which returns values only when they change */
fun <T> LiveData<T>.getDistinct(context: CoroutineScope? = null): LiveData<T> = getDistinctBy(context) { it as Any }

/** Returns a liveData which returns values only when they change */
inline fun <T> LiveData<T>.getDistinctBy(context: CoroutineScope? = null, crossinline function: (T) -> Any): LiveData<T> {
    val distinctLiveData = MediatorLiveData<T>()

    distinctLiveData.addSource(this, object : Observer<T> {
        private var lastObj: T? = null

        override fun onChanged(obj: T?) {
            launchIfActiveOrNull(context) {
                if (lastObj != null && obj != null && function.invoke(lastObj!!) == function.invoke(obj)) return@launchIfActiveOrNull

                lastObj = obj
                distinctLiveData.postValue(lastObj)
            }
        }
    })
    return distinctLiveData
}

/** Returns a liveData which returns values only when they change */
fun <T> LiveData<List<T>>.getListDistinct(context: CoroutineScope? = null): LiveData<List<T>> = this.getListDistinctBy(context) { it as Any }

/** Returns a liveData which returns values only when they change */
inline fun <T> LiveData<List<T>>.getListDistinctBy(context: CoroutineScope? = null, crossinline function: (T) -> Any): LiveData<List<T>> {
    val distinctLiveData = MediatorLiveData<List<T>>()

    distinctLiveData.addSource(this, object : Observer<List<T>> {
        private var lastObj: List<T>? = null

        override fun onChanged(obj: List<T>?) {
            launchIfActiveOrNull(context) {
                if (lastObj != null &&
                    obj?.size == lastObj?.size &&
                    compareListsContent(obj ?: ArrayList(), lastObj ?: ArrayList(), function)
                ) return@launchIfActiveOrNull

                lastObj = obj
                distinctLiveData.postValue(lastObj)
            }
        }

        private inline fun compareListsContent(list1: List<T>, list2: List<T>, compare: (T) -> Any): Boolean {
            for(i in 0 until list1.size)
                if(compare.invoke( tryOrNull { list1[i] } ?: return false ) != compare.invoke( tryOrNull { list2[i] } ?: return false )) return false
            return true
        }
    })
    return distinctLiveData
}

/** Returns a live data that prints its values to log and then returns itself. Useful for debugging (remove it if not needed!) */
fun <T> LiveData<List<T>>.print(context: CoroutineScope? = null): LiveData<List<T>> {
    val printLiveData = MediatorLiveData<List<T>>()
    printLiveData.addSource(this) { obj -> launchIfActiveOrNull(context) { obj?.forEach { Log.d("LiveData", it.toString()) }; printLiveData.postValue(obj ?: ArrayList()) } }
    return printLiveData
}

/** Returns a list containing only elements matching the given [filterBy] */
inline fun <T> LiveData<List<T>>.filter(context: CoroutineScope? = null, crossinline filterBy: (t: T) -> Boolean): LiveData<List<T>> {
    val filterLiveData = MediatorLiveData<List<T>>()
    filterLiveData.addSource(this) { obj -> launchIfActiveOrNull(context) { filterLiveData.postValue(obj?.filter { filterBy.invoke(it) } ?: ArrayList()) } }
    return filterLiveData
}

/** Calls [f] with [launch] using passed context, if it's active. If no context is passed (it's null), [f] is called directly */
inline fun launchIfActiveOrNull(context: CoroutineScope?, crossinline f: () -> Unit) {
    if(context?.isActive == false) return
    context?.launch { f.invoke() }
}

/**
 * Returns a list containing only elements matching the given [filterBy].
 * When [observableField] changes, the list is calculated again.
 */
inline fun <T> LiveData<List<T>>.filter(context: CoroutineScope? = null, observableField: ObservableField<*>, crossinline filterBy: (t: T) -> Boolean): LiveData<List<T>> {
    var lastValue: List<T>?
    var lastFullValue: List<T>? = null
    val filterLiveData = MediatorLiveData<List<T>>()

    filterLiveData.addSource(this) { obj ->
        launchIfActiveOrNull(context) {
            lastFullValue = obj ?: ArrayList()
            lastValue = lastFullValue?.filter { filterBy.invoke(it) } ?: ArrayList()
            filterLiveData.postValue(obj)
        }
    }
    observableField.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            launchIfActiveOrNull(context) {
                lastValue = lastFullValue?.filter { filterBy.invoke(it) } ?: ArrayList()
                filterLiveData.postValue(lastValue)
            }
        }
    } )
    return filterLiveData
}

/** Returns a list of all elements sorted according to natural sort order of the value returned by specified [sortedBy] function. */
inline fun <T, R> LiveData<List<T>>.sortedBy(context: CoroutineScope? = null, crossinline sortedBy: (t: T) -> R): LiveData<List<T>> where R:Comparable<R> {
    val filterLiveData = MediatorLiveData<List<T>>()
    launchIfActiveOrNull(context) { filterLiveData.addSource(this) { obj -> filterLiveData.postValue(obj?.sortedBy { sortedBy.invoke(it) } ?: ArrayList()) } }
    return filterLiveData
}

/** Transforms the liveData using the function [onValue] every time it changes, returning another liveData */
inline fun <T, D> LiveData<T>.map(context: CoroutineScope? = null, crossinline onValue: (t: T?) -> D): LiveData<D> {
    val filterLiveData = MediatorLiveData<D>()
    launchIfActiveOrNull(context) { filterLiveData.addSource(this) { obj -> filterLiveData.postValue(onValue.invoke(obj)) } }
    return filterLiveData
}



class ObservableFieldExtensions

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
inline fun <T> ObservableField<T>.addOnChangedAndNow(crossinline f: (T?) -> Unit): Observable.OnPropertyChangedCallback { val c = onChange{ f.invoke(get()) }; f.invoke(get()); return c }

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
inline fun ObservableBoolean.addOnChangedAndNow(crossinline f: (Boolean) -> Unit ): Observable.OnPropertyChangedCallback { val c = onChange{ f.invoke(get()) }; f.invoke(get()); return c }

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
inline fun ObservableInt.addOnChangedAndNow(crossinline f: (Int) -> Unit ): Observable.OnPropertyChangedCallback { val c = onChange{ f.invoke(get()) }; f.invoke(get()); return c }

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
inline fun ObservableShort.addOnChangedAndNow(crossinline f: (Short) -> Unit ): Observable.OnPropertyChangedCallback { val c = onChange{ f.invoke(get()) }; f.invoke(get()); return c }

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
inline fun ObservableLong.addOnChangedAndNow(crossinline f: (Long) -> Unit ): Observable.OnPropertyChangedCallback { val c = onChange{ f.invoke(get()) }; f.invoke(get()); return c }

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
inline fun ObservableFloat.addOnChangedAndNow(crossinline f: (Float) -> Unit ): Observable.OnPropertyChangedCallback { val c = onChange{ f.invoke(get()) }; f.invoke(get()); return c }

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
inline fun ObservableDouble.addOnChangedAndNow(crossinline f: (Double) -> Unit ): Observable.OnPropertyChangedCallback { val c = onChange{ f.invoke(get()) }; f.invoke(get()); return c }

/** Calls [f] whenever an observable property changes. */
inline fun Observable.onChange(crossinline f:() -> Unit): Observable.OnPropertyChangedCallback {
    val callback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) { f.invoke() }
    }
    addOnPropertyChangedCallback(callback)
    return callback
}




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
inline fun <T> Iterable<T>.runAndWait(crossinline run: (x: T) -> Unit) = runBlocking { map { async {run.invoke(it)} }.map { it.await() } }

/** Run a function for each element, using coroutines */
inline fun <T> Iterable<T>.launch(coroutineScope: CoroutineScope, crossinline run: (x: T) -> Unit) = coroutineScope.launch { map { async {run.invoke(it)} } }

/** Try to execute [toTry] in a try catch block, return null if an exception is raised */
inline fun <T> tryOrNull(toTry: () -> T): T? = tryOr(null, toTry)

/** Try to execute [toTry] in a try catch block, return [default] if an exception is raised */
inline fun <T> tryOr(default: T, toTry: () -> T): T { return try { toTry.invoke() } catch (e: Exception) { default } }

/** Try to execute [toTry] in a try catch block, prints the exception and returns [default] if an exception is raised */
inline fun <T> tryOrPrint(default: T? = null, toTry: () -> T): T? { return try { toTry.invoke() } catch (e: Exception) { e.printStackTrace(); default } }

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

/** Removes all items that satisfy [filter] predicate */
inline fun <K, V> MutableMap<K, V>.removeWhen(filter: (Map.Entry<K, V>) -> Boolean) = this.keys.removeAll(this.filter { filter.invoke(it) }.keys)

/** Removes all items that satisfy [filter] predicate */
inline fun <E> MutableCollection<E>.removeWhen(filter: (E) -> Boolean) = this.removeAll(this.filter { filter.invoke(it) })

