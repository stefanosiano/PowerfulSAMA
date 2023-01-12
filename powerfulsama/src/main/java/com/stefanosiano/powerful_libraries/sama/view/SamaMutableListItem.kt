package com.stefanosiano.powerful_libraries.sama.view

import androidx.room.Ignore
import kotlinx.coroutines.launch

abstract class SamaMutableListItem<T : Any> : SamaListItem() {

    /** Column count of the adapter's recyclerView. Works only when using [SamaRecyclerView]. Surely set in [onBind]. */
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
        this.passedObjects = initObjects; launchableFunctions.clear(); launch {
            onBind(
                bound as T
            )
        }
    }
}
