package com.stefanosiano.powerful_libraries.sama.view

import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.LongSparseArray
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.AsyncPagedListDiffer
import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.PagedList
import androidx.paging.PagingData
import androidx.recyclerview.widget.*
import com.stefanosiano.powerful_libraries.sama.*
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Class that implements RecyclerViewAdapter in an easy and powerful way!
 *
 * @param itemLayoutId Id of the layout of each row
 * @param itemBindingId Id of the dataBinding variable in the row layout
 * @param hasStableId Whether the adapter has stableIds (The items of the adapter must implement getStableId() or getStableIdString(), or it won't have any effect)
 */
open class SamaRvAdapter(
    private var itemLayoutId: Int,
    private val itemBindingId: Int,
    private val hasStableId: Boolean

): RecyclerView.Adapter<SamaRvAdapter.SimpleViewHolder>(), CoroutineScope {

    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    private val mDiffer: AsyncPagingDataDiffer<SamaListItem> = AsyncPagingDataDiffer(object : DiffUtil.ItemCallback<SamaListItem>() {
        override fun areItemsTheSame(old: SamaListItem, new: SamaListItem) = getItemStableId(old) == getItemStableId(new)
        override fun areContentsTheSame(old: SamaListItem, new: SamaListItem) = old.contentEquals(new)
    }, AdapterListUpdateCallback(this))

    private var isPaged = false
    private var isObservableList = false

    private val itemLayoutIds = SparseIntArray()

    /** Objects passed to items during onBind */
    private val passedObjects = HashMap<String, Any>()

    /** callback called when the item list changes */
    private val onListChangedCallback: WeakReferenceOnListChangedCallback = WeakReferenceOnListChangedCallback(this)

    /** items shown bound to rows (implemented through ObservableArrayList) */
    private var items: ObservableList<SamaListItem> = ObservableArrayList()

    /** items implemented through live data */
    private var liveDataItems: LiveData<out List<SamaListItem>>? = null

    /** items implemented through live data */
    private var liveDataPagedItems: LiveData<out PagingData<SamaListItem>>? = null

    /** Whether liveData observers have been stopped by [stopLiveDataObservers] */
    private var liveDataObserversStopped = false

    /** Map that link string ids to unique long numbers, to use as stableId */
    private val idsMap: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

    /** Map that link string ids to unique long numbers, to use as stableId */
    private val maxId = AtomicLong(0)

    /** Function to be called when the liveData changes. It will reload the list */
    private val liveDataObserver = Observer<List<SamaListItem>> { if(it != null) bindItems(it) }

    /** Function to be called when the liveData changes. It will reload the list */
    private val pagedLiveDataObserver = Observer<PagingData<SamaListItem>> { if(it != null) bindPagingItems(it) }

    /** Reference to the recyclerView. Will be used to post runnables to the UIthread */
    var recyclerView: WeakReference<RecyclerView>? = null
        private set

    internal var recyclerViewColumnCount = 1

    @Deprecated("Use itemUpdatedListeners")
    /** Listener passed to items to provide a callback to the adapter's caller */
    private var itemUpdatedListenersOld : MutableList<suspend (SamaListItem.SamaListItemAction?, SamaListItem, Any?) -> Unit> = ArrayList()

    /** Listener passed to items to provide a callback to the adapter's caller */
    private var itemUpdatedListeners : MutableList<(SamaListItem.SamaListItemAction) -> Unit> = ArrayList()

    /** Function called when adapter starts loading items (one of [bindItems] or [bindPagedItems] is called) */
    private var onLoadStarted : (() -> Unit)? = null

    /** Function called when adapter finishes loading items (one of [bindItems] or [bindPagedItems] finished its job) */
    private var onLoadFinished : (() -> Unit)? = null

    /** List of items to be bound to [SamaMutableListItem] */
    private var mutableBoundItems = LongSparseArray<Any>()

    /** Last item detached: if the latest item detached is the one being recycled, then the item needs to be restarted */
    private var lastDetached = -1

    /** Last item detached: if the latest item detached is the one being recycled, then the item needs to be restarted */
    private val spannedSizes = SparseIntArray()

    /** Whether the adapter has just been restarted through [restartLiveDataObservers] */
    private var justRestarted = false

    /**
     * Class that implements RecyclerViewAdapter in an easy and powerful way!
     * [hasStableId] defaults to true
     *
     * @param itemLayoutId Id of the layout of each row
     * @param itemBindingId Id of the dataBinding variable in the row layout
     */
    constructor(itemLayoutId: Int, itemBindingId: Int): this(itemLayoutId, itemBindingId, true)

    init {
        setHasStableIds(hasStableId)
        itemLayoutIds.put(-1, itemLayoutId)
    }


    /** Set a different [layoutId] for different [viewType] (viewTypes should be used in [SamaListItem.getViewType] of the items) */
    fun addLayoutType(viewType: Int, layoutId: Int): SamaRvAdapter { itemLayoutIds.put(viewType, layoutId); return this }

    /** Changes the layout of the rows and reload the list */
    fun setItemLayoutId(itemLayoutId: Int) {
        this.itemLayoutId = itemLayoutId
        launch(Dispatchers.Main) { notifyDataSetChanged() }
    }


    override fun getItemCount():Int = if(isPaged) mDiffer.itemCount else items.size

    override fun getItemViewType(position: Int): Int = getItem(position)?.getViewType() ?: -1

    fun getItemSpanSize(position: Int, columns: Int): Int = getItem(position)?.getItemSpanSize(columns) ?: -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        val layoutId = if(itemLayoutIds.get(viewType) != 0) itemLayoutIds.get(viewType) else itemLayoutId
        val view: View = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return SimpleViewHolder(view)
    }

    /** Function that binds the item to the view holder, calling appropriate methods in the right order */
    override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.binding.get()?.root?.let { item.root = WeakReference(it) }
        item.binding = holder.binding
        holder.binding.get()?.setVariable(itemBindingId, item)

        item.adapterPosition = holder.bindingAdapterPosition
        item.adapterSpannedPosition = if(recyclerViewColumnCount == 1 || holder.bindingAdapterPosition == 0) holder.bindingAdapterPosition else
            (getItem(holder.bindingAdapterPosition-1)?.adapterSpannedPosition?.plus(spannedSizes.get(holder.bindingAdapterPosition-1, 1))) ?: holder.bindingAdapterPosition
        item.adapterSize = itemCount
        item.adapterColumnCount = recyclerViewColumnCount
        item.adapter = WeakReference(this)
        if(item is SamaMutableListItem<*>) {
            val bound = mutableBoundItems.get(getItemStableId(item)) ?: item.newBoundItem().also { mutableBoundItems.put(getItemStableId(item), it) }

            item.mEditBoundItem = { mutableBoundItems.put(getItemStableId(item), it) }
            item.bind(bound, passedObjects)
        }
        else
            item.onBind(passedObjects)

        item.setPostActionListener { action, item2, data -> itemUpdatedListenersOld.forEach { it.invoke(action, item2, data) } }
        item.setSendActionListener { action -> itemUpdatedListeners.forEach { it.invoke(action) } }
        item.onStart()
    }



    override fun onViewDetachedFromWindow(holder: SimpleViewHolder) {
        super.onViewDetachedFromWindow(holder)
        lastDetached = holder.bindingAdapterPosition
        val item = getItem(holder.bindingAdapterPosition) ?: return
        item.onStop()
    }

    override fun onViewAttachedToWindow(holder: SimpleViewHolder) {
        super.onViewAttachedToWindow(holder)
        val item = getItem(holder.bindingAdapterPosition) ?: return
        item.onStart()
    }


    /** Observe the items of this [RecyclerView] passing an action, the item and optional data when they change (when [SamaListItem.sendAction] is called) */
    @Suppress("UNCHECKED_CAST")
    fun <T: SamaListItem.SamaListItemAction> onAction(f: (action: T) -> Unit) { this.itemUpdatedListeners.add(f as (SamaListItem.SamaListItemAction) -> Unit) }

    @Deprecated("Use onAction")
    /** Observe the items of this [RecyclerView] passing an action, the item and optional data when they change (when [SamaListItem.onPostAction] is called) */
    @Suppress("UNCHECKED_CAST")
    fun <T: SamaListItem> observe(f: suspend (action: SamaListItem.SamaListItemAction?, item: T, data: Any?) -> Unit) { this.itemUpdatedListenersOld.add(f as suspend (SamaListItem.SamaListItemAction?, SamaListItem, Any?) -> Unit) }

    @Deprecated("Use onAction")
    /** Observe the items of this [RecyclerView] passing an action and the item when they change (when [SamaListItem.onPostAction] is called) */
    @Suppress("UNCHECKED_CAST")
    fun <T: SamaListItem> observe(f: suspend (action: SamaListItem.SamaListItemAction?, item: T) -> Unit) { observe<T> { action, item, _ -> f(action, item) } }

    @Deprecated("Use onAction")
    /** Observe the items of this [RecyclerView] passing an action when they change (when [SamaListItem.onPostAction] is called) */
    @Suppress("UNCHECKED_CAST")
    fun <T: SamaListItem> observe(f: suspend (action: SamaListItem.SamaListItemAction?) -> Unit) { observe<T> { action, _, _ -> f(action) } }

    /**
     * Binds the items of the adapter to the passed list
     *
     * @param list ObservableList that will be bound to the adapter.
     *              When it changes, the changes will be reflected to the adapter.
     */
    @Suppress("unchecked_cast")
    @Synchronized fun bindItems(list: ObservableList<out SamaListItem>) : SamaRvAdapter {
        isPaged = false
        isObservableList = true
        onLoadStarted?.invoke()
        launch(Dispatchers.Main) {
            items.removeOnListChangedCallback(onListChangedCallback)
            items.clear()
            items = list as ObservableList<SamaListItem>
            items.addOnListChangedCallback(onListChangedCallback)
            notifyDataSetChanged()
        }
        onLoadFinished?.invoke()
        return this
    }


    /**
     * Binds the items of the adapter to the passed list
     *
     * @param list List that will be bound to the adapter. Checks differences with previous items to check what changed
     * @param forceReload Force the adapter to completely reload all of its items, calling [notifyDataSetChanged]. Use it if you know all the items will change (will be faster), otherwise leave the default (false)
     */
    @Synchronized fun bindItems(list: List<SamaListItem>) : SamaRvAdapter {
        isPaged = false
        isObservableList = false
        this.items.removeOnListChangedCallback(onListChangedCallback)
        onLoadStarted?.invoke()
        val diffResult = DiffUtil.calculateDiff(LIDiffCallback(items, list))

        launch(Dispatchers.Main) {
            recyclerView?.get()?.also {
                //I have to stop the scrolling if the new list has less items then current item list
                if(items.size > list.size) {
                    val firstPos = (it.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: items.size
                    if(firstPos > list.size)
                        it.stopScroll()
                }
            }
            items.clear()
            items = list.mapTo(ObservableArrayList(), { it })
            diffResult.dispatchUpdatesTo(this@SamaRvAdapter)
            onLoadFinished?.invoke()
            if(justRestarted) {
                notifyDataSetChanged()
                justRestarted = false
            }
        }

        return this
    }



    /**
     * Binds the items of the adapter to the passed list
     *
     * @param list LiveData of List that will be bound to the adapter. When it changes, the changes will be reflected to the adapter.
     */
    @Synchronized fun bindItems(list: LiveData<out List<SamaListItem>>?) : SamaRvAdapter {
        isPaged = false
        isObservableList = false
        //remove the observer from the optional current liveData
        launch(Dispatchers.Main) {
            liveDataPagedItems?.removeObserver(pagedLiveDataObserver)
            liveDataItems?.removeObserver(liveDataObserver)
            liveDataItems = list
            liveDataItems?.observeForever(liveDataObserver)
        }
        return this
    }

    fun stopLiveDataObservers() {
        logDebug("Stop observing liveData in adapter")
        liveDataObserversStopped = true
        launch(Dispatchers.Main) {
            if(isObservableList) items.removeOnListChangedCallback(onListChangedCallback)
            liveDataPagedItems?.removeObserver(pagedLiveDataObserver)
            liveDataItems?.removeObserver(liveDataObserver)
        }
    }

    fun restartLiveDataObservers() {
        if(!liveDataObserversStopped) return
        logDebug("Restart observing liveData in adapter")
        liveDataObserversStopped = false
        justRestarted = true
        launch(Dispatchers.Main) { when {
            isObservableList -> items.addOnListChangedCallback(onListChangedCallback)
            isPaged -> liveDataPagedItems?.observeForever(pagedLiveDataObserver)
            else -> liveDataItems?.observeForever(liveDataObserver)
        } }
    }



    /**
     * Binds the items of the adapter to the passed paged list
     *
     * @param list LiveData of List that will be bound to the adapter. When it changes, the changes will be reflected to the adapter
     */
    @Suppress("UNCHECKED_CAST")
    @Deprecated("Use bindPagingItems(PagingData)")
    @Synchronized fun bindPagedItems(list: PagedList<out SamaListItem>) : SamaRvAdapter = bindPagingItems(PagingData.from(list))

    /**
     * Binds the items of the adapter to the passed paged list
     *
     * @param list LiveData of List that will be bound to the adapter. When it changes, the changes will be reflected to the adapter
     */
    @Suppress("UNCHECKED_CAST")
    @Synchronized fun bindPagingItems(list: PagingData<out SamaListItem>) : SamaRvAdapter {

        isPaged = true
        isObservableList = false
        this.items.removeOnListChangedCallback(onListChangedCallback)
        onLoadStarted?.invoke()
        launch(Dispatchers.Main) {
            items.clear()
            mDiffer.submitData(list as PagingData<SamaListItem>)
            recyclerView?.get()?.also {
                //I have to stop the scrolling if the new list has less items then current item list
                if(itemCount > mDiffer.itemCount) {
                    val firstPos = (it.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: items.size
                    if(firstPos > mDiffer.itemCount)
                        it.stopScroll()
                }
            }

            items = mDiffer.snapshot().items.mapTo(ObservableArrayList()) { it }
            onLoadFinished?.invoke()
            if(justRestarted) {
                notifyDataSetChanged()
                justRestarted = false
            }

        }

        return this
    }





    /**
     * Binds the items of the adapter to the passed list
     *
     * @param list LiveData of List that will be bound to the adapter. When it changes, the changes will be reflected to the adapter.
     */
    @Suppress("UNCHECKED_CAST")
    @Deprecated("Use bindPagingItems(LiveData<PagingData>)")
    @Synchronized fun bindPagedItems(list: LiveData<out PagedList<out SamaListItem>>?) : SamaRvAdapter =
        bindPagingItems(list?.transform { PagingData.from(it) })

    /**
     * Binds the items of the adapter to the passed list
     *
     * @param list LiveData of List that will be bound to the adapter. When it changes, the changes will be reflected to the adapter.
     */
    @Suppress("UNCHECKED_CAST")
    @Synchronized fun bindPagingItems(list: LiveData<out PagingData<out SamaListItem>>?) : SamaRvAdapter {
        isPaged = true
        isObservableList = false
        //remove the observer from the optional current liveData
        launch(Dispatchers.Main) {
            liveDataItems?.removeObserver(liveDataObserver)
            liveDataPagedItems?.removeObserver(pagedLiveDataObserver)
            liveDataPagedItems = list as LiveData<PagingData<SamaListItem>>?
            liveDataPagedItems?.observeForever(pagedLiveDataObserver)
        }
        return this
    }

    /** Pass the [ob] to all the items in the list, using [key] */
    fun passToItems(key: String, ob: Any) : SamaRvAdapter {
        passedObjects[key] = ob
        return this
    }

    /** Function called when adapter starts loading items (one of [bindItems] or [bindPagedItems] is called) */
    fun onLoadStarted (f: () -> Unit) : SamaRvAdapter { this.onLoadStarted = f; return this }

    /** Function called when adapter finishes loading items (one of [bindItems] or [bindPagedItems] finished its job) */
    fun onLoadFinished (f: () -> Unit) : SamaRvAdapter { this.onLoadFinished = f; return this }

    private fun getItemStableId(listItem: SamaListItem?): Long {
        return if(listItem?.getStableId() != RecyclerView.NO_ID)
            listItem?.getStableId() ?: RecyclerView.NO_ID
        else {
            val id = idsMap[listItem.getStableIdString()] ?: RecyclerView.NO_ID
            if(id == RecyclerView.NO_ID) idsMap[listItem.getStableIdString()] = maxId.incrementAndGet()
            idsMap[listItem.getStableIdString()] ?: RecyclerView.NO_ID
        }
    }

    /** Clear liveData observer (if any) */
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView?.clear()
        this.recyclerView = null

        //remove the observer from the optional current liveData
        launch(Dispatchers.Main) {
            liveDataItems?.removeObserver(liveDataObserver)
            liveDataPagedItems?.removeObserver(pagedLiveDataObserver)
        }
        items.forEach { it.onStop() }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView?.clear()
        this.recyclerView = WeakReference(recyclerView)

        //remove the observer from the optional current liveData
        items.forEach { it.onStart() }
        launch(Dispatchers.Main) {
            liveDataItems?.observeForever(liveDataObserver)
            liveDataPagedItems?.observeForever(pagedLiveDataObserver)
        }
    }

    /** Clears all data from the adapter (call it only if you know the adapter is not needed anymore!) */
    fun clear() {
        //remove the observer from the optional current liveData
        launch(Dispatchers.Main) {
            liveDataItems?.removeObserver(liveDataObserver)
            liveDataPagedItems?.removeObserver(pagedLiveDataObserver)
        }

        itemUpdatedListenersOld.clear()
        items.forEach { it.onDestroy() }
    }

    override fun onViewRecycled(holder: SimpleViewHolder) {
        super.onViewRecycled(holder)
        if(holder.bindingAdapterPosition == lastDetached)
            tryOrNull { items[holder.bindingAdapterPosition] }?.onStart()
    }


    /** Returns the list of bound items to [SamaMutableListItem], if adapter hasStableId */
    @Suppress("UNCHECKED_CAST")
    fun <T> getBoundItems(): List<T> {
        val size = mutableBoundItems.size()

        val boundItems = ArrayList<T?>(size)
        for(i in 0..size)
            boundItems.add(mutableBoundItems.valueAt(i) as T?)

        return boundItems.filterNotNull()
    }

    /** Clears all bound items from the adapter. If [reloadList] is set, [notifyDataSetChanged] is called */
    fun clearBoundItems(reloadList: Boolean = true) {
        mutableBoundItems.clear()
        if(reloadList) launch(Dispatchers.Main) { notifyDataSetChanged() }
    }

    /** Returns the stableId of the item at position [position], if available and if adapter hasStableId. */
    override fun getItemId(position: Int): Long = if(hasStableId && getItem(position) != null) getItemStableId(getItem(position)!!) else RecyclerView.NO_ID

    /** Returns all the items in the adapter */
    fun getItems(): List<SamaListItem> = this.items


    fun getItemPosition(id: String): Int = (0 until itemCount).firstOrNull { getItem(it)?.getStableIdString() == id } ?: -1

    fun getItemPosition(id: Long): Int = (0 until itemCount).firstOrNull { getItem(it)?.getStableId() == id } ?: -1

    fun getItemPosition(item: SamaListItem): Int {
        val stableId = getItemStableId(item)
        return (0 until itemCount).firstOrNull { i -> getItem(i)?.let { getItemStableId(it) } == stableId } ?: -1
    }


    /** Returns the currently shown PagedList, but not necessarily the most recent passed via
     *  [bindPagedItems], because a diff is computed asynchronously before updating the currentList value.
     * May be null if no PagedList is being presented or adapter is not using a paged list */
    @Suppress("UNCHECKED_CAST")
    @Deprecated("It will always return null now")
    fun getPagedItems(): PagedList<SamaListItem>? =  null

    /** Returns the item at position [position]. If the items are from a paged list the item is returned only if it was already loaded */
    @Deprecated("Use getItem(Int)")
    private fun getItemOrNull(position: Int): SamaListItem? = tryOrNull { if(isPaged) mDiffer.getItem(position) ?: items[position] else items[position] }

    /** Returns the item at position [position] */
    fun getItem(position: Int): SamaListItem? = tryOrNull { if(isPaged) mDiffer.getItem(position) else items[position] }



    internal fun setSpannedSize(position: Int, size: Int) = spannedSizes.put(position, size)





    //list observer stuff
    /** Function to be called when some items change */
    private fun itemRangeChanged(positionStart: Int, itemCount: Int) =
        launch(Dispatchers.Main) { notifyItemRangeChanged(positionStart, itemCount) }

    /** Function to be called when some items are added */
    private fun itemRangeInserted(positionStart: Int, itemCount: Int) =
        launch(Dispatchers.Main) { notifyItemRangeInserted(positionStart, itemCount) }

    /** Function to be called when some items are removed */
    private fun itemRangeRemoved(positionStart: Int, itemCount: Int) =
        launch(Dispatchers.Main) { notifyItemRangeRemoved(positionStart, itemCount) }

    /** Function to be called when some items are moved */
    private fun itemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        itemRangeRemoved(fromPosition, itemCount)
        itemRangeInserted(toPosition, itemCount)
    }



    /** Class that implement the ViewHolder of the Adapter */
    class SimpleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding: WeakReference<ViewDataBinding> = WeakReference(DataBindingUtil.bind(view))
        override fun toString(): String = "$bindingAdapterPosition: " + super.toString()
    }

    /** Class that listens for changes of the passed list and calls the methods of the adapter */
    inner class WeakReferenceOnListChangedCallback( bindingRvAdapter: SamaRvAdapter): ObservableList.OnListChangedCallback<ObservableList<SamaListItem>>() {

        private val adapterReference = WeakReference(bindingRvAdapter)

        @Synchronized override fun onChanged(sender: ObservableList<SamaListItem>?) { launch(Dispatchers.Main) { adapterReference.get()?.notifyDataSetChanged() } }
        @Synchronized override fun onItemRangeRemoved(sender: ObservableList<SamaListItem>?, positionStart: Int, itemCount: Int) { launch(Dispatchers.Main) { adapterReference.get()?.itemRangeRemoved(positionStart, itemCount) } }
        @Synchronized override fun onItemRangeMoved(sender: ObservableList<SamaListItem>?, fromPosition: Int, toPosition: Int, itemCount: Int) { launch(Dispatchers.Main) { adapterReference.get()?.itemRangeMoved(fromPosition, toPosition, itemCount) } }
        @Synchronized override fun onItemRangeInserted(sender: ObservableList<SamaListItem>?, positionStart: Int, itemCount: Int) { launch(Dispatchers.Main) { adapterReference.get()?.itemRangeInserted(positionStart, itemCount) } }
        @Synchronized override fun onItemRangeChanged(sender: ObservableList<SamaListItem>?, positionStart: Int, itemCount: Int) { launch(Dispatchers.Main) { adapterReference.get()?.itemRangeChanged(positionStart, itemCount) } }
    }



    inner class LIDiffCallback(private val oldList: List<SamaListItem>, private val newList: List<SamaListItem>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = if(hasStableId) getItemStableId(oldList[oldItemPosition]) == getItemStableId(newList[newItemPosition]) else true
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            newList.size > newItemPosition
                    && oldList.size > oldItemPosition
                    && oldList[oldItemPosition].contentEquals(newList[newItemPosition])
    }
}