package com.stefanosiano.powerful_libraries.sama.view

import kotlinx.coroutines.launch


abstract class SamaMutableListItem<T> : SamaListItem() {

    @Deprecated(message = "Use method with bound object, since this is a mutableListItem", replaceWith = ReplaceWith("onBind(bound, initObjects)"))
    final override fun onBind(initObjects: Map<String, Any>) = super.onBind(initObjects)
    final override suspend fun onBindInBackground(initObjects: Map<String, Any>) = super.onBindInBackground(initObjects)


    /** Called when it's bound to the view */
    abstract fun newBoundItem(): T

    /** Called when it's bound to the view */
    open fun onBind(bound: T, initObjects: Map<String, Any>) { launch { onBindInBackground(bound, initObjects) } }

    /** Called when it's bound to the view */
    @Suppress("UNCHECKED_CAST")
    internal fun bind(bound: Any, initObjects: Map<String, Any>) = onBind(bound as T, initObjects)

    /** Called when it's bound to the view, in background after [onBind] */
    protected open suspend fun onBindInBackground(bound: T, initObjects: Map<String, Any>) {}

}