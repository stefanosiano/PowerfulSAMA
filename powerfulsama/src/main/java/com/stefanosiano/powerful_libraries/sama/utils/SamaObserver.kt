package com.stefanosiano.powerful_libraries.sama.utils

import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableByte
import androidx.databinding.ObservableChar
import androidx.databinding.ObservableDouble
import androidx.databinding.ObservableField
import androidx.databinding.ObservableFloat
import androidx.databinding.ObservableInt
import androidx.databinding.ObservableList
import androidx.databinding.ObservableLong
import androidx.databinding.ObservableShort
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/** Interface that allows a component to observe variables and call methods when they change.
 * The main methods to call are [initObserver], [destroyObserver], [stopObserver] and [startObserver]. */
@Suppress("TooManyFunctions", "ComplexInterface", "MethodOverloading")
interface SamaObserver {
    /**
     * Initializes the observer with the current coroutine,
     * used to handle delays on multi-variables observe and to observe liveData on UI.
     */
    fun initObserver(coroutineScope: CoroutineScope)

    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R, T> observe(o: Flow<T>, vararg obs: Observable, obFun: (data: T) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableInt, vararg obs: Observable, obFun: (data: Int) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableShort, vararg obs: Observable, obFun: (data: Short) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableLong, vararg obs: Observable, obFun: (data: Long) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableByte, vararg obs: Observable, obFun: (data: Byte) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableChar, vararg obs: Observable, obFun: (data: Char) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableBoolean, vararg obs: Observable, obFun: (data: Boolean) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableFloat, vararg obs: Observable, obFun: (data: Float) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R> observe(o: ObservableDouble, vararg obs: Observable, obFun: (data: Double) -> R): ObservableField<R>


    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     * Returns an [ObservableField] with initial value of null.
     */
    fun <R, T> observe(o: ObservableField<T>, vararg obs: Observable, obFun: (data: T) -> R): ObservableField<R>

    /**
     * Observes [o] until this object is destroyed and calls [obFun] in the background,
     * now and whenever [o] or any of [obs] change, with the current value of [o].
     * Does nothing if [o] is null or already changed.
     */
    fun <T> observe(o: ObservableList<T>, vararg obs: Observable, obFun: (data: List<T>) -> Unit)

    /**
     * Observes a liveData until this object is destroyed into an observable field.
     * Does not update the observable if the value of the liveData is null.
     */
    fun <T> observeAsOf(liveData: LiveData<T>): ObservableField<T>

    /** Observes a liveData until this object is destroyed, using a custom observer. */
    fun <T> observe(liveData: LiveData<T>, vararg obs: Observable, observerFunction: (data: T) -> Unit): LiveData<T>

    /**
     * Run [f] to get a [LiveData] every time any of [o] or [obs] changes, removing the old one.
     * It return a [LiveData] of the same type as [f].
     */
    fun <T> observeAndReloadLiveData(o: ObservableField<*>, vararg obs: Observable, f: () -> LiveData<T>?): LiveData<T>

    /** Start observing the variables and call the methods. */
    fun startObserver()

    /** Stop observing the variables. */
    fun stopObserver()

    /** Clear all references to observed variables and methods, stopping and detaching them. */
    fun destroyObserver()
}
