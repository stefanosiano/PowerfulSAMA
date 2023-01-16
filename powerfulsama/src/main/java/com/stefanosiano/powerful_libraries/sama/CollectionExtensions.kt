package com.stefanosiano.powerful_libraries.sama

import android.util.SparseArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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

/**
 * Run a function for each element and wait for the completion of all of them, using coroutines.
 * Note: It can block all threads, so be careful when using it.
 */
inline fun <T> Iterable<T>.runAndWait(crossinline run: suspend (x: T) -> Unit) = runBlocking {
    map {
        async { run(it) }
    }.map {
        it.await()
    }
}

/** Run a function for each element, using coroutines. */
inline fun <T> Iterable<T>.launch(coroutineScope: CoroutineScope, crossinline run: suspend (x: T) -> Unit) =
    coroutineScope.launch {
        map {
            async { run(it) }
        }
    }

/**
 * Returns a list containing only elements matching the given [predicate].
 * Use [runAndWait] internally.
 */
suspend fun <T> Iterable<T>.filterK(predicate: suspend (T) -> Boolean): List<T> {
    val filtered = ArrayList<T>()
    this.runAndWait {
        if (predicate(it)) {
            filtered.add(it)
        }
    }
    return filtered
}

/**
 * Returns a list containing the results of applying the given [transform] to each element in the original collection.
 * Use [runAndWait] internally.
 */
suspend fun <T, R> Iterable<T>.mapK(transform: suspend (T) -> R): List<R> {
    val destination = ArrayList<R>()
    this.runAndWait {
        destination.add(transform(it))
    }
    return destination
}

/**
 * Returns a list containing the non-null results of applying the given [transform] to each element in the original
 *  collection.
 * Use [runAndWait] internally.
 */
suspend fun <T, R> Iterable<T>.mapKNotNull(transform: suspend (T) -> R?): List<R> {
    val destination = ArrayList<R>()
    this.runAndWait {
        transform(it)?.let { t -> destination.add(t) }
    }
    return destination
}

/**
 * Groups elements of the original collection by the key returned by the given [keySelector] applied to each element
 *  and puts to a map each group key associated with a list of corresponding elements.
 * Use [runAndWait] internally.
 */
suspend fun <T, K> Iterable<T>.groupByK(keySelector: suspend (T) -> K): MutableMap<in K, MutableList<T>> {
    val destination = LinkedHashMap<K, MutableList<T>>()
    runAndWait {
        val key = keySelector(it)
        val list = destination.getOrPut(key) { ArrayList<T>() }
        list.add(it)
    }
    return destination
}

/** For loop that uses iterator item (useful to modify elements in a list without concurrentModification exceptions). */
inline fun <T : Iterable<S>, S> T.iterate(f: (S) -> Unit): T {
    val i = iterator()
    while (i.hasNext()) {
        f(i.next())
    }
    return this
}

/** For loop that uses iterator item (useful to modify elements in a list without concurrentModification exceptions). */
inline fun <T : Iterable<S>, S> T.iterateIndexed(f: (S, index: Int) -> Unit): T {
    var index = 0
    iterate {
        f(it, index)
        index++
    }
    return this
}
