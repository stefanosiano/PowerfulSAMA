package com.stefanosiano.powerful_libraries.sama.extensions

@Suppress("UnusedPrivateClass")
private class CollectionKExtensions

/**
 * Groups elements of the original collection by the key returned by the given [keySelector] function
 * applied to each element and puts to a map each group key associated with a list of corresponding elements.
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

/** Returns a list containing only elements matching the given [predicate]. */
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
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 */
suspend fun <T, R> Iterable<T>.mapK(transform: suspend (T) -> R): List<R> {
    val destination = ArrayList<R>()
    this.runAndWait { destination.add(transform(it)) }
    return destination
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 */
suspend fun <T, R> Iterable<T>.mapKNotNull(transform: suspend (T) -> R?): List<R> {
    val destination = ArrayList<R>()
    this.runAndWait { transform(it)?.let { t -> destination.add(t) } }
    return destination
}
