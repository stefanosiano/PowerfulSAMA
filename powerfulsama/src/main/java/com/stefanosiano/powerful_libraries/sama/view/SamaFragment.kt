package com.stefanosiano.powerful_libraries.sama.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.logVerbose
import com.stefanosiano.powerful_libraries.sama.tryOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** Abstract Fragment for all Fragments to extend. */
abstract class SamaFragment: Fragment(), CoroutineScope {

    internal var logTag: String? = null

    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logVerbose("$logTag: onCreate")
    }

    override fun onResume() {
        super.onResume()
        logVerbose("$logTag: onResume")
    }

    override fun onStart() {
        super.onStart()
        logVerbose("$logTag: onStart")
    }

    override fun onPause() {
        super.onPause()
        logVerbose("$logTag: onPause")
    }

    override fun onStop() {
        super.onStop()
        logVerbose("$logTag: onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        logVerbose("$logTag: onDestroy")
        coroutineContext.cancel()
    }

    /** Used by extending classes to programmatically clear references used by the fragment (does nothing by default). */
    open fun clear() {}

    /** Removes this fragment from the stack of the fragment manager. */
    fun remove() { tryOrNull { fragmentManager?.beginTransaction()?.remove(this)?.commit() } }

}
