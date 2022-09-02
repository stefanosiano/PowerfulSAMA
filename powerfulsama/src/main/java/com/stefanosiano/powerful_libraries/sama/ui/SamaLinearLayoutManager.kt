package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stefanosiano.powerful_libraries.sama.logExceptionWorkarounded

/**
 * Simple [LinearLayoutManager] that catches the [IndexOutOfBoundsException] occurring with inconsistency detected.
 * If the exception raises, it will only print it. If it happens: check your code! You may be making some calls in the background!
 *
 * For more info check https://stackoverflow.com/questions/31759171/recyclerview-and-java-lang-indexoutofboundsexception-inconsistency-detected-in/33822747#33822747
 */
open class SamaLinearLayoutManager : LinearLayoutManager {

    internal var disablePredictiveAnimation = false

    constructor(context: Context) : super(context)
    constructor(context: Context?, orientation: Int, reverseLayout: Boolean) : super(context, orientation, reverseLayout)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
        try { super.onLayoutChildren(recycler, state) }
        catch (e: IndexOutOfBoundsException) { logExceptionWorkarounded(e) }
    }

    override fun supportsPredictiveItemAnimations(): Boolean {
        return if(disablePredictiveAnimation) false
        else super.supportsPredictiveItemAnimations()
    }
}
