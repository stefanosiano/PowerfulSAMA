package com.stefanosiano.powerful_libraries.sama.view

import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.*
import androidx.lifecycle.LiveData
import com.stefanosiano.powerful_libraries.sama.coroutineSamaHandler
import com.stefanosiano.powerful_libraries.sama.forEach
import com.stefanosiano.powerful_libraries.sama.logVerbose
import com.stefanosiano.powerful_libraries.sama.utils.*
import com.stefanosiano.powerful_libraries.sama.utils.SamaActivityCallback
import com.stefanosiano.powerful_libraries.sama.viewModel.SamaViewModel
import com.stefanosiano.powerful_libraries.sama.viewModel.VmAction
import com.stefanosiano.powerful_libraries.sama.viewModel.VmResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.atomic.AtomicInteger

/** Abstract Activity for all Activities to extend */
abstract class SamaActivity : AppCompatActivity(), CoroutineScope {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** Object that takes care of observing liveData and observableFields */
    private val samaObserver: SamaObserver = SamaObserverImpl()

    private val registeredViewModels = ArrayList<SamaViewModel<*>>()

    private val registeredCallbacks = ArrayList<SamaActivityCallback>()

    private val managedDialog = SparseArray<SamaDialogFragment>()


    companion object {
        /** Request codes used to pass to activity's onRequestPermissionsResult method */
        internal val samaRequestCodes = AtomicInteger(42000)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logVerbose("onCreate")
        samaObserver.initObserver(this)
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
        samaObserver.startObserver()
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
        samaObserver.stopObserver()
        synchronized(registeredViewModels) { registeredViewModels.forEach { it.stopObserving() } }
        synchronized(registeredCallbacks) { registeredCallbacks.forEach { it.onStop(this) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        logVerbose("onDestroy")
        samaObserver.destroyObserver()
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

    internal fun registerSamaCallback(cb: SamaActivityCallback) = synchronized(registeredCallbacks) { registeredCallbacks.add(cb) }
    internal fun unregisterSamaCallback(cb: SamaActivityCallback) = synchronized(registeredCallbacks) { registeredCallbacks.remove(cb) }

    /** Manages a dialogFragment, making it restore and show again if it was dismissed due to device rotation */
    fun <T: SamaDialogFragment> manageDialog(f: () -> T): T =
        f().also { dialog -> managedDialog.put(dialog.getUidInternal(), dialog) }

    /** Manages a dialogFragment, making it restore and show again if it was dismissed due to device rotation */
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


    @Deprecated("Use onVmAction")
    /** Observes the vmResponse of the [vm]. It's just a simpler way to call [SamaViewModel.observeVmResponse]. Call it on Ui thread */
    protected fun <A: VmAction> observeVmResponse(vm: SamaViewModel<A>, f: suspend (A, Any?) -> Boolean) {
        registeredViewModels.add(vm)
        vm.observeVmResponse(this, f)
    }

    /** Observes the vmResponse of the [vm]. It's just a simpler way to call [SamaViewModel.observeVmResponse]. Call it on Ui thread */
    protected fun <A: VmAction> onVmAction(vm: SamaViewModel<A>, f: (A) -> Unit) {
        registeredViewModels.add(vm)
        vm.onVmAction(this, f)
    }

    val samaIntent
        /** Returns the intent that started this activity as [SamaIntent], allowing the use of [SamaIntent.getExtraStatic] */
        get() = SamaIntent(super.getIntent())






    /** Observes a liveData until this object is destroyed into an observable field. Does not update the observable if the value of the liveData is null */
    protected fun <T> observeAsOf(liveData: LiveData<T>): ObservableField<T> = samaObserver.observeAsOf(liveData)
    /** Observes a liveData until this object is destroyed, using a custom observer */
    protected fun <T> observe(liveData: LiveData<T>, vararg obs: Observable, observerFunction: (data: T) -> Unit): LiveData<T> = samaObserver.observe(liveData, *obs) { observerFunction(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed */
    protected fun <T> observe(o: ObservableList<T>, vararg obs: Observable, obFun: (data: List<T>) -> Unit): Unit = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableInt, vararg obs: Observable, obFun: (data: Int) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableLong, vararg obs: Observable, obFun: (data: Long) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableByte, vararg obs: Observable, obFun: (data: Byte) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableChar, vararg obs: Observable, obFun: (data: Char) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableBoolean, vararg obs: Observable, obFun: (data: Boolean) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableFloat, vararg obs: Observable, obFun: (data: Float) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableDouble, vararg obs: Observable, obFun: (data: Double) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R, T> observe(o: ObservableField<T>, vararg obs: Observable, obFun: (data: T) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes the flow [f] until this object is destroyed and calls [obFun] in the background, now and whenever [f] or any of [obs] change, with the current value of [f]. Does nothing if [f] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R, T> observe(f: Flow<T>, vararg obs: Observable, obFun: (data: T) -> R): ObservableField<R> = samaObserver.observe(f, *obs) { obFun(it) }

    /** Run [f] to get a [LiveData] every time any of [o] or [obs] changes, removing the old one. It return a [LiveData] of the same type as [f] */
    protected fun <T> observeAndReloadLiveData(o: ObservableField<*>, vararg obs: Observable, f: () -> LiveData<T>?): LiveData<T> = samaObserver.observeAndReloadLiveData(o, *obs) { f() }

}
