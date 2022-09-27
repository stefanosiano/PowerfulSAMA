package com.stefanosiano.powerful_libraries.sama.extensions

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
import kotlinx.coroutines.CoroutineScope

@Suppress("UnusedPrivateClass")
private class ObservablesExtensions

/**
 * Called by an Observable whenever an observable property changes.
 * It also runs the same function now.
 * You can optionally pass a CoroutineScope [c] to execute it in the background.
 */
inline fun <T> ObservableField<T>.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (T?) -> Unit) =
    onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/**
 * Called by an Observable whenever an observable property changes.
 * It also runs the same function now.
 * You can optionally pass a CoroutineScope [c] to execute it in the background.
 */
inline fun ObservableBoolean.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Boolean) -> Unit ) =
    onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/**
 * Called by an Observable whenever an observable property changes.
 * It also runs the same function now.
 * You can optionally pass a CoroutineScope [c] to execute it in the background.
 */
inline fun ObservableChar.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Char) -> Unit ) =
    onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/**
 * Called by an Observable whenever an observable property changes.
 * It also runs the same function now.
 * You can optionally pass a CoroutineScope [c] to execute it in the background.
 */
inline fun ObservableByte.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Byte) -> Unit ) =
    onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/**
 * Called by an Observable whenever an observable property changes.
 * It also runs the same function now.
 * You can optionally pass a CoroutineScope [c] to execute it in the background.
 */
inline fun ObservableInt.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Int) -> Unit ) =
    onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/**
 * Called by an Observable whenever an observable property changes.
 * It also runs the same function now.
 * You can optionally pass a CoroutineScope [c] to execute it in the background.
 */
inline fun ObservableShort.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Short) -> Unit ) =
    onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/**
 * Called by an Observable whenever an observable property changes.
 * It also runs the same function now.
 * You can optionally pass a CoroutineScope [c] to execute it in the background.
 */
inline fun ObservableLong.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Long) -> Unit ) =
    onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/**
 * Called by an Observable whenever an observable property changes.
 * It also runs the same function now.
 * You can optionally pass a CoroutineScope [c] to execute it in the background.
 */
inline fun ObservableFloat.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Float) -> Unit ) =
    onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/**
 * Called by an Observable whenever an observable property changes.
 * It also runs the same function now.
 * You can optionally pass a CoroutineScope [c] to execute it in the background.
 */
inline fun ObservableDouble.addOnChangedAndNow(c: CoroutineScope? = null, crossinline f: suspend (Double) -> Unit ) =
    onChange(c) { f(get()) }.also { launchOrNow(c) { f(get()) } }

/**
 * Calls [f] whenever anything on this list changes.
 * To have a better management use [ObservableList.addOnListChangedCallback].
 */
fun <T> ObservableList<T>.onAnyChange(
    f: (ObservableList<T>) -> Unit
): ObservableList.OnListChangedCallback<ObservableList<T>> {
    val callback = object : ObservableList.OnListChangedCallback<ObservableList<T>>() {
        override fun onChanged(sender: ObservableList<T>?) {
            f(sender ?: return)
        }
        override fun onItemRangeRemoved(sender: ObservableList<T>?, positionStart: Int, itemCount: Int) {
            f(sender ?: return)
        }
        override fun onItemRangeMoved(sender: ObservableList<T>?, fromPosition: Int, toPosition: Int, itemCount: Int) {
            f(sender ?: return)
        }
        override fun onItemRangeInserted(sender: ObservableList<T>?, positionStart: Int, itemCount: Int) {
            f(sender ?: return)
        }
        override fun onItemRangeChanged(sender: ObservableList<T>?, positionStart: Int, itemCount: Int) {
            f(sender ?: return)
        }
    }
    addOnListChangedCallback(callback)
    return callback
}
