package com.stefanosiano.powerful_libraries.sama

import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableByte
import androidx.databinding.ObservableChar
import androidx.databinding.ObservableDouble
import androidx.databinding.ObservableField
import androidx.databinding.ObservableFloat
import androidx.databinding.ObservableInt
import androidx.databinding.ObservableLong
import androidx.databinding.ObservableShort
import kotlinx.coroutines.CoroutineScope

internal class ObservableExtensions

/**
 * Called by an Observable whenever an observable property changes.
 * It also runs the same function now.
 * You can optionally pass a CoroutineScope [c] to execute it in the background.
 */
inline fun Observable.addOnChangedAndNowBase(c: CoroutineScope? = null, crossinline f: suspend (Any?) -> Unit) =
    onChange(c) { f(get()) }
        .also { launchOrNow(c) { f(get()) } }

/**
 * Calls [f] whenever an observable property changes.
 * It also runs the same function now.
 * You can optionally pass a CoroutineScope [c] to execute it in the background.
 */
inline fun Observable.onChangeAndNow(c: CoroutineScope? = null, crossinline f: suspend () -> Unit) =
    onChange(c, f)
        .also { launchOrNow(c) { f() } }

/**
 * Calls [f] whenever an observable property changes.
 * You can optionally pass a CoroutineScope [c] to execute it in the background.
 */
inline fun Observable.onChange(c: CoroutineScope? = null, crossinline f: suspend () -> Unit) =
    object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(o: Observable?, id: Int) {
            launchOrNow(c) { f() }
        }
    }.also { addOnPropertyChangedCallback(it) }

/** Calls [f] whenever an observable property changes. */
fun Observable.onPropertyChanged(f: () -> Unit): Observable.OnPropertyChangedCallback {
    val callback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(o: Observable?, id: Int) = f()
    }
    addOnPropertyChangedCallback(callback)
    return callback
}

/** Calls get on the right extending class (e.g. ObservableInt). If no class is found, null is returned. */
fun Observable.get() = when (this) {
    is ObservableInt -> get()
    is ObservableShort -> get()
    is ObservableLong -> get()
    is ObservableFloat -> get()
    is ObservableDouble -> get()
    is ObservableBoolean -> get()
    is ObservableByte -> get()
    is ObservableChar -> get()
    is ObservableField<*> -> get()
    else -> null
}
