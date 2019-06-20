package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stefanosiano.powerful_libraries.sama.R

/** Simple RecyclerView implementation. It just have a fix to avoid memory leaks when using a long living adapter */
open class SamaRecyclerView: RecyclerView {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {

        val attrSet = context.theme.obtainStyledAttributes(attrs, R.styleable.SamaRecyclerView, defStyle, 0)
        val columns = attrSet.getInt(R.styleable.SamaRecyclerView_srv_columns, 0)
        val inconsistencyWorkaround = attrSet.getBoolean(R.styleable.SamaRecyclerView_srv_inconsistency_workaround, false)

        if(columns > 0) {
            when {
                columns == 1 && inconsistencyWorkaround -> layoutManager = SamaLinearLayoutManager(context)
                columns == 1 && !inconsistencyWorkaround -> layoutManager = LinearLayoutManager(context)
                columns > 1 && inconsistencyWorkaround -> layoutManager = SamaGridLayoutManager(context, columns)
                columns > 1 && !inconsistencyWorkaround -> layoutManager = GridLayoutManager(context, columns)
            }
        }

        attrSet.recycle()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        //NEEDED to avoid memory leak! Removes the adapter, removing the lock on any observer/liveData
        adapter = null
    }
}