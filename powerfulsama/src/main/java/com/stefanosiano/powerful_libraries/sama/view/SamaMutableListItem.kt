package com.stefanosiano.powerful_libraries.sama.view


abstract class SamaMutableListItem<T> : SamaListItem() {
    final override fun onBind(initObjects: Map<String, Any>) = super.onBind(initObjects)
    final override suspend fun onBindInBackground(initObjects: Map<String, Any>) = super.onBindInBackground(initObjects)


    /** Called when it's bound to the view */
    abstract fun newBoundItem(): T

    /** Called when it's bound to the view */
    open fun onBind(bound: T, initObjects: Map<String, Any>) {  }

    /** Called when it's bound to the view */
    @Suppress("UNCHECKED_CAST")
    internal fun bind(bound: Any, initObjects: Map<String, Any>) = onBind(bound as T, initObjects)

    /** Called when it's bound to the view, in background after [onBind] */
    @Suppress("UNCHECKED_CAST")
    internal suspend fun bindInBackground(bound: Any, initObjects: Map<String, Any>) = onBindInBackground(bound as T, initObjects)

    /** Called when it's bound to the view, in background after [onBind] */
    open suspend fun onBindInBackground(bound: T, initObjects: Map<String, Any>) {}

}