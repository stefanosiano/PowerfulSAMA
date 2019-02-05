package com.stefanosiano.powerful_libraries.sama.view

import android.util.SparseArray
import androidx.recyclerview.widget.RecyclerView

abstract class SamaListItem {

    /** Returns the unique id of the item (defaults to [RecyclerView.NO_ID]). Overrides [getStableIdString] */
    open fun getStableId(): Long = RecyclerView.NO_ID

    /** Returns the unique id (as a string) of the item (defaults to an empty string). It's converted to a long in the adapter. Is overridden by [getStableId] */
    open fun getStableIdString(): String = ""

    /** returns the viewType of the item. Defaults to -1 */
    open fun getViewType() = -1

    /** Function called to save the item variables. You can save any variable with an integer id, using saveItems.put(1, lockedState) */
    open fun onSaveItems(saveItems: SparseArray<Any>): SparseArray<Any> = saveItems

    /** Function called when the item is bound to the view */
    internal fun bind(initObjects: Map<String, Any>) = onBind(initObjects)

    /** Function called when the item is bound to the view */
    open fun onBind(initObjects: Map<String, Any>) {  }

    /** Function called when the item is bound to the view */
    internal suspend fun bindInBackground(initObjects: Map<String, Any>) = onBindInBackground(initObjects)

    /** Function called when the item is bound to the view. Called in background after [onBind]. It gets blocked if the adapter is destroyed (e.g. activity finished) */
    open suspend fun onBindInBackground(initObjects: Map<String, Any>) {}

    /** Function called when the item variables should be restored. Called after [onBind] and [onBindInBackground]. Works only id [getStableId] is overridden and the adapter's hasStableId is true! */
    internal fun reload(savedObjects: SparseArray<Any>) = onReload(savedObjects)

    /** Function called when the item variables should be restored. Called after [onBind] and [onBindInBackground]. Works only id [getStableId] is overridden and the adapter's hasStableId is true! */
    open fun onReload(savedObjects: SparseArray<Any>) {}

    /** Function called in background when the item variables should be restored, after [onReload]. It gets blocked if the adapter is destroyed (e.g. activity finished). Works only id [getStableId] is overridden and the adapter's hasStableId is true! */
    internal suspend fun reloadInBackground(savedObjects: SparseArray<Any>) = onReloadInBackground(savedObjects)

    /** Function called in background when the item variables should be restored, after [onReload]. It gets blocked if the adapter is destroyed (e.g. activity finished). Works only id [getStableId] is overridden and the adapter's hasStableId is true! */
    open suspend fun onReloadInBackground(savedObjects: SparseArray<Any>) {}

    /** Function called when the adapter is removed from the parent or when notifyDatasetChanged() is called on adapter */
    internal fun stop() = onStop()

    /** Function called when the adapter is removed from the parent or when notifyDatasetChanged() is called on adapter */
    open fun onStop() {}

    /** Compares this to another item to decide if they are the same when the list is reloaded. By default it calls == */
    open fun contentEquals(other: SamaListItem) = this == other

}