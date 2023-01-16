package com.stefanosiano.powerful_libraries.sama.view

import androidx.room.Ignore
import kotlinx.coroutines.launch

/**
 * Class to be extended by other list items. To be used with [SamaRvAdapter].
 * It contains a dynamic object that is retained in memory by the adapter, passed in [onBind].
 * In case no dynamic objects are needed, use [SamaListItem].
 */
abstract class SamaMutableListItem<T : Any> : SamaListItem() {

    /** Function to edit the object bound to this item. */
    @Ignore internal var mEditBoundItem: (T) -> Unit = {}

    final override suspend fun onBind() = super.onBind()

    /** Called the first time the bound item needs to be created. */
    abstract fun newBoundItem(): T

    /** Called when it's bound to the view. */
    open suspend fun onBind(bound: T) { }

    /** Edit the item bound to this list item. */
    protected fun editBoundItem(item: T) { mEditBoundItem(item) }

    /** Called to bind the item to the view. */
    @Suppress("UNCHECKED_CAST")
    internal fun bind(bound: Any, initObjects: Map<String, Any>) {
        this.passedObjects = initObjects
        launchableFunctions.clear()
        launch { onBind(bound as T) }
    }
}
