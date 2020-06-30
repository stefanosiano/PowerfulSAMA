package com.stefanosiano.powerful_libraries.sama.utils

import java.util.*
import java.util.concurrent.atomic.AtomicLong

internal class SamaActivityCallback(
    private val onCreate: (() -> Unit)? = null,
    private val onStart: (() -> Unit)? = null,
    private val onResume: (() -> Unit)? = null,
    private val onPause: (() -> Unit)? = null,
    private val onStop: (() -> Unit)? = null,
    private val onDestroy: (() -> Unit)? = null
) {

    companion object {
        val id = AtomicLong()
    }
    val uid = id.incrementAndGet()

    fun onCreate() = onCreate?.invoke()
    fun onStart() = onStart?.invoke()
    fun onResume() = onResume?.invoke()
    fun onPause() = onPause?.invoke()
    fun onStop() = onStop?.invoke()
    fun onDestroy() = onDestroy?.invoke()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SamaActivityCallback) return false

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }
}