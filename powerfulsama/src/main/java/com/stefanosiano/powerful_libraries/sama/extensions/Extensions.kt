package com.stefanosiano.powerful_libraries.sama.extensions

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference

internal val mainThreadHandler by lazy { Handler(Looper.getMainLooper()) }

@Suppress("UnusedPrivateClass")
private class Extensions

/**
 * Returns a liveData that will be updated with the values of the liveData returned by [f].
 * [f] is executed through [launch] and delayed by [millis] (just the first time).
 */
inline fun <T> CoroutineScope.liveData(millis: Long = 0, crossinline f: suspend () -> LiveData<T>): LiveData<T> =
    MediatorLiveData<T>().also { mld ->
        this.launch {
            delay(millis)
            f().let { ld ->
                withContext(Dispatchers.Main) {
                    mld.addSource(ld) {
                        mld.postValue(it)
                    }
                }
            }
        }
    }

/** Gets an enum from a string through enumValueOf<T>(). Useful to use in [string?.toEnum<>() ?: default]. */
inline fun <reified T : Enum<T>> String.toEnum(default: T) : T = tryOr(default) { enumValueOf(this) }

/** Copies content from this [InputStream] to [output], managing open and close stream. */
fun InputStream.into(output: OutputStream) = use { inp -> output.use { outp -> inp.copyTo(outp) } }


/** Replace all occurrences except the first one of [old] with [new].
 * Return [missingDelimeter] (which defaults to the string itself) if [old] is not present. */
fun String.replaceAfterFirst(old: String, new: String, missingDelimeter: String = this): String {
    return if (contains(old)) {
        "${substringBefore(old)}$old${substringAfter(old, "").replace(old, new)}"
    } else {
        missingDelimeter
    }
}

/** Returns a weakReference to this object. */
fun <T> T.toWeakReference() = WeakReference(this)
