package com.stefanosiano.powerful_libraries.sama.utils

import androidx.databinding.ObservableField
import java.lang.ref.WeakReference

/** Returns a pair made up of weakReferences */
open class WeakPair<T, R>(first: T, second: R) {
    val p = Pair(WeakReference(first), WeakReference(second))

    /** Return first item */
    fun first() : T? = p.first.get()

    /** Return second item */
    fun second() : R? = p.second.get()

    /** Clear internal weakReferences */
    fun clear() { p.first.clear(); p.second.clear() }
}

/** An [ObservableField] with non-nullable values */
class ObservableF<T>(value: T) : ObservableField<T>(value) {
    override fun get(): T = super.get()!!
    override fun set(value: T) = super.set(value)
}
