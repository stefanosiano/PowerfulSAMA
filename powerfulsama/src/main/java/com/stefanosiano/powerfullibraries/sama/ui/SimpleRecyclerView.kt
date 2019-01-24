package com.stefanosiano.powerfullibraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.stefanosiano.powerfullibraries.sama.view.SamaRvAdapter

/** Simple RecyclerView implementation */
class SimpleRecyclerView: RecyclerView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    /** Empty view to show when there are no items in the adapter */
    private var emptyView: View? = null

    /** Loading view to show when adapter is loading its items */
    private var loadingView: View? = null

    /** Callback called when adapter list changes */
    private val observer = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {super.onChanged(); checkIfEmpty()}
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {super.onItemRangeInserted(positionStart, itemCount); checkIfEmpty()}
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {super.onItemRangeRemoved(positionStart, itemCount); checkIfEmpty()}
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        //removes the adapter, removing the lock on any observer/liveData
        adapter = null
    }

    /** Sets the adapter */
    override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {

        (adapter as? SamaRvAdapter)?.setRecyclerView(this)

        val oldAdapter = getAdapter()
        oldAdapter?.unregisterAdapterDataObserver(observer)
        adapter?.registerAdapterDataObserver(observer)
        super.setAdapter(adapter)

        checkIfEmpty()
    }

    /** Stops observing the list adapter changes */
    internal fun unregisterAdapterDataObserver() = adapter?.unregisterAdapterDataObserver(observer)

    /** Starts observing the list adapter changes */
    internal fun registerAdapterDataObserver() = adapter?.registerAdapterDataObserver(observer)

    /** Swaps the adapter, retaining the views and binding them again */
    override fun swapAdapter(adapter: RecyclerView.Adapter<*>?, removeAndRecycleExistingViews: Boolean) {

        val oldAdapter = getAdapter()
        oldAdapter?.unregisterAdapterDataObserver(observer)
        adapter?.registerAdapterDataObserver(observer)

        (adapter as? SamaRvAdapter)?.setRecyclerView(this)

        super.swapAdapter(adapter, removeAndRecycleExistingViews)

        checkIfEmpty()
    }

    /** Sets the [emptyView] to be shown when the adapter for this object is empty */
    fun setEmptyView(emptyView: View?) {
        this.emptyView?.visibility = View.GONE
        this.emptyView = emptyView
        checkIfEmpty()
    }

    /** Sets the [loadingView] to be shown when the adapter is loading the items */
    fun setLoadingView(loadingView: View?) {
        this.loadingView?.visibility = View.GONE
        this.loadingView = loadingView
        this.loadingView?.visibility = View.GONE
    }

    /** Shows the loading view, if available */
    fun showLoading() {
        emptyView?.visibility = View.GONE
        if(loadingView != null) visibility = View.GONE
        loadingView?.visibility = View.VISIBLE
    }

    /** Hide the loading view, if available */
    fun hideLoading() {
        loadingView?.visibility = View.GONE
        checkIfEmpty()
    }

    /** Check adapter item count and toggleDrawer visibility of empty view if the adapter is empty */
    private fun checkIfEmpty() {
        if(adapter == null) return

        loadingView?.visibility = View.GONE
        emptyView?.visibility = if (adapter?.itemCount ?: 0 > 0) View.GONE else View.VISIBLE
        visibility = if (adapter?.itemCount ?: 0  > 0) View.VISIBLE else View.GONE
    }
}