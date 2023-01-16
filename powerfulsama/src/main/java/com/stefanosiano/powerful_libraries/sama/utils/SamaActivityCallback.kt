package com.stefanosiano.powerful_libraries.sama.utils

import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import java.util.concurrent.atomic.AtomicLong

internal class SamaActivityCallback(
    private val onCreate: ((activity: SamaActivity) -> Unit)? = null,
    private val onStart: ((activity: SamaActivity) -> Unit)? = null,
    private val onResume: ((activity: SamaActivity) -> Unit)? = null,
    private val onPause: ((activity: SamaActivity) -> Unit)? = null,
    private val onStop: ((activity: SamaActivity) -> Unit)? = null,
    private val onDestroy: ((activity: SamaActivity) -> Unit)? = null,
    private val onSaveInstanceState: ((activity: SamaActivity) -> Unit)? = null
) {
    val uid = id.incrementAndGet()

    fun onCreate(activity: SamaActivity) = onCreate?.invoke(activity)
    fun onStart(activity: SamaActivity) = onStart?.invoke(activity)
    fun onResume(activity: SamaActivity) = onResume?.invoke(activity)
    fun onPause(activity: SamaActivity) = onPause?.invoke(activity)
    fun onStop(activity: SamaActivity) = onStop?.invoke(activity)
    fun onDestroy(activity: SamaActivity) = onDestroy?.invoke(activity)
    fun onSaveInstanceState(activity: SamaActivity) = onSaveInstanceState?.invoke(activity)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SamaActivityCallback) return false

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int = uid.hashCode()

    companion object {
        val id = AtomicLong()
    }
}
