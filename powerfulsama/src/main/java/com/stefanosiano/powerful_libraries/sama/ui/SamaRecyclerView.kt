package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import android.util.SparseIntArray
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stefanosiano.powerful_libraries.sama.R
import com.stefanosiano.powerful_libraries.sama.findActivity
import com.stefanosiano.powerful_libraries.sama.utils.SamaActivityCallback
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import com.stefanosiano.powerful_libraries.sama.view.SamaRvAdapter

/** Simple RecyclerView implementation. It just have a fix to avoid memory leaks when using a long living adapter */
open class SamaRecyclerView: RecyclerView {

    private var inconsistencyWorkaround = true
    private var disableAdapterAutoStop = false
    private var disablePredictiveAnimation = false
    private var horizontal = false
    private var autoDetach = false
    private var columns = 0

    private val spans = SparseIntArray()
    private val activityCallback = SamaActivityCallback(
        onStart = { if(!disableAdapterAutoStop) (adapter as? SamaRvAdapter)?.restartLiveDataObservers() },
        onStop = { if(!disableAdapterAutoStop) (adapter as? SamaRvAdapter)?.stopLiveDataObservers() }
    )

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {

        val attrSet = context.theme.obtainStyledAttributes(attrs, R.styleable.SamaRecyclerView, defStyle, 0)
        columns = attrSet.getInt(R.styleable.SamaRecyclerView_srvColumns, columns)
        horizontal = attrSet.getBoolean(R.styleable.SamaRecyclerView_srvHorizontal, horizontal)
        autoDetach = attrSet.getBoolean(R.styleable.SamaRecyclerView_srvAutoDetach, autoDetach)
        inconsistencyWorkaround = attrSet.getBoolean(R.styleable.SamaRecyclerView_srvInconsistencyWorkaround, inconsistencyWorkaround)
        disableAdapterAutoStop = attrSet.getBoolean(R.styleable.SamaRecyclerView_srvDisableAdapterAutoStop, disableAdapterAutoStop)
        disablePredictiveAnimation = attrSet.getBoolean(R.styleable.SamaRecyclerView_srvDisablePredictiveAnimation, disablePredictiveAnimation)
        attrSet.recycle()
        resetLayoutManager()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (context.findActivity() as? SamaActivity)?.registerSamaCallback(activityCallback)
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        super.setAdapter(adapter)

        (adapter as? SamaRvAdapter)?.recyclerViewColumnCount = columns
        if(columns > 1 && adapter is SamaRvAdapter) { (layoutManager as? GridLayoutManager)?.let {

            it.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val requestedSpan: Int = adapter.getItemSpanSize(position, columns)
                    val nextRequestedSpan: Int = adapter.getItemSpanSize(position+1, columns)
                    var currentPosSpan = 0
                    for(i in 0 until position) currentPosSpan += spans[i]
                    val rem = currentPosSpan.rem(columns)
                    val remNext = (currentPosSpan+requestedSpan).rem(columns)

                    val res = when {
                        requestedSpan == -1 || nextRequestedSpan == -1 -> requestedSpan
                        rem <= columns - requestedSpan && remNext <= columns - nextRequestedSpan -> requestedSpan
                        else -> columns-rem
                    }
                    spans.put(position, res)
                    (adapter as? SamaRvAdapter)?.setSpannedSize(position, res)
                    return res.coerceAtLeast(1)
                }
            }
        } }
    }

    override fun onDetachedFromWindow() {
        if(autoDetach) adapter = null
        (context.findActivity() as? SamaActivity)?.unregisterSamaCallback(activityCallback)
        super.onDetachedFromWindow()
    }

    fun setSrvColumns(columns: Int) {
        this.columns = columns
        (adapter as? SamaRvAdapter?)?.recyclerViewColumnCount = columns
        resetLayoutManager()
    }

    fun getColumnCount() = if(columns == 0) 0 else columns

    fun setSrvHorizontal(horizontal: Boolean) {
        this.horizontal = horizontal
        resetLayoutManager()
    }

    fun setSrvInconsistencyWorkaround(inconsistencyWorkaround: Boolean) {
        this.inconsistencyWorkaround = inconsistencyWorkaround
        resetLayoutManager()
    }

    /** Call [LinearLayoutManager.scrollToPositionWithOffset] if the underlying layoutManager is a [LinearLayoutManager].
     * Otherwise it calls [scrollToPosition] */
    fun scrollToPositionWithOffset(position: Int, offset: Int = 0) {
        if(layoutManager is LinearLayoutManager)
            (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, offset)
        else
            scrollToPosition(position)
    }

    /** Sets whether to disable the predictive animation. Useful when items are changed and take some time to reload the adapter.
     * Used only with srvInconsistencyWorkaround. Reduces (or removes) inconsistency exceptions */
    fun setSrvDisablePredictiveAnimation(srvDisablePredictiveAnimation: Boolean) {
        this.disablePredictiveAnimation = srvDisablePredictiveAnimation
        updateDisablePredictiveAnimationInManager()
    }

    private fun resetLayoutManager() {
        val position = (layoutManager as? LinearLayoutManager?)?.findFirstVisibleItemPosition()
        recycledViewPool.clear()
        if(columns > 0 || horizontal) {
            when {
                columns == 1 && inconsistencyWorkaround -> layoutManager = SamaLinearLayoutManager(context)
                columns == 1 && !inconsistencyWorkaround -> layoutManager = LinearLayoutManager(context)
                columns > 1 && inconsistencyWorkaround -> layoutManager = SamaGridLayoutManager(context, columns)
                columns > 1 && !inconsistencyWorkaround -> layoutManager = GridLayoutManager(context, columns)
                horizontal && inconsistencyWorkaround -> layoutManager = SamaLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                horizontal && !inconsistencyWorkaround -> layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }
        }
        (layoutManager as? LinearLayoutManager?)?.scrollToPosition(position ?: 0)
    }

    private fun updateDisablePredictiveAnimationInManager() {
        (layoutManager as? SamaLinearLayoutManager)?.disablePredictiveAnimation = disablePredictiveAnimation
        (layoutManager as? SamaGridLayoutManager)?.disablePredictiveAnimation = disablePredictiveAnimation
    }

}