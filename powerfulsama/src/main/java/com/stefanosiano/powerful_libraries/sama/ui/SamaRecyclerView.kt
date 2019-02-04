package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

/** Simple RecyclerView implementation. It just have a fix to avoid memory leaks when using a long living adapter */
class SamaRecyclerView: RecyclerView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        //NEEDED to avoid memory leak! Removes the adapter, removing the lock on any observer/liveData
        adapter = null
    }
}