package com.stefanosiano.powerful_libraries.sama.view

import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.forEach
import com.stefanosiano.powerful_libraries.sama.logVerbose
import com.stefanosiano.powerful_libraries.sama.utils.Perms
import com.stefanosiano.powerful_libraries.sama.utils.Saf
import com.stefanosiano.powerful_libraries.sama.utils.SamaActivityCallback
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserver
import com.stefanosiano.powerful_libraries.sama.utils.SamaObserverImpl
import com.stefanosiano.powerful_libraries.sama.viewModel.SamaViewModel
import com.stefanosiano.powerful_libraries.sama.viewModel.VmAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicInteger

/** Abstract Activity for all Activities to extend. */
abstract class SamaActivity : AppCompatActivity(), CoroutineScope, SamaObserver by SamaObserverImpl() {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    private val registeredViewModels = ArrayList<SamaViewModel<*>>()

    private val registeredCallbacks = ArrayList<SamaActivityCallback>()

    private val managedDialog = SparseArray<SamaDialogFragment>()

    val samaIntent
        /** Returns the intent that started this activity as [SamaIntent], allowing the use of [SamaIntent.getExtraStatic]. */
        get() = SamaIntent(super.getIntent())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logVerbose("onCreate")
        initObserver(this)
        synchronized(registeredCallbacks) { registeredCallbacks.forEach { it.onCreate(this) } }
    }

    override fun onResume() {
        super.onResume()
        logVerbose("onResume")
        synchronized(managedDialog) { managedDialog.forEach { it.onResumeRestore(this) } }
        synchronized(registeredCallbacks) { registeredCallbacks.forEach { it.onResume(this) } }
    }

    override fun onStart() {
        super.onStart()
        logVerbose("onStart")
        startObserver()
        synchronized(registeredViewModels) { registeredViewModels.forEach { it.restartObserving() } }
        synchronized(registeredCallbacks) { registeredCallbacks.forEach { it.onStart(this) } }
    }

    override fun onPause() {
        super.onPause()
        logVerbose("onPause")
        synchronized(registeredCallbacks) { registeredCallbacks.forEach { it.onPause(this) } }
    }

    override fun onStop() {
        super.onStop()
        logVerbose("onStop")
        stopObserver()
        synchronized(registeredViewModels) { registeredViewModels.forEach { it.stopObserving() } }
        synchronized(registeredCallbacks) { registeredCallbacks.forEach { it.onStop(this) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        logVerbose("onDestroy")
        destroyObserver()
        synchronized(managedDialog) { managedDialog.forEach { it.onDestroy(this) } }
        managedDialog.clear()
        synchronized(registeredCallbacks) { registeredCallbacks.forEach { it.onDestroy(this) }; registeredCallbacks.clear() }
        registeredViewModels.clear()
        coroutineContext.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        synchronized(managedDialog) { managedDialog.forEach { it.onSaveInstanceState(this) } }
        synchronized(registeredCallbacks) { registeredCallbacks.forEach { it.onSaveInstanceState(this) } }
        super.onSaveInstanceState(outState)
    }

    internal fun registerSamaCallback(cb: SamaActivityCallback) = synchronized(registeredCallbacks) {
        registeredCallbacks.add(
            cb
        )
    }

    internal fun unregisterSamaCallback(cb: SamaActivityCallback) = synchronized(registeredCallbacks) {
        registeredCallbacks.remove(
            cb
        )
    }

    /** Manages a dialogFragment, making it restore and show again if it was dismissed due to device rotation. */
    fun <T : SamaDialogFragment> manageDialog(f: () -> T): T =
        f().also { dialog -> managedDialog.put(dialog.getUidInternal(), dialog) }

    /** Manages a dialogFragment, making it restore and show again if it was dismissed due to device rotation. */
    internal fun manageDialogInternal(dialog: SamaDialogFragment) = managedDialog.put(dialog.getUidInternal(), dialog)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        logVerbose("Selected item ${item.title}")
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Perms.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Saf.onRequestSafResult(this, requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    /** Observes the vmResponse of the [vm]. Call it on Ui thread. */
    protected fun <A : VmAction> onVmAction(vm: SamaViewModel<A>, f: (A) -> Unit) {
        registeredViewModels.add(vm)
        vm.onVmAction(this, f)
    }

    companion object {
        /** Request codes used to pass to activity's onRequestPermissionsResult method. */
        internal val samaRequestCodes = AtomicInteger(42000)
    }
}
