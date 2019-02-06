package com.stefanosiano.powerful_libraries.sama.view

import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.stefanosiano.powerful_libraries.sama.runAndWait
import com.stefanosiano.powerful_libraries.sama.tryOrNull
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

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
    override val coroutineContext = coroutineJob + CoroutineExceptionHandler { _, t -> t.printStackTrace() }

    private val itemLayoutIds = SparseIntArray()

    /** Objects passed to items during onBind */
    private val initObjects = HashMap<String, Any>()

    /** handler to execute stuff in the main thread */
    private val handler: Handler = Handler(Looper.getMainLooper())

    /** callback called when the item list changes */
    private val onListChangedCallback: WeakReferenceOnListChangedCallback = WeakReferenceOnListChangedCallback(this)

    /** items shown bound to rows (implemented through ObservableArrayList) */
    private var items: ObservableList<SamaListItem> = ObservableArrayList()

    /** Coroutine contexts bound to each adapter item */
    private val contexts: HashMap<Long, CoroutineContext> = HashMap()

    /** items implemented through live data */
    private var liveDataItems: LiveData<out List<SamaListItem>>? = null

    /** map that saves variables of each row and reload them when the items are reloaded (available only if hasStableId = true) */
    private val savedItems: HashMap<Long, SparseArray<Any>> = HashMap()

    private val idsMap: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

    /** Function to be called when the liveData changes. It will reload the list */
    private val liveDataObserver = Observer<List<SamaListItem>> { if(it != null) bindItems(it, false) }

    /**
     * Class that implements RecyclerViewAdapter in an easy and powerful way!
     * hasStableId defaults to true
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
        dataSetChanged()
    }

    override fun getItemCount():Int = items.size

    /** save variables for each item that will be restored when the list is reloaded */
    private fun saveAll(){
        if(hasStableId) items.runAndWait { savedItems[getItemStableId(it)] = it.onSaveItems(SparseArray()) }
    }

    /** forces all items to reload all saved variables */
    fun forceReload() { if(hasStableId) items.runAndWait { bindItemToViewHolder(null, it, getItemContext(it)!!) } }

    override fun getItemViewType(position: Int): Int = getItem(position)?.getViewType() ?: -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        val layoutId = if(itemLayoutIds.get(viewType) != 0) itemLayoutIds.get(viewType) else itemLayoutId
        val view: View = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return SimpleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
        getItemContext(position).cancel()
        val bindJob = launch(getItemContext(position)) { holder.binding.get()?.setVariable(itemBindingId, getItem(position)) }
        bindItemToViewHolder(bindJob, getItem(position), getItemContext(position))
    }

    override fun onViewDetachedFromWindow(holder: SimpleViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val item = getItem(holder.adapterPosition) ?: return
        item.onStop()
        contexts[getItemStableId(item)]?.cancel()
    }

    /** Function that binds the item to the view holder, calling appropriate methods in the right order */
    private fun bindItemToViewHolder(job: Job?, listItem: SamaListItem?, context: CoroutineContext){
        listItem ?: return
        runBlocking(context) { job?.join() }
        listItem.onBind(initObjects)
        if(!isActive) return

        val bindBackgrounJob = launch(context) { listItem.onBindInBackground(initObjects) }

        //reload saved variables of the items
        if(hasStableId) {
            val saved = savedItems[getItemStableId(listItem)]
            if(saved != null) {
                runBlocking(context) { bindBackgrounJob.join() }
                listItem.onReload(saved)
                launch(context) { listItem.onReloadInBackground(saved) }
            }
            savedItems.remove(getItemStableId(listItem))
        }
    }

    /**
     * Binds the items of the adapter to the passed list
     *
     * @param items ObservableList that will be bound to the adapter.
     *              When it changes, the changes will be reflected to the adapter.
     */
    @Suppress("unchecked_cast")
    fun bindItems(items: ObservableList<out SamaListItem>) : SamaRvAdapter {
        this.items.removeOnListChangedCallback(onListChangedCallback)
        this.saveAll()
        this.items.clear()
        this.contexts.clear()
        this.items = items as ObservableList<SamaListItem>
        this.items.addOnListChangedCallback(onListChangedCallback)
        itemRangeInserted(0, items.size)
        return this
    }


    /**
     * Binds the items of the adapter to the passed list
     *
     * @param list List that will be bound to the adapter. Checks differences with previous items to check what changed
     * @param forceReload Force the adapter to completely reload all of its items, calling [notifyDataSetChanged]. Use it if you know all the items will change (will be faster), otherwise leave the default (false)
     */
    fun bindItems(list: List<out SamaListItem>, forceReload: Boolean = false) : SamaRvAdapter {
        this.items.removeOnListChangedCallback(onListChangedCallback)
        saveAll()

        if(forceReload) {
            this.items.clear()
            this.contexts.clear()
            this.items.addAll(list)
            dataSetChanged()
        }
        else {
            if(!isActive) return this

            val diffResult = DiffUtil.calculateDiff(LIDiffCallback(items, list))
            items.clear()
            items.addAll(list)
            diffResult.dispatchUpdatesTo(this@SamaRvAdapter)
        }
        return this
    }


    /**
     * Binds the items of the adapter to the passed list
     *
     * @param items LiveData of List that will be bound to the adapter. When it changes, the changes will be reflected to the adapter.
     */
    fun bindItems(items: LiveData<out List<SamaListItem>>?) : SamaRvAdapter {
        //remove the observer from the optional current liveData
        liveDataItems?.removeObserver(liveDataObserver)
        liveDataItems = items
        liveDataItems?.observeForever(liveDataObserver)
        return this
    }

    /** Pass the [ob] to all the items in the list, using [key] */
    fun passToItems(key: String, ob: Any) : SamaRvAdapter {
        initObjects.remove(key)
        initObjects[key] = ob
        return this
    }

    private fun getItemStableId(listItem: SamaListItem): Long {
        return if(listItem.getStableId() != RecyclerView.NO_ID)
            listItem.getStableId()
        else {
            val id = idsMap[listItem.getStableIdString()] ?: RecyclerView.NO_ID
            if(id == RecyclerView.NO_ID) idsMap[listItem.getStableIdString()] = idsMap.size.toLong()
            idsMap[listItem.getStableIdString()] ?: RecyclerView.NO_ID
        }
    }

    /** Clear liveData observer (if any) */
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        //remove the observer from the optional current liveData
        liveDataItems?.removeObserver(liveDataObserver)
        items.forEach { it.onStop() }
        contexts.values.forEach { it.cancel() }
        coroutineContext.cancelChildren()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        //remove the observer from the optional current liveData
        liveDataItems?.observeForever(liveDataObserver)
    }

    /** Clears all data from the adapter (call it only if you know the adapter is not needed anymore!) */
    fun clear() {
        //remove the observer from the optional current liveData
        liveDataItems?.removeObserver(liveDataObserver)
        items.forEach { it.onStop() }
        contexts.values.forEach { it.cancel() }
        coroutineContext.cancel()
    }

    override fun onViewRecycled(holder: SimpleViewHolder) {
        super.onViewRecycled(holder)
        getItem(holder.adapterPosition)?.onStop()
        getItemContext(holder.adapterPosition).cancel()
    }

    /** Returns the stableId of the item at position [position], if available and if adapter hasStableId. */
    override fun getItemId(position: Int): Long = if(hasStableId && getItem(position) != null) getItemStableId(getItem(position)!!) else RecyclerView.NO_ID

    /** Returns all the items in the adapter */
    fun getItems(): List<SamaListItem> = this.items

    /** Returns the item at position [position] */
    fun getItem(position: Int): SamaListItem? = tryOrNull { items[position] }

    /** Returns the coroutine context bound to the item at position [position] */
    private fun getItemContext(position: Int) = getLIContext( if(hasStableId) getItemId(position) else position.toLong() )

    /** Returns the coroutine context bound to the item [item]. Use it only if [hasStableId]!!! */
    private fun getItemContext(item: SamaListItem) = if(!hasStableId) null else getLIContext(getItemStableId(item))


    private fun getLIContext(itemId: Long): CoroutineContext {
        if(contexts[itemId]?.isActive != true)
            contexts.put( itemId, Job(coroutineJob) + CoroutineExceptionHandler { _, t -> t.printStackTrace() } )

        return contexts[itemId]!!
    }







    //list observer stuff
    /** Function to be called when some items change */
    private fun itemRangeChanged(positionStart: Int, itemCount: Int) = handler.post {
        this.saveAll()
        notifyItemRangeChanged(positionStart, itemCount)
    }

    /** Function to be called when some items are added */
    private fun itemRangeInserted(positionStart: Int, itemCount: Int) = handler.post { notifyItemRangeInserted(positionStart, itemCount) }

    /** Function to be called when some items are moved */
    private fun itemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int){
        itemRangeRemoved(fromPosition, itemCount)
        itemRangeInserted(toPosition, itemCount)
    }

    /** Function to be called when some items are removed */
    private fun itemRangeRemoved(positionStart: Int, itemCount: Int) = handler.post {
        this.saveAll()
        for(i in positionStart until positionStart+itemCount) {
            getItem(i)?.onStop()
            getItemContext(i).cancel()
        }

        notifyItemRangeRemoved(positionStart, itemCount)
    }

    /** Function to be called when the whole list changes */
    private fun dataSetChanged() {
        this.saveAll()
        items.forEach { it.onStop() }
        contexts.values.forEach { it.cancel() }
        coroutineContext.cancelChildren()
        notifyDataSetChanged()
    }



    /** Class that implement the ViewHolder of the Adapter */
    class SimpleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding: WeakReference<ViewDataBinding> = WeakReference(DataBindingUtil.bind(view))
    }

    /** Class that listens for changes of the passed list and calls the methods of the adapter */
    inner class WeakReferenceOnListChangedCallback( bindingRvAdapter: SamaRvAdapter ): ObservableList.OnListChangedCallback<ObservableList<SamaListItem>>() {

        private val adapterReference = WeakReference(bindingRvAdapter)

        @Synchronized override fun onChanged(sender: ObservableList<SamaListItem>?) { adapterReference.get()?.dataSetChanged() }
        @Synchronized override fun onItemRangeRemoved(sender: ObservableList<SamaListItem>?, positionStart: Int, itemCount: Int) { adapterReference.get()?.itemRangeRemoved(positionStart, itemCount) }
        @Synchronized override fun onItemRangeMoved(sender: ObservableList<SamaListItem>?, fromPosition: Int, toPosition: Int, itemCount: Int) { adapterReference.get()?.itemRangeMoved(fromPosition, toPosition, itemCount) }
        @Synchronized override fun onItemRangeInserted(sender: ObservableList<SamaListItem>?, positionStart: Int, itemCount: Int) { adapterReference.get()?.itemRangeInserted(positionStart, itemCount) }
        @Synchronized override fun onItemRangeChanged(sender: ObservableList<SamaListItem>?, positionStart: Int, itemCount: Int) { adapterReference.get()?.itemRangeChanged(positionStart, itemCount) }
    }



    inner class LIDiffCallback(private val oldList: List<SamaListItem>, private val newList: List<SamaListItem>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = if(hasStableId) getItemStableId(oldList[oldItemPosition]) == getItemStableId(newList[newItemPosition]) else true
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = oldList[oldItemPosition].contentEquals(newList[newItemPosition])
    }
}