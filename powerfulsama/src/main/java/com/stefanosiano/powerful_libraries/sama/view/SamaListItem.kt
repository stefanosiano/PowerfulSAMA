package com.stefanosiano.powerful_libraries.sama.view

import android.util.SparseArray
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Ignore
import com.stefanosiano.powerful_libraries.sama.extensions.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.extensions.forEach
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserver
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserverImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Class that represents a list item. Used in conjunction with [SamaRvAdapter]. */
@Suppress("TooManyFunctions")
abstract class SamaListItem : CoroutineScope, SamaObserver by SamaObserverImpl() {
    @Ignore
    private val coroutineJob: Job = SupervisorJob()
    @Ignore
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    @Deprecated("Use sendAction")
    @Ignore
    internal var onPostAction: (suspend (SamaListItemAction?, SamaListItem, Any?) -> Unit)? = null

    @Ignore
    internal var onSendAction: ((SamaListItemAction) -> Unit)? = null

    /** Delay in milliseconds after which a function in "observe(ob, ob, ob...)" can be called again.
     * Used to avoid calling the same method multiple times due to observing multiple variables. */
    @Ignore
    protected var multiObservableDelay: Long = 100L

    /** Root View this item is bound to. Use it only in [onBind] method. */
    @Ignore
    lateinit var root: WeakReference<View>
        internal set

    /** Root View this item is bound to. Use it only in [onBind] method. */
    @Ignore
    lateinit var binding: WeakReference<ViewDataBinding>
        internal set

    @Ignore
    internal var updateJobs = HashMap<String, Job>()

    /**
     * Current position given by the [SamaRvAdapter] (0 at beginning). Use it only in [onBind].
     * It's not reliable out of these methods! To get the item position call [SamaRvAdapter.getItemPosition].
     */
    @Ignore
    var adapterPosition: Int = 0
        internal set

    /**
     * Current position given by the [SamaRvAdapter] (0 at beginning). Use it only in [onBind].
     * It's not reliable out of this method! It takes into account the spanned size passed through [getItemSpanSize].
     */
    @Ignore
    var adapterSpannedPosition: Int = 0
        internal set

    /**
     * Current adapter size given by the [SamaRvAdapter] (0 at beginning).
     * It may be inaccurate on item list reload. Surely set in [onBind].
     */
    @Ignore
    var adapterSize: Int = 0
        internal set

    /**
     * Adapter this item is attached to (null at beginning). Surely set in [onBind].
     * Do not store references to real value: leak memory danger!.
     */
    @Ignore
    var adapter: WeakReference<SamaRvAdapter>? = null
        internal set

    /** Column count of the adapter's recyclerView. Works only when using [SamaRecyclerView]. Surely set in [onBind]. */
    @Ignore
    var adapterColumnCount = 1
        internal set

    /** Map of objects passed from recyclerView's adapter. Surely set in [onBind]. */
    @Ignore
    internal var passedObjects: Map<String, Any>? = null

    /** Whether the item is started or paused. */
    @Ignore
    private var isStarted = AtomicBoolean(false)

    /** Functions to call after a delay (to avoid doing too many things when users scroll too fast). */
    @Ignore
    internal val launchableFunctions = SparseArray<LaunchableFunction>()

    /** Ids used by [launchableFunctions]. */
    @Ignore
    private val launchableFunctionsUid = AtomicInteger(0)

    init {
        initObserver(this)
    }

    /** Get the adapter this item is in (may be null). */
    fun getAdapter() = adapter?.get()

    /** Get the recyclerview of the adapter this item is in (may be null). */
    fun getRecyclerView() = adapter?.get()?.recyclerView?.get()


    @Deprecated("Use sendAction")
    /**
     * Calls the listener set to the [SamaRvAdapter] through [SamaRvAdapter.observe] after [millis] milliseconds,
     *  optionally passing an [action].
     * If called again before [millis] milliseconds are passed, previous call is cancelled.
     */
    protected fun <T> postAction(
        action: T? = null,
        millis: Long = 0,
        data: Any? = null
    ) where T : SamaListItemAction, T : Enum<T> {
        updateJobs[action?.name ?: ""]?.cancel()
        updateJobs[action?.name ?: ""] =
            launch { delay(millis); if (isActive) onPostAction?.invoke(action, this@SamaListItem, data) }
    }


