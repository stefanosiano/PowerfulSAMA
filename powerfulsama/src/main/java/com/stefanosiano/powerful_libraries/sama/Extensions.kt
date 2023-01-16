package com.stefanosiano.powerful_libraries.sama

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference

internal class Extensions

/**
 * Returns a liveData that will be updated with the values of the liveData returned by [f].
 * It's executed through [launch] and delayed by [millis] the first time.
 */
inline fun <T> CoroutineScope.liveData(millis: Long = 0, crossinline f: suspend () -> LiveData<T>): LiveData<T> =
    MediatorLiveData<T>().also { mld ->
        this.launch {
            delay(millis)
            val ld = f()
            withContext(Dispatchers.Main) {
                mld.addSource(ld) {
                    mld.postValue(it)
                }
            }
        }
    }

/**
 * Delays for [millis] milliseconds the current coroutine until [f] returns true or [timeout] milliseconds passed.
 * A negative timeout means the delay will continue forever.
 */
suspend inline fun delayUntil(millis: Long = 100, timeout: Long = 6000, crossinline f: () -> Boolean) {
    if (f()) {
        return
    }
    var passed = 0L
    while (!f() && (timeout < 0 || passed < timeout)) {
        delay(millis); passed += millis
    }
}

/**
 * Calls [f] with [launch] using passed scope, if it's active.
 * If no scope is passed (it's null), [f] is called directly through [runBlocking].
 */
inline fun launchOrNow(c: CoroutineScope?, crossinline f: suspend () -> Unit) {
    if (c?.isActive == false) {
        return
    }
    c?.launch { f() }
        ?: runBlocking { f() }
}

/**
 * Return a copy of the file retrieved through the uri using Android providers into app internal cache directory,
 *  using [fileName].
 */
fun Uri.toFileFromProviders(context: Context, fileName: String): File? =
    tryOrNull {
        File(context.cacheDir, fileName).also { f ->
            context.contentResolver.openInputStream(this)?.use {
                it.into(FileOutputStream(f))
            }
        }
    }
        ?: tryOrNull { File(path) }

/**
 * Retrieves the activity this context is associated with.
 * If no activity is found (e.g. activity destroyed, service, etc.) returns null.
 */
fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) {
            return c
        }
        c = c.baseContext
    }
    return null
}

/** Try to execute [toTry] in a try catch block, return null if an exception is raised. */
inline fun <T> tryOrNull(toTry: () -> T): T? = tryOr(null, toTry)

/** Try to execute [toTry] in a try catch block, return [default] if an exception is raised. */
@Suppress("TooGenericExceptionCaught")
inline fun <T> tryOr(default: T, toTry: () -> T): T =
    try {
        toTry()
    } catch (ignored: Exception) {
        default
    }

/** Execute [toTry] in a try catch block, prints the exception and returns [default] if an exception is raised. */
@Suppress("TooGenericExceptionCaught", "PrintStackTrace")
inline fun <T> tryOrPrint(default: T, toTry: () -> T): T =
    try {
        toTry()
    } catch (e: Exception) {
        e.printStackTrace()
        default
    }

/** Try to execute [toTry] in a try catch block, prints the exception and returns [null] if an exception is raised. */
inline fun <T> tryOrPrint(toTry: () -> T): T? = tryOrPrint(null, toTry)

/** Gets an enum from a string through enumValueOf<T>(). Useful to use in [string?.toEnum<>() ?: default]. */
inline fun <reified T : Enum<T>> String.toEnum(default: T): T =
    tryOr(default) { enumValueOf(this) }

/** Copies content from this [InputStream] to [output], managing open and close stream. */
fun InputStream.into(output: OutputStream) = use { input ->
    output.use { outp ->
        input.copyTo(outp)
    }
}

/**
 * Replace all occurrences except the first one of [old] with [new].
 * Return [missingDelimeter] (which defaults to the string itself) if [old] is not present.
 */
fun String.replaceAfterFirst(old: String, new: String, missingDelimeter: String = this): String =
    if (contains(old)) {
        "${substringBefore(old)}$old${substringAfter(old, "").replace(old, new)}"
    } else {
        missingDelimeter
    }

/** Returns a weakReference to this object. */
fun <T> T.toWeakReference() = WeakReference(this)
