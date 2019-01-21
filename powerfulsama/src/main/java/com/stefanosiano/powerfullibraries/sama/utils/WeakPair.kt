package com.stefanosiano.powerfullibraries.sama.utils

import java.lang.ref.WeakReference

/** Returns a pair made up of weakReferences */
internal class WeakPair<T, R>(first: T, second: R) {
    val p = Pair(WeakReference(first), WeakReference(second))

    /** Return first item */
    fun first() : T? = p.first.get()

    /** Return second item */
    fun second() : R? = p.second.get()

    /** Clear internal weakReferences */
    fun clear() { p.first.clear(); p.second.clear() }
}
