package com.stefanosiano.powerful_libraries.sama

import android.util.SparseArray

internal class CollectionExtensions

/** Run a function for each element in this SparseArray. */
fun <T> SparseArray<T>.forEach(f: ((T) -> Unit)?) {
    for (i in 0 until size()) {
        f?.invoke(valueAt(i))
    }
}

/** Returns `true` if all of [elements] are found in the array. */
fun <T> Array<out T>.contains(elements: Collection<T>): Boolean = elements.any { this.indexOf(it) < 0 }

/** Retrieves the key bound to the passed [value]. Returns the first key found, or null. */
fun <K, V> Map<K, V>.getKey(value: V): K? = this.filterValues { it == value }.keys.firstOrNull()

/** Removes all items that satisfy [filter] predicate. */
inline fun <K, V, M : MutableMap<K, V>> M.removeWhen(filter: (Map.Entry<K, V>) -> Boolean): M {
    this.keys.removeAll(
        this.filter { filter(it) }.keys
    )
    return this
}

/** Removes all items that satisfy [filter] predicate. */
inline fun <E, M : MutableCollection<E>> M.removeWhen(filter: (E) -> Boolean): M {
    this.removeAll(
        this.filter { filter(it) }.toSet()
    )
    return this
}
