package com.stefanosiano.powerful_libraries.sama.view

import android.util.SparseArray
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Ignore
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import kotlinx.coroutines.*

abstract class SamaListItem : CoroutineScope {

    @Ignore private val coroutineJob: Job = SupervisorJob()
    @Ignore override val coroutineContext = coroutineSamaHandler(coroutineJob)

    @Ignore internal var onItemUpdated : (suspend (SamaListItem) -> Unit)? = null
    @Ignore internal var isLazyInit = false
    @Ignore internal var isStarted = false



    /** Sets a listener through [SamaRvAdapter] to be called by the item */
    protected fun onItemUpdated() = launch { onItemUpdated?.invoke(this@SamaListItem) }

    /** Sets a listener through [SamaRvAdapter] to be called by the item */
    internal fun onItemUpdatedListenerSet(f: suspend (SamaListItem) -> Unit) { onItemUpdated = f }

    /** Returns the unique id of the item (defaults to [RecyclerView.NO_ID]). Overrides [getStableIdString] if specified */
    open fun getStableId(): Long = RecyclerView.NO_ID

    /** Returns the unique id (as a string) of the item (defaults to an empty string). It's converted to a long in the adapter. Is overridden by [getStableId] if specified */
    open fun getStableIdString(): String = ""

    /** Returns the viewType of the item. Use it to provide different layouDefaults to -1 */
    open fun getViewType() = -1

    /** Called when it's bound to the view */
    open fun onBind(initObjects: Map<String, Any>) {  }

    /** Called when it's bound to the view, in background after [onBind] */
    open suspend fun onBindInBackground(initObjects: Map<String, Any>) {}

    /** Compares this to another item to decide if they are the same when the list is reloaded. By default it calls == */
    open fun contentEquals(other: SamaListItem) = this == other

    /** Called when it's removed from the recyclerview or the recyclerView no longer observes the adapter. Always called after [onStop]. Use it to completely clear any resource */
    open fun onDestroy() { coroutineContext.cancel() }

    /** Called when it's reattached to the recyclerview after being detached. Use it if you need to reuse resources freed in [onStop] */
    open fun onStart() { isStarted = true }

    /** Called when it's removed from the recyclerview, or its view was recycled or the recyclerView no longer observes the adapter. Use it to clear resources, keeping in mind the item may be reused later on */
    open fun onStop() { isStarted = false }

    /** Return if it was lazy initialized. Use it with [onLazyInit] */
    internal fun isLazyInitialized(): Boolean { return isLazyInit }

    /** Cancel anything the coroutine is doing. Used internally */
    internal fun cancelCoroutine() { if(isActive) coroutineContext.cancel() }

    /** Called in background only once when it should be initialized (it's about to be shown or the initialization background thread reaches it). [isLazyInitialized] is used to understand if the item was already lazy initialized */
    open suspend fun onLazyInit() { isLazyInit = true }
}