    /** Calls the listener set to the [SamaRvAdapter] through [SamaRvAdapter.observe] passing an [action]. */
    protected fun <T : SamaListItemAction> sendAction(action: T) {
        onSendAction?.invoke(action)
    }

    @Deprecated("Use sendAction")
    /** Sets a listener through [SamaRvAdapter] to be called by the item. */
    internal fun setPostActionListener(f: suspend (SamaListItemAction?, SamaListItem, Any?) -> Unit) {
        onPostAction = f
    }

    /** Sets a listener through [SamaRvAdapter] to be called by the item. */
    internal fun setSendActionListener(f: (SamaListItemAction) -> Unit) {
        onSendAction = f
    }

    /**
     * Returns the unique id of the item (defaults to [RecyclerView.NO_ID]).
     * Overrides [getStableIdString] if specified.
     */
    open fun getStableId(): Long = RecyclerView.NO_ID

    /**
     * Returns the unique id (as a string) of the item (defaults to an empty string).
     * It's converted to a long in the adapter. Is overridden by [getStableId] if specified.
     */
    open fun getStableIdString(): String = ""

    /** Returns the viewType of the item. Use it to provide different layouDefaults to -1. */
    open fun getViewType() = -1

    /**
     * Returns the span size requested by the item. Can use only with [SamaRecyclerView] using more than 1 column.
     * Span previous item to full row if this span is too long. Passes the total column count to simplify management.
     */
    open fun getItemSpanSize(columns: Int) = 1

    /** Called when it's bound to the view. */
    internal fun onBind(passedObjects: Map<String, Any>) {
        this.passedObjects = passedObjects
        synchronized(launchableFunctions) { launchableFunctions.clear() }
        launch { onBind() }
    }

    @Suppress("UNCHECKED_CAST")
    /** Get an item passed from the adapter from its key. Safe to call in [onBind]. */
    protected fun <T> getPassed(key: String): T? = passedObjects?.get(key) as? T

    /**
     * Calls a function through [launch] after [millis]. Useful to avoid calculations when user scrolls too fast.
     * It gets automatically called in [onStart] if not already executed and if [onBind] is not called.
     */
    fun launchAfter(millis: Long, f: suspend () -> Unit) {
        val lf = LaunchableFunction(millis, f)
        synchronized(launchableFunctions) { launchableFunctions.put(lf.id, lf) }
        launch {
            delay(millis)
            if (isStarted.get()) {
                f()
                synchronized(launchableFunctions) { launchableFunctions.remove(lf.id) }
            }
        }
    }

    /** Called when it's bound to the view, in background after [onBind]. */
    protected open suspend fun onBind() {}

    /**
     * Compares this to another item to decide if they are the same when the list is reloaded.
     * By default it calls [equals].
     */
    open fun contentEquals(other: SamaListItem) = this == other

    /**
     * Called when the view or the adapter is reattached to the recyclerview after being detached.
     * Use it if you need to reuse resources freed in [onStop].
     * By default restart all [observe] methods.
     */
    open fun onStart() {
        if (isStarted.getAndSet(true)) return
        startObserver()
        synchronized(launchableFunctions) {
            launchableFunctions.forEach { lf ->
                launch {
                    delay(lf.millis)
                    if (isStarted.get()) {
                        lf.f()
                        synchronized(launchableFunctions) { launchableFunctions.remove(lf.id) }
                    }
                }
            }
        }
    }

    /**
     * Called when the view is detached from the recyclerview or the adapter is detached.
     * Use it if you need to stop some heavy computation. By default it stops all [observe] methods.
     */
    open fun onStop() {
        if (!isStarted.getAndSet(false)) return
        stopObserver()
    }

    /**
     * Called when it's removed from the recyclerview, or its view was recycled or the recyclerView no longer observes
     *  the adapter.
     * [canBeReused] determines if the item may be reused (the view was recycled, but the item is still in the adapter).
     * Use it to completely clear any resource. Its coroutines are cancelled here.
     */
    open fun onDestroy() {
        onStop()
        destroyObserver()
        synchronized(launchableFunctions) { launchableFunctions.clear() }
        coroutineContext.cancelChildren()
    }


    /** Interface that indicates the action of the ListItem sent. */
    interface SamaListItemAction

    internal inner class LaunchableFunction(val millis: Long, val f: suspend () -> Unit) {
        /** Id of the function. */
        val id: Int = launchableFunctionsUid.incrementAndGet()
    }
}
