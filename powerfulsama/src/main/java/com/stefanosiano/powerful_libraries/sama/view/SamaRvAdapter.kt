package com.stefanosiano.powerful_libraries.sama.view

import android.util.SparseArray
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
import androidx.paging.PagedList
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stefanosiano.powerful_libraries.sama.*
import com.stefanosiano.powerful_libraries.sama.utils.KLongSparseArray
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicLong

/**
 * Class that implements RecyclerViewAdapter in an easy and powerful way!
 *
 * @param itemLayoutId Id of the layout of each row
 * @param itemBindingId Id of the dataBinding variable in the row layout
 * @param hasStableId Whether the adapter has stableIds (The items of the adapter must implement getStableId() or getStableIdString(), or it won't have any effect)
 * @param preserveLazyInitializedItemCache Whether the adapter preserves its internal item cache for lazy initialized items. It's used only if [hasStableId] is true.
 *      Setting true will increase memory usage, but improve performance when filtering, since it won't call [lazyInit] on items removed from the adapter and then added to it again
 */
open class SamaRvAdapter(
    private var itemLayoutId: Int,
    private val itemBindingId: Int,
    private val hasStableId: Boolean,
    private val preserveLazyInitializedItemCache: Boolean

): RecyclerView.Adapter<SamaRvAdapter.SimpleViewHolder>(), CoroutineScope {

    private val mDiffer: AsyncPagedListDiffer<SamaListItem> = AsyncPagedListDiffer(this, object : DiffUtil.ItemCallback<SamaListItem>() {
        override fun areItemsTheSame(old: SamaListItem, new: SamaListItem) = getItemStableId(old) == getItemStableId(new)
        override fun areContentsTheSame(old: SamaListItem, new: SamaListItem) = old.contentEquals(new)
    })

    var isPaged = false



    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    private val itemLayoutIds = SparseIntArray()

    /** Objects passed to items during onBind */
    private val initObjects = HashMap<String, Any>()

    /** callback called when the item list changes */
    private val onListChangedCallback: WeakReferenceOnListChangedCallback = WeakReferenceOnListChangedCallback(this)

    /** items shown bound to rows (implemented through ObservableArrayList) */
    private var items: ObservableList<SamaListItem> = ObservableArrayList()

    /** items implemented through live data */
    private var liveDataItems: LiveData<out List<SamaListItem>>? = null

    /** items implemented through live data */
    private var liveDataPagedItems: LiveData<out PagedList<SamaListItem>>? = null

    /** map that saves initialized variables to avoid to reinitialize them when reloaded */
    private val lazyInitializedItemCacheMap = LongSparseArray<SamaListItem>()

    /** Map that link string ids to unique long numbers, to use as stableId */
    private val idsMap: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

    /** Map that link string ids to unique long numbers, to use as stableId */
    private val maxId = AtomicLong(0)

    /** Job used to bind the item list in the background */
    private var bindListJob: Job? = null

    /** Function to be called when the liveData changes. It will reload the list */
    private val liveDataObserver = Observer<List<SamaListItem>> { if(it != null) bindItems(it, false) }

    /** Function to be called when the liveData changes. It will reload the list */
    private val pagedLiveDataObserver = Observer<PagedList<SamaListItem>> { if(it != null) bindPagedItems(it) }

    /** Reference to the recyclerView. Will be used to post runnables to the UIthread */
    private var recyclerView: WeakReference<RecyclerView>? = null

    /** Set used to understand if the item is being initialized, to be sure to lazy init only once */
    private val lazyInitSet = ConcurrentSkipListSet<Long>()

    /** Job used to cancel and start lazy initialization */
    private var lazyInitsJob : Job? = null

    /** Listener passed to items to provide a callback to the adapter's caller */
    private var itemUpdatedListeners : MutableList<suspend (SamaListItem) -> Unit> = ArrayList()

    /** Function called when adapter starts loading items (one of [bindItems] or [bindPagedItems] is called) */
    private var onLoadStarted : (suspend () -> Unit)? = null

    /** Function called when adapter finishes loading items (one of [bindItems] or [bindPagedItems] finished its job) */
    private var onLoadFinished : (() -> Unit)? = null

    /** List of items to be bound to [SamaMutableListItem] */
    private var mutableBoundItems = LongSparseArray<Any>()

    /**
     * Class that implements RecyclerViewAdapter in an easy and powerful way!
     * [hasStableId] defaults to true
     * [preserveLazyInitializedItemCache] defaults to true
     *
     * @param itemLayoutId Id of the layout of each row
     * @param itemBindingId Id of the dataBinding variable in the row layout
     */
    constructor(itemLayoutId: Int, itemBindingId: Int): this(itemLayoutId, itemBindingId, true, true)

    init {
        setHasStableIds(hasStableId)
        itemLayoutIds.put(-1, itemLayoutId)
    }

    /** Clear the cache of the lazy initialized items (Used only if [hasStableId] is true).
     * Use it when setting [preserveLazyInitializedItemCache] as true to be sure to clear the cache */
    fun clearLazyInitializedCache() = lazyInitializedItemCacheMap.clear()


    /** Set a different [layoutId] for different [viewType] (viewTypes should be used in [SamaListItem.getViewType] of the items) */
    fun addLayoutType(viewType: Int, layoutId: Int): SamaRvAdapter { itemLayoutIds.put(viewType, layoutId); return this }

    /** Changes the layout of the rows and reload the list */
    fun setItemLayoutId(itemLayoutId: Int) {
        this.itemLayoutId = itemLayoutId
        runOnUi { dataSetChanged() }
    }


    override fun getItemCount():Int = if(isPaged) mDiffer.itemCount else items.size

    /** forces all items to call their [SamaListItem.onBind] and [SamaListItem.onBindInBackground]. Only if [hasStableId] is true */
    @Synchronized fun rebind() { runOnUi { if(hasStableId) items.iterate { bindItemToViewHolder(null, it) } } }

    override fun getItemViewType(position: Int): Int = getItem(position)?.getViewType() ?: -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        val layoutId = if(itemLayoutIds.get(viewType) != 0) itemLayoutIds.get(viewType) else itemLayoutId
        val view: View = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return SimpleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
        val item = getItem(position) ?: return
        runBlocking { bindListJob?.join() }
        val bindJob = item.launch { holder.binding.get()?.setVariable(itemBindingId, getItem(holder.adapterPosition)) }
        bindItemToViewHolder(bindJob, getItem(holder.adapterPosition))
    }

    override fun onViewDetachedFromWindow(holder: SimpleViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val item = getItem(holder.adapterPosition) ?: return
        item.onStop()
    }

    override fun onViewAttachedToWindow(holder: SimpleViewHolder) {
        super.onViewAttachedToWindow(holder)
        val item = getItem(holder.adapterPosition) ?: return
        item.onStart()
        lazyInit(item)
    }

    /** Function that binds the item to the view holder, calling appropriate methods in the right order */
    private fun bindItemToViewHolder(job: Job?, listItem: SamaListItem?){
        listItem ?: return
        runBlocking(listItem.coroutineContext) { job?.join() }
        if(listItem is SamaMutableListItem<*>) {
            val bound = mutableBoundItems.get(getItemStableId(listItem)) ?: {
                listItem.newBoundItem().also { mutableBoundItems.put(getItemStableId(listItem), it) }
            }
            listItem.bind(bound, initObjects)
        }
        else
            listItem.onBind(initObjects)

        listItem.onItemUpdatedListenerSet { item -> itemUpdatedListeners.forEach { it.invoke(item) } }
        if(!isActive) return
        runBlocking { bindListJob?.join() }
        listItem.launch {
            if(listItem is SamaMutableListItem<*>) {
                val bound = mutableBoundItems.get(getItemStableId(listItem)) ?: {
                    listItem.newBoundItem().also { mutableBoundItems.put(getItemStableId(listItem), it) }
                }
                listItem.bindInBackground(bound, initObjects)
            }
            else
                listItem.onBindInBackground(initObjects)
        }
    }


    /** Observe the items of this [RecyclerView] passing the updated item when it changes (when [SamaListItem.onItemUpdated] is called) */
    @Suppress("UNCHECKED_CAST")
    fun <T> observe(f: suspend (item: T) -> Unit) where T: SamaListItem { this.itemUpdatedListeners.add(f as suspend (SamaListItem) -> Unit) }

    /**
     * Binds the items of the adapter to the passed list
     *
     * @param list ObservableList that will be bound to the adapter.
     *              When it changes, the changes will be reflected to the adapter.
     */
    @Suppress("unchecked_cast")
    @Synchronized fun bindItems(list: ObservableList<out SamaListItem>) : SamaRvAdapter {
        isPaged = false
        lazyInitializedItemCacheMap.clear()
        bindListJob?.cancel()
        bindListJob = launch {
            onLoadStarted?.invoke()
            runOnUi {
                items.removeOnListChangedCallback(onListChangedCallback)
                items.clear()
                items = list as ObservableList<SamaListItem>
                items.addOnListChangedCallback(onListChangedCallback)
                itemRangeInserted(0, list.size)
                onLoadFinished?.invoke()
                startLazyInits()
            }
        }
        return this
    }


    /**
     * Binds the items of the adapter to the passed list
     *
     * @param list List that will be bound to the adapter. Checks differences with previous items to check what changed
     * @param forceReload Force the adapter to completely reload all of its items, calling [notifyDataSetChanged]. Use it if you know all the items will change (will be faster), otherwise leave the default (false)
     */
    @Synchronized fun bindItems(list: List<SamaListItem>, forceReload: Boolean = false) : SamaRvAdapter {
        isPaged = false
        this.items.removeOnListChangedCallback(onListChangedCallback)
        bindListJob?.cancel()
        bindListJob = launch {
            if (!isActive) return@launch
            onLoadStarted?.invoke()
            if (forceReload) {
                if (!isActive) return@launch
                runOnUi {
                    items.clear()
                    items.addAll(list)
                    dataSetChanged()
                    onLoadFinished?.invoke()
                    startLazyInits()
                }
            } else {
                val diffResult = DiffUtil.calculateDiff(LIDiffCallback(items, list))
                if (!isActive) return@launch

                recyclerView?.get()?.also {
                    //I have to stop the scrolling if the new list has less items then current item list
                    if(items.size > list.size) {
                        val firstPos = (it.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: items.size
                        if(firstPos > list.size)
                            it.stopScroll()
                    }
                }
                if (!isActive) return@launch
                runOnUi {
                    items.clear()
                    items = list.mapTo(ObservableArrayList(), {
                        val itemCached = lazyInitializedItemCacheMap.get(getItemStableId(it))
                        if(itemCached?.contentEquals(it) == true) itemCached else it
                    })
                    diffResult.dispatchUpdatesTo(this@SamaRvAdapter)
                    onLoadFinished?.invoke()
                    startLazyInits()
                }
            }
        }

        return this
    }


    @Synchronized private fun startLazyInits() {
        lazyInitsJob?.cancel()
        lazyInitsJob = launch {
            if(!isActive) return@launch
            if(!preserveLazyInitializedItemCache)
                lazyInitializedItemCacheMap.clear()
            items.filter { it.isLazyInitialized() }.forEach { lazyInitializedItemCacheMap.put(getItemStableId(it), it) }
            delay(150)
            val lazyItems = items.filter { !it.isLazyInitialized() }
            lazyItems.iterateIndexed { item, index ->
                if(!isActive) return@launch
                if(lazyInit(item))
                    delay(15+5*index.toLong().coerceAtMost(100))
            }
        }
    }


    private fun lazyInit(item: SamaListItem): Boolean {
        if(hasStableId && !item.isLazyInitialized() && !lazyInitSet.contains(getItemStableId(item))) {
            item.launch {
                if (!lazyInitSet.add(getItemStableId(item))) return@launch
                item.onLazyInit()
                lazyInitializedItemCacheMap.put(getItemStableId(item), item)
                lazyInitSet.remove(getItemStableId(item))
            }
            return true
        }
        return false
    }


    /**
     * Binds the items of the adapter to the passed list
     *
     * @param list LiveData of List that will be bound to the adapter. When it changes, the changes will be reflected to the adapter.
     */
    @Synchronized fun bindItems(list: LiveData<out List<SamaListItem>>?) : SamaRvAdapter {
        isPaged = false
        lazyInitializedItemCacheMap.clear()
        //remove the observer from the optional current liveData
        runOnUi {
            liveDataItems?.removeObserver(liveDataObserver)
            liveDataItems = list
            liveDataItems?.observeForever(liveDataObserver)
        }
        return this
    }



    /**
     * Binds the items of the adapter to the passed paged list
     *
     * @param list LiveData of List that will be bound to the adapter. When it changes, the changes will be reflected to the adapter
     */
    @Suppress("UNCHECKED_CAST")
    @Synchronized fun bindPagedItems(list: PagedList<out SamaListItem?>) : SamaRvAdapter {

        isPaged = true
        this.items.removeOnListChangedCallback(onListChangedCallback)
        bindListJob?.cancel()
        bindListJob = launch {
            if (!isActive) return@launch
            onLoadStarted?.invoke()

            recyclerView?.get()?.also {
                //I have to stop the scrolling if the new list has less items then current item list
                if(itemCount > list.size) {
                    val firstPos = (it.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: items.size
                    if(firstPos > list.size)
                        it.stopScroll()
                }
            }

            if (!isActive) return@launch
            items.clear()
            runOnUi {
                mDiffer.submitList(list as PagedList<SamaListItem>) {
                    items.addAll(list)

                    items = list.snapshot().filterNotNull().mapTo(ObservableArrayList(), {
                        val itemCached = lazyInitializedItemCacheMap.get(getItemStableId(it))
                        if(itemCached?.contentEquals(it) == true) itemCached else it
                    })
                    onLoadFinished?.invoke()
                    startLazyInits()
                }
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
    @Synchronized fun bindPagedItems(list: LiveData<out PagedList<out SamaListItem>>?) : SamaRvAdapter {
        isPaged = true
        lazyInitializedItemCacheMap.clear()
        //remove the observer from the optional current liveData
        runOnUi {
            liveDataPagedItems?.removeObserver(pagedLiveDataObserver)
            liveDataPagedItems = list as LiveData<PagedList<SamaListItem>>?
            liveDataPagedItems?.observeForever(pagedLiveDataObserver)
        }
        return this
    }

    /** Pass the [ob] to all the items in the list, using [key] */
    fun passToItems(key: String, ob: Any) : SamaRvAdapter {
        initObjects.remove(key)
        initObjects[key] = ob
        return this
    }

    /** Function called when adapter starts loading items (one of [bindItems] or [bindPagedItems] is called) */
    fun onLoadStarted (f: suspend () -> Unit) : SamaRvAdapter { this.onLoadStarted = f; return this }

    /** Function called when adapter finishes loading items (one of [bindItems] or [bindPagedItems] finished its job) */
    fun onLoadFinished (f: () -> Unit) : SamaRvAdapter { this.onLoadFinished = f; return this }

    private fun getItemStableId(listItem: SamaListItem): Long {
        return if(listItem.getStableId() != RecyclerView.NO_ID)
            listItem.getStableId()
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
        runOnUi { liveDataItems?.removeObserver(liveDataObserver) }
        runOnUi { liveDataPagedItems?.removeObserver(pagedLiveDataObserver) }
        //putting try blocks: if object is destroyed and variables (lists) are destroyed before finishing this function, there could be some crash
        items.forEach { tryOrNull { it.onStop() } }
        lazyInitializedItemCacheMap.clear()
        coroutineContext.cancelChildren()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView?.clear()
        this.recyclerView = WeakReference(recyclerView)
        //remove the observer from the optional current liveData
        runOnUi { liveDataItems?.observeForever(liveDataObserver) }
        runOnUi { liveDataPagedItems?.observeForever(pagedLiveDataObserver) }
    }

    /** Clears all data from the adapter (call it only if you know the adapter is not needed anymore!) */
    fun clear() {
        //remove the observer from the optional current liveData
        runOnUi { liveDataItems?.removeObserver(liveDataObserver) }
        runOnUi { liveDataPagedItems?.removeObserver(pagedLiveDataObserver) }

        itemUpdatedListeners.clear()
        items.forEach { it.onStop(); it.onDestroy() }
        lazyInitializedItemCacheMap.clear()
        coroutineContext.cancel()
    }

    override fun onViewRecycled(holder: SimpleViewHolder) {
        super.onViewRecycled(holder)
        getItem(holder.adapterPosition)?.onStop()
    }


    /** Returns the list of bound items to [SamaMutableListItem], if adapter hasStableId */
    @Suppress("UNCHECKED_CAST")
    fun <T> getBoundItems(): List<T> {
        val size = mutableBoundItems.size()
        val boundItems = ArrayList<T>(size)
        for(i in 0..size) {
            val key = mutableBoundItems.keyAt(i)
            boundItems.add(mutableBoundItems.get(key) as T)
        }
        return boundItems
    }

    /** Returns the stableId of the item at position [position], if available and if adapter hasStableId. */
    override fun getItemId(position: Int): Long = if(hasStableId && getItem(position) != null) getItemStableId(getItem(position)!!) else RecyclerView.NO_ID

    /** Returns all the items in the adapter */
    fun getItems(): List<SamaListItem> = this.items

    /** Returns the currently shown PagedList, but not necessarily the most recent passed via
     *  [submitList], because a diff is computed asynchronously before updating the currentList value.
     * May be null if no PagedList is being presented or adapter is not using a paged list */
    @Suppress("UNCHECKED_CAST")
    fun getPagedItems(): PagedList<SamaListItem>? = if(isPaged) mDiffer.currentList as PagedList<SamaListItem> else null

    /** Returns the item at position [position]. If the items are from a paged list the item is returned only if it was already loaded */
    private fun getItemOrNull(position: Int): SamaListItem? = tryOrNull { if(isPaged) mDiffer.currentList?.get(position) ?: items[position] else items[position] }

    /** Returns the item at position [position] */
    fun getItem(position: Int): SamaListItem? = tryOrNull { if(isPaged) mDiffer.getItem(position) else items[position] }







    //list observer stuff
    /** Function to be called when some items change */
    private fun itemRangeChanged(positionStart: Int, itemCount: Int) = runOnUi {
        notifyItemRangeChanged(positionStart, itemCount)
    }

    /** Function to be called when some items are added */
    private fun itemRangeInserted(positionStart: Int, itemCount: Int) = runOnUi { notifyItemRangeInserted(positionStart, itemCount) }

    /** Function to be called when some items are moved */
    private fun itemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int){
        itemRangeRemoved(fromPosition, itemCount)
        itemRangeInserted(toPosition, itemCount)
    }

    /** Function to be called when some items are removed */
    private fun itemRangeRemoved(positionStart: Int, itemCount: Int) = runOnUi {
        for(i in positionStart until positionStart+itemCount) {
            getItemOrNull(i)?.onStop()
        }

        notifyItemRangeRemoved(positionStart, itemCount)
    }

    /** Function to be called when the whole list changes */
    private fun dataSetChanged() {
        items.forEach { it.onStop() }
        lazyInitializedItemCacheMap.clear()
        coroutineContext.cancelChildren()
        notifyDataSetChanged()
    }



    /** Class that implement the ViewHolder of the Adapter */
    class SimpleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding: WeakReference<ViewDataBinding> = WeakReference(DataBindingUtil.bind(view))
    }

    /** Class that listens for changes of the passed list and calls the methods of the adapter */
    inner class WeakReferenceOnListChangedCallback( bindingRvAdapter: SamaRvAdapter): ObservableList.OnListChangedCallback<ObservableList<SamaListItem>>() {

        private val adapterReference = WeakReference(bindingRvAdapter)

        @Synchronized override fun onChanged(sender: ObservableList<SamaListItem>?) { runOnUi { adapterReference.get()?.dataSetChanged() } }
        @Synchronized override fun onItemRangeRemoved(sender: ObservableList<SamaListItem>?, positionStart: Int, itemCount: Int) { runOnUi { adapterReference.get()?.itemRangeRemoved(positionStart, itemCount) } }
        @Synchronized override fun onItemRangeMoved(sender: ObservableList<SamaListItem>?, fromPosition: Int, toPosition: Int, itemCount: Int) { runOnUi { adapterReference.get()?.itemRangeMoved(fromPosition, toPosition, itemCount) } }
        @Synchronized override fun onItemRangeInserted(sender: ObservableList<SamaListItem>?, positionStart: Int, itemCount: Int) { runOnUi { adapterReference.get()?.itemRangeInserted(positionStart, itemCount) } }
        @Synchronized override fun onItemRangeChanged(sender: ObservableList<SamaListItem>?, positionStart: Int, itemCount: Int) { runOnUi { adapterReference.get()?.itemRangeChanged(positionStart, itemCount) } }
    }



    inner class LIDiffCallback(private val oldList: List<SamaListItem>, private val newList: List<SamaListItem>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = if(hasStableId) getItemStableId(oldList[oldItemPosition]) == getItemStableId(newList[newItemPosition]) else true
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = oldList[oldItemPosition].contentEquals(newList[newItemPosition])
    }
}