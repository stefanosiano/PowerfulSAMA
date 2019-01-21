package com.stefanosiano.powerfullibraries.sama.view

import androidx.fragment.app.Fragment
import com.stefanosiano.powerfullibraries.sama.tryOrNull
import kotlinx.coroutines.*


/** Abstract Fragment for all Fragments to extend */
abstract class BaseFragment: Fragment(), CoroutineScope {

    private val loggingExceptionHandler = CoroutineExceptionHandler { _, t -> t.printStackTrace() }
    override val coroutineContext = SupervisorJob() + loggingExceptionHandler

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel()
    }

    /** Used by extending classes to programmatically clear references used by the fragment (does nothing by default) */
    open fun clear() {}

    /** Removes this fragment from the stack of the fragment manager */
    fun remove() { tryOrNull { fragmentManager?.beginTransaction()?.remove(this)?.commit() } }
}