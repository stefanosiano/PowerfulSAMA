package com.stefanosiano.powerful_libraries.sama.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

@Suppress("UnusedPrivateClass")
private class LiveDataExtensions

/** Transforms the liveData using the function [onValue] every time it changes, returning another liveData. */
fun <T, D> LiveData<T>.transform(onValue: (t: T) -> D): LiveData<D> {
    val transformedLiveData = MediatorLiveData<D>()
    transformedLiveData.addSource(this) {
        transformedLiveData.postValue(onValue(it))
    }
    return transformedLiveData
}

/** Returns a liveData which returns values only when they change. */
fun <T> LiveData<T>.getDistinct(): LiveData<T> = getDistinctBy { it as Any }

/** Returns a liveData which returns values only when they change. */
fun <T> LiveData<T>.getDistinctBy(function: (T) -> Any): LiveData<T> {
    val distinctLiveData = MediatorLiveData<T>()

    distinctLiveData.addSource(this, object : Observer<T> {
        private var lastObj: T? = null

        override fun onChanged(obj: T?) {
            if (lastObj != null && obj != null && function(lastObj!!) == function(obj)) return

            lastObj = obj
            distinctLiveData.postValue(lastObj)
        }
    })
    return distinctLiveData
}

/** Returns a liveData which returns values only when they change. */
fun <T> LiveData<List<T>>.getListDistinct(): LiveData<List<T>> = this.getListDistinctBy { it as Any }

/** Returns a liveData which returns values only when they change. */
fun <T> LiveData<List<T>>.getListDistinctBy(function: (T) -> Any): LiveData<List<T>> {
    val distinctLiveData = MediatorLiveData<List<T>>()

    distinctLiveData.addSource(this, object : Observer<List<T>> {
        private var lastObj: List<T>? = null

        override fun onChanged(obj: List<T>?) {
            if (lastObj != null &&
                obj?.size == lastObj?.size &&
                compareListsContent(obj ?: ArrayList(), lastObj ?: ArrayList(), function)
            ) return

            lastObj = obj
            distinctLiveData.postValue(lastObj)
        }

        private inline fun compareListsContent(list1: List<T>, list2: List<T>, compare: (T) -> Any): Boolean =
            list1.indices.all { compare( list1[it] ) == compare( list2[it] ) }
    })
    return distinctLiveData
}
