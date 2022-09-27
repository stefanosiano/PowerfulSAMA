package com.stefanosiano.powerful_libraries.sama.extensions

import android.util.SparseArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Suppress("UnusedPrivateClass")
private class CollectionsExtensions

/** Run a function for each element and wait for the completion of all of them, using coroutines. */
inline fun <T> Iterable<T>.runAndWait(crossinline run: suspend (x: T) -> Unit) =
    runBlocking { map { async { run(it) } }.map { it.await() } }

/** Run a function for each element, using coroutines. */
inline fun <T> Iterable<T>.launch(coroutineScope: CoroutineScope, crossinline run: suspend (x: T) -> Unit) =
    coroutineScope.launch { map { async { run(it)} } }

/** Run a function for each element in this SparseArray. */
fun <T> SparseArray<T>.forEach(f: ((T) -> Unit)?) {
    for (i in 0 until size()) {
        f?.invoke(valueAt(i))
    }
}

/** Returns `true` if all of [elements] are found in the array. */
fun <T> Array<out T>.contains(elements: Collection<T>): Boolean =
    elements.map { indexOf(it) >= 0 }.firstOrNull { !it } ?: true

/** Retrieves the key bound to the passed [value]. Returns the first key found, or null. */
fun <K, V> Map<K, V>.getKey(value: V): K? = this.filterValues { it == value }.keys.firstOrNull()

/** Removes all items that satisfy [filter] predicate. */
inline fun <K, V, M: MutableMap<K, V>> M.removeWhen(filter: (Map.Entry<K, V>) -> Boolean): M {
    keys.removeAll(this.filter { filter(it) }.keys)
    return this
}

/** Removes all items that satisfy [filter] predicate. */
inline fun <E, M: MutableCollection<E>> M.removeWhen(filter: (E) -> Boolean): M {
    removeAll(this.filter { filter(it) }.toSet())
    return this
}

/** For loop that uses iterator item (useful to modify elements in a list without concurrentModification exceptions). */
inline fun <T: Iterable<S>, S> T.iterate(f: (S) -> Unit): T {
    val i = iterator()
    while (i.hasNext()) {
        f(i.next())
    }
    return this
}

/** For loop that uses iterator item (useful to modify elements in a list without concurrentModification exceptions). */
inline fun <T: Iterable<S>, S> T.iterateIndexed(f: (S, index: Int) -> Unit): T {
    val i = iterator()
    var index = 0
    while (i.hasNext()) {
        f(i.next(), index)
        index++
    }
    return this
}
