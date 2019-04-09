package com.stefanosiano.powerful_libraries.sama.view

import android.util.SparseArray
import androidx.recyclerview.widget.RecyclerView

interface SamaListItem {

    /** Returns the unique id of the item (defaults to [RecyclerView.NO_ID]). Overrides [getStableIdString] if specified */
    open fun getStableId(): Long = RecyclerView.NO_ID

    /** Returns the unique id (as a string) of the item (defaults to an empty string). It's converted to a long in the adapter. Is overridden by [getStableId] if specified */
    open fun getStableIdString(): String = ""

    /** Returns the viewType of the item. Use it to provide different layouDefaults to -1 */
    open fun getViewType() = -1

    /** Called to save its variables. You can save any variable with an integer id, using saveItems.put(1, lockedState) */
    open fun onSaveItems(saveItems: SparseArray<Any>): SparseArray<Any> = saveItems

    /** Called when it's bound to the view */
    open fun onBind(initObjects: Map<String, Any>) {  }

    /** Called when it's bound to the view, in background after [onBind] */
    open suspend fun onBindInBackground(initObjects: Map<String, Any>) {}

    /** Called in background only once when it should be initialized (it's about to be shown or the initialization background thread reaches it). [isLazyInitialized] is used to understand if the item was already lazy initialized */
    open suspend fun onLazyInit() {}

    /** Return if it was lazy initialized. Use it with [onLazyInit] */
    open fun isLazyInitialized() = false

    /** Called when its variables should be restored. Called after [onBind] and [onBindInBackground]. Works only if [getStableId] or [getStableIdString] is overridden and the adapter's hasStableId is true! */
    open fun onReload(savedObjects: SparseArray<Any>) {}

    /** Called in background when its variables should be restored, after [onReload]. Works only if [getStableId] or [getStableIdString] is overridden and the adapter's hasStableId is true! */
    open suspend fun onReloadInBackground(savedObjects: SparseArray<Any>) {}

    /** Called when it's removed from the recyclerview, or its view was recycled or the recyclerView no longer observes the adapter. Use it to clear resources, keeping in mind the item may be reused later on */
    open fun onStop() {}

    /** Called when it's reattached to the recyclerview after being detached. Use it if you need to reuse resources freed in [onStop] */
    open fun onStart() {}

    /** Called when it's removed from the recyclerview or the recyclerView no longer observes the adapter. Always called after [onStop]. Use it to completely clear any resource */
    open fun onDestroy() {}

    /** Compares this to another item to decide if they are the same when the list is reloaded. By default it calls == */
    open fun contentEquals(other: SamaListItem) = this == other
}