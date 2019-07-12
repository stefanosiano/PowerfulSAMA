package com.stefanosiano.powerful_libraries.sama.utils

import android.util.LongSparseArray
import android.util.SparseArray
import kotlinx.coroutines.*

/** Simple [LongSparseArray] using [runBlocking] to avoid concurrency */
class KLongSparseArray<T> : LongSparseArray<T>(), CoroutineScope {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineJob + CoroutineExceptionHandler { _, t -> t.printStackTrace(); PowerfulSama.onExceptionWorkarounded?.invoke(this::class.java, t) }

    override fun clear() { runBlocking { launch { super.clear() } } }
    override fun get(key: Long): T? = runBlocking { super.get(key) }
    override fun put(key: Long, value: T) = runBlocking { super.put(key, value) }
}
/** Simple [SparseArray] using [runBlocking] to avoid concurrency */
class KSparseArray<T> : SparseArray<T>(), CoroutineScope {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineJob + CoroutineExceptionHandler { _, t -> t.printStackTrace(); PowerfulSama.onExceptionWorkarounded?.invoke(this::class.java, t) }

    override fun clear() { runBlocking { launch { super.clear() } } }
    override fun get(key: Int): T? = runBlocking { super.get(key) }
    override fun put(key: Int, value: T) = runBlocking { super.put(key, value) }
}