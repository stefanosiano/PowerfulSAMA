package com.stefanosiano.powerful_libraries.sama.extensions

import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

@Suppress("UnusedPrivateClass")
private class InternalExtensions

/**
 * Creates a [CoroutineExceptionHandler] calling [PowerfulSama.onCoroutineException]
 *  in case of error and logs the stackTrace.
 */
internal fun CoroutineScope.coroutineSamaHandler(job: Job): CoroutineContext =
    job + CoroutineExceptionHandler { _, t -> logException(t) }

internal fun Any.logVerbose(m: String) { PowerfulSama.logger?.logVerbose(this::class.java, m) }
internal fun Any.logDebug(m: String) { PowerfulSama.logger?.logDebug(this::class.java, m) }
internal fun Any.logInfo(m: String) { PowerfulSama.logger?.logInfo(this::class.java, m) }
internal fun Any.logWarning(m: String) { PowerfulSama.logger?.logWarning(this::class.java, m) }
internal fun Any.logError(m: String) { PowerfulSama.logger?.logError(this::class.java, m) }
internal fun Any.logException(t: Throwable) { PowerfulSama.logger?.logException(this::class.java, t) }
internal fun Any.logExceptionWorkarounded(t: Throwable) {
    PowerfulSama.logger?.logExceptionWorkarounded(this::class.java, t)
}
