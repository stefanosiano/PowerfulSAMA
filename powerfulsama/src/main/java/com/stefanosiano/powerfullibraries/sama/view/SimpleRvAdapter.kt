package com.stefanosiano.powerfullibraries.sama.view

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
import androidx.recyclerview.widget.RecyclerView
import com.stefanosiano.powerfullibraries.sama.ui.SimpleRecyclerView
import com.stefanosiano.powerfullibraries.sama.runAndWait
import com.stefanosiano.powerfullibraries.sama.tryOrNull
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

/**
 * Class that implements RecyclerViewAdapter in an easy and powerful way!
 *
 * @param itemLayoutId Id of the layout of each row
 * @param itemBindingId Id of the dataBinding variable in the row layout
 * @param hasStableId Whether the adapter has stableIds (The items of the adapter must implement getStableId(), or it won't have any effect)
 */
class SimpleRvAdapter(
    private var itemLayoutId: Int,
    private val itemBindingId: Int,
    private val hasStableId: Boolean
): RecyclerView.Adapter<SimpleRvAdapter.SimpleViewHolder>(), CoroutineScope {

    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineJob + CoroutineExceptionHandler { _, t -> t.printStackTrace() }

    private val itemLayoutIds = SparseIntArray()

    /** Objects passed to items during onBind */
    private val initObjects = HashMap<String, Any>()

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

    /** handler to execute stuff in the main thread */
    private val handler: Handler = Handler(Looper.getMainLooper())

    /** callback called when the item list changes */
    private val onListChangedCallback: WeakReferenceOnListChangedCallback = WeakReferenceOnListChangedCallback(this)

    /** items shown bound to rows (implemented through ObservableArrayList) */
    private var items: ObservableList<BaseLI> = ObservableArrayList()

    /** Coroutine contexts bound to each adapter item */
    private val contexts: HashMap<Long, CoroutineContext> = HashMap()

    /** items implemented through live data */
    private var liveDataItems: LiveData<out List<BaseLI>>? = null

    /** map that saves variables of each row and reload them when the items are reloaded (available only if hasStableId = true) */
    private val savedItems: HashMap<Long, SparseArray<Any>> = HashMap()

    /** SimpleRecyclerView instance */
    private var recyclerView: WeakReference<SimpleRecyclerView>? = null


    /** Function to be called when the liveData changes. It will reload the list */
    private val liveDataObserver = Observer<List<BaseLI>> {
        //        Timber.v(it.toString())
        if(it != null) {
            recyclerView?.get()?.unregisterAdapterDataObserver()
            items.removeOnListChangedCallback(onListChangedCallback)
            saveAll()
            items.forEach { item -> item.stop() }
            contexts.values.forEach { context -> context.cancel() }
            coroutineContext.cancelChildren()
            items.clear()
            contexts.clear()
            items.addAll(it)
            recyclerView?.get()?.registerAdapterDataObserver()
            items.addOnListChangedCallback(onListChangedCallback)
            notifyDataSetChanged()
            recyclerView?.get()?.hideLoading()
        }
    }

    fun addLayoutType(viewType: Int, layoutId: Int): SimpleRvAdapter { itemLayoutIds.put(viewType, layoutId); return this }

    /** Sets the SimpleRecyclerView instance and tries to show the loadingView when loading the list */
    fun setRecyclerView(simpleRV: SimpleRecyclerView) { recyclerView = WeakReference(simpleRV) }

    /** Tries to show the loadingView of the attached recyclerView (if available) */
    fun showLoading() { recyclerView?.get()?.showLoading() }

    /** Tries to hide the loadingView of the attached recyclerView (if available) */
    fun hideLoading() { recyclerView?.get()?.hideLoading() }

    /** Changes the layout of the rows and reload the list */
    fun setItemLayoutId(itemLayoutId: Int) {
        this.itemLayoutId = itemLayoutId
        dataSetChanged()
    }

    override fun getItemCount():Int = items.size

    /** save variables for each item that will be restored when the list is reloaded */
    private fun saveAll(){
        if(hasStableId) items.runAndWait { savedItems[it.getStableId()] = it.onSaveItems(SparseArray()) }
    }

    /** forces all items to reload all saved variables */
    fun forceReload() { if(hasStableId) items.runAndWait { bindItemToViewHolder(it, getItemContext(it)!!) } }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.getViewType() ?: -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        val layoutId = if(itemLayoutIds.get(viewType) != 0) itemLayoutIds.get(viewType) else itemLayoutId
        val view: View = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return SimpleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
        holder.binding.get()?.setVariable(itemBindingId, getItem(position))
        bindItemToViewHolder(getItem(position), getItemContext(position))
    }


    /** Function that binds the item to the view holder, calling appropriate methods in the right order */
    private fun bindItemToViewHolder(listItem: BaseLI?, context: CoroutineContext){
        listItem ?: return
        listItem.bind(initObjects)
        launch(context) { listItem.bindInBackground(initObjects) }

        //reload saved variables of the items
        if(hasStableId) {
            val saved = savedItems[listItem.getStableId()]
            if(saved != null) {
                listItem.reload(saved)
                launch(context) { listItem.reloadInBackground(saved) }
            }
            savedItems.remove(listItem.getStableId())
        }
    }

    /**
     * Binds the items of the adapter to the passed list
     *
     * @param items ObservableList that will be bound to the adapter.
     *              When it changes, the changes will be reflected to the adapter.
     */
    @Suppress("unchecked_cast")
    fun bindItems(items: ObservableList<out BaseLI>) : SimpleRvAdapter {
        recyclerView?.get()?.showLoading()
        recyclerView?.get()?.unregisterAdapterDataObserver()
        this.items.removeOnListChangedCallback(onListChangedCallback)
        this.saveAll()
        this.items.clear()
        this.contexts.clear()
        this.items = items as ObservableList<BaseLI>
        recyclerView?.get()?.registerAdapterDataObserver()
        this.items.addOnListChangedCallback(onListChangedCallback)
        itemRangeInserted(0, items.size)
        recyclerView?.get()?.hideLoading()
        return this
    }


    /**
     * Binds the items of the adapter to the passed list
     *
     * @param items LiveData of List that will be bound to the adapter.
     *              When it changes, the changes will be reflected to the adapter.
     */
    fun bindItems(items: LiveData<out List<BaseLI>>?) : SimpleRvAdapter {
        //remove the observer from the optional current liveData
        liveDataItems?.removeObserver(liveDataObserver)
        liveDataItems = items
        recyclerView?.get()?.showLoading()
        liveDataItems?.observeForever(liveDataObserver)
        return this
    }

    /** Pass the [ob] to all the items in the list, using [key] */
    fun passToItems(key: String, ob: Any) : SimpleRvAdapter {
        initObjects.remove(key)
        initObjects[key] = ob
        return this
    }

    /** Clear liveData observer (if any) */
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        //remove the observer from the optional current liveData
        liveDataItems?.removeObserver(liveDataObserver)
        items.forEach { it.stop() }
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
        items.forEach { it.stop() }
        contexts.values.forEach { it.cancel() }
        coroutineContext.cancel()
    }

    override fun onViewRecycled(holder: SimpleViewHolder) {
        super.onViewRecycled(holder)
        getItem(holder.adapterPosition)?.stop()
        getItemContext(holder.adapterPosition).cancel()
    }

    /** Returns the stableId of the item at position [position], if available and if adapter hasStableId. */
    override fun getItemId(position: Int): Long = if(hasStableId) getItem(position)?.getStableId() ?: RecyclerView.NO_ID else RecyclerView.NO_ID

    /** Returns all the items in the adapter */
    fun getItems(): List<BaseLI> = this.items

    /** Returns the item at position [position] */
    fun getItem(position: Int): BaseLI? = tryOrNull { items[position] }

    /** Returns the coroutine context bound to the item at position [position] */
    private fun getItemContext(position: Int): CoroutineContext {
        val id = if(hasStableId) getItemId(position) else position.toLong()
        if(contexts[id] == null || contexts[id]?.isActive != true) {
            val c = Job(coroutineJob) + CoroutineExceptionHandler { _, t -> t.printStackTrace() }
            if(contexts[id]?.isActive != true) contexts.put(id, c)
        }
        return contexts[id]!!
    }

    /** Returns the coroutine context bound to the item [item]. Use it only if [hasStableId]!!! */
    private fun getItemContext(item: BaseLI): CoroutineContext? {
        if(!hasStableId) return null
        if(contexts[item.getStableId()] == null || contexts[item.getStableId()]?.isActive != true) {
            val c = Job(coroutineJob) + CoroutineExceptionHandler { _, t -> t.printStackTrace() }
            if(contexts[item.getStableId()]?.isActive != true) contexts.put(item.getStableId(), c)
        }
        return contexts[item.getStableId()]!!
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
        var i = positionStart
        while(i < positionStart + itemCount) {
            getItem(i)?.stop()
            getItemContext(i).cancel()
            i++
        }

        notifyItemRangeRemoved(positionStart, itemCount)
    }

    /** Function to be called when the whole list changes */
    private fun dataSetChanged() {
        this.saveAll()
        recyclerView?.get()?.recycledViewPool?.clear()
        items.forEach { it.stop() }
        contexts.values.forEach { it.cancel() }
        coroutineContext.cancelChildren()
        notifyDataSetChanged()
    }



    /** Class that implement the ViewHolder of the Adapter */
    class SimpleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding: WeakReference<ViewDataBinding> = WeakReference(DataBindingUtil.bind(view))
    }

    /** Class that listens for changes of the passed list and calls the methods of the adapter */
    inner class WeakReferenceOnListChangedCallback( bindingRvAdapter: SimpleRvAdapter ): ObservableList.OnListChangedCallback<ObservableList<BaseLI>>() {

        private val adapterReference = WeakReference(bindingRvAdapter)

        @Synchronized override fun onChanged(sender: ObservableList<BaseLI>?) { adapterReference.get()?.dataSetChanged() }
        @Synchronized override fun onItemRangeRemoved(sender: ObservableList<BaseLI>?, positionStart: Int, itemCount: Int) { adapterReference.get()?.itemRangeRemoved(positionStart, itemCount) }
        @Synchronized override fun onItemRangeMoved(sender: ObservableList<BaseLI>?, fromPosition: Int, toPosition: Int, itemCount: Int) { adapterReference.get()?.itemRangeMoved(fromPosition, toPosition, itemCount) }
        @Synchronized override fun onItemRangeInserted(sender: ObservableList<BaseLI>?, positionStart: Int, itemCount: Int) { adapterReference.get()?.itemRangeInserted(positionStart, itemCount) }
        @Synchronized override fun onItemRangeChanged(sender: ObservableList<BaseLI>?, positionStart: Int, itemCount: Int) { adapterReference.get()?.itemRangeChanged(positionStart, itemCount) }
    }

}