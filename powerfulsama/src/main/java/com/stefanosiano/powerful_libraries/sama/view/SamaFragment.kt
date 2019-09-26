package com.stefanosiano.powerful_libraries.sama.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.logVerbose
import com.stefanosiano.powerful_libraries.sama.tryOrNull
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama
import kotlinx.coroutines.*


/** Abstract Fragment for all Fragments to extend */
abstract class SamaFragment: Fragment(), CoroutineScope {

    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logVerbose("onCreate")
    }

    override fun onResume() {
        super.onResume()
        logVerbose("onResume")
    }

    override fun onStart() {
        super.onStart()
        logVerbose("onStart")
    }

    override fun onPause() {
        super.onPause()
        logVerbose("onPause")
    }

    override fun onStop() {
        super.onStop()
        logVerbose("onStop")
    }

    override fun onDestroy() {
        logVerbose("onDestroy")
        super.onDestroy()
        coroutineContext.cancel()
    }

    /** Used by extending classes to programmatically clear references used by the fragment (does nothing by default) */
    open fun clear() {}

    /** Removes this fragment from the stack of the fragment manager */
    fun remove() { tryOrNull { fragmentManager?.beginTransaction()?.remove(this)?.commit() } }
}