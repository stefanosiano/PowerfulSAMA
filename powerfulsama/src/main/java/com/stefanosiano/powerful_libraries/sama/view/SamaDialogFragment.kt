package com.stefanosiano.powerful_libraries.sama.view

import android.view.View
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.view.SimpleSamaFragment.Companion.new
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob


/** Abstract DialogFragment for all DialogFragments to extend. It includes a dialogFragment usable by subclasses
 * [layoutId] and [dataBindingId] are used to create the underlying dialogFragment.
 * If you want more control over them override [getDialogLayout] and [getDialogDataBindingId] */
abstract class SamaDialogFragment(
    private val layoutId: Int,
    private val dataBindingId: Int
): CoroutineScope {

    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    protected val dialogFragment by lazy {
        SimpleSamaDialogFragment.new(getDialogLayout(), true).with(getDialogDataBindingId(), this)
    }

    /** Return the layout used to create the dialog fragment. Defaults to [layoutId] of constructor */
    protected open fun getDialogLayout(): Int = layoutId

    /** Return the data binding id used to create the dialog fragment. Defaults to [dataBindingId] of constructor */
    protected open fun getDialogDataBindingId(): Int = dataBindingId

    /** Dismiss the dialog through [dismissAllowingStateLoss]. Always use it in [SamaActivity.onSaveInstanceState] */
    fun dismissDialog() { if(dialogFragment.isAdded) dialogFragment.dismissAllowingStateLoss() }

    /** Dismiss the dialog through [dismissAllowingStateLoss]. Always use it in [SamaActivity.onSaveInstanceState] */
    fun dismissDialog(v: View) { if(dialogFragment.isAdded) dialogFragment.dismissAllowingStateLoss() }

    fun getSamaActivity() = dialogFragment.activity as? SamaActivity?
}
