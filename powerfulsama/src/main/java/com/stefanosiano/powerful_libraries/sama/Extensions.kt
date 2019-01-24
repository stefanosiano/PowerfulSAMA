package com.stefanosiano.powerful_libraries.sama

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.databinding.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference


class LiveDataExtensions

/** Observes a live data using a lambda function instead of an Observer (use this only if you don't need a reference to the observer */
internal fun <T> LiveData<T>.observeLd(lifecycleOwner: LifecycleOwner, observerFunction: (data: T?) -> Unit) = this.observe(lifecycleOwner, Observer { observerFunction.invoke(it) })

/** Returns a liveData which returns values only when they change */
fun <T> LiveData<T>.getDistinct(): LiveData<T> {
    val distinctLiveData = MediatorLiveData<T>()

    distinctLiveData.addSource(this, object : Observer<T> {
        private var lastObj: T? = null

        override fun onChanged(obj: T?) {
            if(lastObj != null && obj == lastObj) return
            lastObj = obj
            distinctLiveData.postValue(lastObj)
        }
    })
    return distinctLiveData
}

/** Returns a list containing only elements matching the given [filterBy] */
fun <T> LiveData<List<T>>.filter(filterBy: (t: T) -> Boolean): LiveData<List<T>> {
    val filterLiveData = MediatorLiveData<List<T>>()
    filterLiveData.addSource(this) { obj -> filterLiveData.postValue(obj?.filter { filterBy.invoke(it) } ?: ArrayList()) }
    return filterLiveData
}

/**
 * Returns a list containing only elements matching the given [filterBy].
 * When [observableField] changes, the list is calculated again.
 */
fun <T> LiveData<List<T>>.filter(observableField: ObservableField<*>, filterBy: (t: T) -> Boolean): LiveData<List<T>> {
    var lastValue: List<T>?
    var lastFullValue: List<T>? = null
    val filterLiveData = MediatorLiveData<List<T>>()

    filterLiveData.addSource(this) { obj ->
        lastFullValue = obj ?: ArrayList()
        lastValue = lastFullValue?.filter { filterBy.invoke(it) } ?: ArrayList()
        filterLiveData.postValue(obj)
    }
    observableField.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            lastValue = lastFullValue?.filter { filterBy.invoke(it) } ?: ArrayList()
            filterLiveData.postValue(lastValue)
        }
    } )
    return filterLiveData
}

/** Returns a list of all elements sorted according to natural sort order of the value returned by specified [sortedBy] function. */
fun <T, R> LiveData<List<T>>.sortedBy(sortedBy: (t: T) -> R): LiveData<List<T>> where R:Comparable<R> {
    val filterLiveData = MediatorLiveData<List<T>>()
    filterLiveData.addSource(this) { obj -> filterLiveData.postValue(obj?.sortedBy { sortedBy.invoke(it) } ?: ArrayList()) }
    return filterLiveData
}

/** Transforms the liveData using the function [onValue] every time it changes, returning another liveData */
fun <T, D> LiveData<T>.transform(onValue: (t: T?) -> D): LiveData<D> {
    val filterLiveData = MediatorLiveData<D>()
    filterLiveData.addSource(this) { obj -> filterLiveData.postValue(onValue.invoke(obj)) }
    return filterLiveData
}



class ObservableFieldExtensions

/** Called by an Observable whenever an observable property changes. */
internal fun BaseObservable.addOnPropertyChangedCallback(onChanged: () -> Unit ){
    this.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) { onChanged.invoke() }
    })
}

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
internal fun <T> ObservableField<T>.addOnChangedAndNow(onChanged: (T?) -> Unit): Observable.OnPropertyChangedCallback
{ val callback = this.onChange{ onChanged.invoke(get()) }; onChanged.invoke(get()); return callback }

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
internal fun ObservableBoolean.addOnChangedAndNow(onChanged: (Boolean) -> Unit ): Observable.OnPropertyChangedCallback
{ val callback = this.onChange{ onChanged.invoke(get()) }; onChanged.invoke(get()); return callback }

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
internal fun ObservableInt.addOnChangedAndNow(onChanged: (Int) -> Unit ): Observable.OnPropertyChangedCallback
{ val callback = this.onChange{ onChanged.invoke(get()) }; onChanged.invoke(get()); return callback }

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
internal fun ObservableShort.addOnChangedAndNow(onChanged: (Short) -> Unit ): Observable.OnPropertyChangedCallback
{ val callback = this.onChange{ onChanged.invoke(get()) }; onChanged.invoke(get()); return callback }

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
internal fun ObservableLong.addOnChangedAndNow(onChanged: (Long) -> Unit ): Observable.OnPropertyChangedCallback
{ val callback = this.onChange{ onChanged.invoke(get()) }; onChanged.invoke(get()); return callback }

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
internal fun ObservableFloat.addOnChangedAndNow(onChanged: (Float) -> Unit ): Observable.OnPropertyChangedCallback
{ val callback = this.onChange{ onChanged.invoke(get()) }; onChanged.invoke(get()); return callback }

/** Called by an Observable whenever an observable property changes. It also runs the same function now */
internal fun ObservableDouble.addOnChangedAndNow(onChanged: (Double) -> Unit ): Observable.OnPropertyChangedCallback
{ val callback = this.onChange{ onChanged.invoke(get()) }; onChanged.invoke(get()); return callback }

private fun Observable.onChange(f:() -> Unit): Observable.OnPropertyChangedCallback {
    val callback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) { f.invoke() }
    }
    addOnPropertyChangedCallback(callback)
    return callback
}




class AndroidExtensions

/** Returns the drawable associated with the id */
fun Context.getCompatDrawable(drawableId: Int) = AppCompatResources.getDrawable(this, drawableId)

/** Returns the color associated with the id */
fun Context.getCompatColor(colorId: Int) = ContextCompat.getColor(this, colorId)

/** Returns the dimension associated with the id in dp */
fun Context.getDimensInDp(dimenId: Int) = resources.getDimension(dimenId)/resources.displayMetrics.density

/** Returns the dimension associated with the id in px */
fun Context.getDimensInPx(dimenId: Int) = resources.getDimension(dimenId)



class Extensions


/** Run a function for each element and wait for the completion of all of them, using coroutines */
fun <T> Iterable<T>.runAndWait(run: (x: T) -> Unit) = runBlocking { map { async {run.invoke(it)} }.map { it.await() } }

/** Run a function for each element, using coroutines */
fun <T> Iterable<T>.launch(coroutineScope: CoroutineScope, run: (x: T) -> Unit) = coroutineScope.launch { map { async {run.invoke(it)} } }

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

/** Returns a weakReference to this object */
fun <T> T.toWeakReference() = WeakReference<T>(this)

/** Removes all items that satisfy [filter] predicate */
fun <K, V> MutableMap<K, V>.removeWhen(filter: (Map.Entry<K, V>) -> Boolean) = this.keys.removeAll(this.filter { filter.invoke(it) }.keys)

/** Removes all items that satisfy [filter] predicate */
fun <E> MutableCollection<E>.removeWhen(filter: (E) -> Boolean) = this.removeAll(this.filter { filter.invoke(it) })

