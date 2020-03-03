package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import android.util.SparseIntArray
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stefanosiano.powerful_libraries.sama.R
import com.stefanosiano.powerful_libraries.sama.toWeakReference
import com.stefanosiano.powerful_libraries.sama.view.SamaRvAdapter
import java.lang.ref.WeakReference

/** Simple RecyclerView implementation. It just have a fix to avoid memory leaks when using a long living adapter */
open class SamaRecyclerView: RecyclerView {

    private var inconsistencyWorkaround = true
    private var horizontal = false
    private var autoDetach = false
    private var columns = 0

    private val spans = SparseIntArray()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {

        val attrSet = context.theme.obtainStyledAttributes(attrs, R.styleable.SamaRecyclerView, defStyle, 0)
        columns = attrSet.getInt(R.styleable.SamaRecyclerView_srvColumns, 0)
        horizontal = attrSet.getBoolean(R.styleable.SamaRecyclerView_srvHorizontal, false)
        autoDetach = attrSet.getBoolean(R.styleable.SamaRecyclerView_srvAutoDetach, true)
        inconsistencyWorkaround = attrSet.getBoolean(R.styleable.SamaRecyclerView_srvInconsistencyWorkaround, true)
        attrSet.recycle()
        resetLayoutManager()
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        super.setAdapter(adapter)

        if(columns > 1 && adapter is SamaRvAdapter) { (layoutManager as? GridLayoutManager)?.let {
            adapter.recyclerViewColumnCount = columns

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
                    return res
                }
            }
        } }
    }

    override fun onDetachedFromWindow() {
        if(autoDetach) adapter = null
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

}