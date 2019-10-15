package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stefanosiano.powerful_libraries.sama.R
import com.stefanosiano.powerful_libraries.sama.toWeakReference
import java.lang.ref.WeakReference

/** Simple RecyclerView implementation. It just have a fix to avoid memory leaks when using a long living adapter */
open class SamaRecyclerView: RecyclerView {

    private var inconsistencyWorkaround = true
    private var horizontal = false
    private var columns = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {

        val attrSet = context.theme.obtainStyledAttributes(attrs, R.styleable.SamaRecyclerView, defStyle, 0)
        columns = attrSet.getInt(R.styleable.SamaRecyclerView_srvColumns, 0)
        horizontal = attrSet.getBoolean(R.styleable.SamaRecyclerView_srvHorizontal, false)
        inconsistencyWorkaround = attrSet.getBoolean(R.styleable.SamaRecyclerView_srvInconsistencyWorkaround, true)
        attrSet.recycle()
        resetLayoutManager()
    }
    
    fun setSrvColumns(columns: Int) {
        this.columns = columns
        resetLayoutManager()
    }

    fun setSrvHorizontal(horizontal: Boolean) {
        this.horizontal = horizontal
        resetLayoutManager()
    }

    fun setSrvInconsistencyWorkaround(inconsistencyWorkaround: Boolean) {
        this.inconsistencyWorkaround = inconsistencyWorkaround
        resetLayoutManager()
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