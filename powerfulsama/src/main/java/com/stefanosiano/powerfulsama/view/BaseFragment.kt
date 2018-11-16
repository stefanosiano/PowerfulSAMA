package com.stefanosiano.powerfulsama.view

import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel


/**
 * Abstract Fragment for all Fragments to extend.
 */
abstract class BaseFragment: Fragment(), CoroutineScope {
    private val loggingExceptionHandler = CoroutineExceptionHandler { _, t -> t.printStackTrace() }
    override val coroutineContext = Job() + loggingExceptionHandler

    override fun onStop() {
        super.onStop()
        coroutineContext.cancel()
    }

}