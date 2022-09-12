package com.stefanosiano.powerful_libraries.sama.utils

import androidx.databinding.ObservableField
import java.lang.ref.WeakReference
import java.math.BigDecimal

/** Returns a pair made up of weakReferences. */
open class WeakPair<T, R>(first: T, second: R) {
    private val p = Pair(WeakReference(first), WeakReference(second))

    /** Return first item. */
    fun first() : T? = p.first.get()

    /** Return second item. */
    fun second() : R? = p.second.get()

    /** Clear internal weakReferences. */
    fun clear() { p.first.clear(); p.second.clear() }
}

/**
 * An [ObservableField] with non-nullable values. You can specify to update the value with [onlyWhen] function.
 * By default the new value will be set only when it's different from old (via != comparison).
 * Supports [BigDecimal], too.
 */
class ObservableF<T>(value: T, onlyWhen: ((old: T, new: T) -> Boolean)? = null) : ObservableField<T>(value) {
    private val setOnlyWhen: (old: T, new: T) -> Boolean = onlyWhen ?: { old: T, new: T -> when {
        new is BigDecimal -> (old as BigDecimal).toDouble() != new.toDouble()
        else -> old != new
    } }

    override fun get(): T = super.get()!!
    override fun set(value: T) { if(setOnlyWhen(get(), value)) super.set(value) }
}
