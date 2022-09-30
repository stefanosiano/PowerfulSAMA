package com.stefanosiano.powerful_libraries.sama.extensions

import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Suppress("UnusedPrivateClass")
private class HigherOrderExtensions

/**
 * Delays for [millis] milliseconds the current coroutine until [f] returns true or [timeout] milliseconds passed.
 * A negative timeout means the delay will continue forever.
 */
suspend inline fun delayUntil(millis: Long = 100, timeout: Long = 6000, crossinline f: () -> Boolean) {
    if(f()) return
    var passed = 0L
    while(!f() && (timeout < 0 || passed < timeout)) { delay(millis); passed += millis }
}


/**
 * Calls [f] with [launch] using passed scope, if it's active.
 * If no scope is passed (it's null), [f] is called directly through [runBlocking].
 */
inline fun launchOrNow(c: CoroutineScope?, crossinline f: suspend () -> Unit) {
    if(c?.isActive == false) return
    c?.launch { f() } ?: runBlocking { f() }
}

/** Run [f] on ui thread, waits for its completion and return its value. */
@Deprecated("Use corountines: launch(Dispatchers.Main) { ... }")
fun <T> runOnUiAndWait(f: () -> T): T? {
    if(Looper.myLooper() == mainThreadHandler.looper) return f.invoke()
    var ret: T? = null
    var finished = false
    runBlocking {
        runOnUi { if(isActive) ret = f(); finished = true }
        while (isActive && !finished) {
            delay(10)
        }
    }
    return ret
}

/** Run [f] on ui thread. */
@Deprecated("Use corountines: launch(Dispatchers.Main) { ... }")
fun runOnUi(f: () -> Unit) {
    if(Looper.myLooper() == mainThreadHandler.looper) f.invoke() else mainThreadHandler.post { f.invoke() }
}

/** Try to execute [toTry] in a try catch block, return [default] if an exception occurs. */
inline fun <T> tryOr(default: T, toTry: () -> T): T = tryOrNull(toTry) ?: default

/** Try to execute [toTry] in a try catch block, return null if an exception occurs. */
inline fun <T> tryOrNull(toTry: () -> T): T? = try { toTry() } catch (ignored: Exception) { null }

/** Try to execute [toTry] in a try catch block, prints the exception and returns [default] if an exception occurs. */
inline fun <T> tryOrPrint(default: T, toTry: () -> T): T = tryOrPrint(toTry) ?: default

/** Try to execute [toTry] in a try catch block, prints the exception and returns [null] if an exception occurs. */
@Suppress("PrintStackTrace")
inline fun <T> tryOrPrint(toTry: () -> T): T? {
    return try {
        toTry()
    } catch (expected: Exception) {
        expected.printStackTrace()
        null
    }
}
