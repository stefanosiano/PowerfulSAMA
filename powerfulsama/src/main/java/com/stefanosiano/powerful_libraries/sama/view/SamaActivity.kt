package com.stefanosiano.powerful_libraries.sama.view

import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.*
import androidx.lifecycle.LiveData
import com.stefanosiano.powerful_libraries.sama.*
import com.stefanosiano.powerful_libraries.sama.utils.*
import com.stefanosiano.powerful_libraries.sama.viewModel.SamaViewModel
import com.stefanosiano.powerful_libraries.sama.viewModel.VmResponse
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

/** Abstract Activity for all Activities to extend */
abstract class SamaActivity : AppCompatActivity(), CoroutineScope {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** Object that takes care of observing liveData and observableFields */
    private val samaObserver = SamaObserver(this)

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
        samaObserver.restartObserver()
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
//        synchronized(managedDialog) { managedDialog.forEach { it.destroy(this) } }
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
    fun <T> manageDialog(f: () -> T): T where T: SamaDialogFragment =
        f().also { dialog -> managedDialog.put(dialog.getUidInternal(), dialog) }

    /** Manages a dialogFragment, making it restore and show again if it was dismissed due to device rotation */
    internal fun manageDialogInternal(dialog: SamaDialogFragment) = managedDialog.put(dialog.getUidInternal(), dialog)

    /** Initializes the toolbar leaving the default title */
    protected fun initActivity() = initActivity("")

    /** Initializes the toolbar with the title provided */
    protected fun initActivity(titleId: Int){
        if(titleId != 0) initActivity(getString(titleId))
        else initActivity("")
    }

    /** Initializes the toolbar with the title provided */
    protected fun initActivity(title: String){
        if(title.isNotEmpty()) supportActionBar?.title = title
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        logVerbose("Selected item ${item.title}")
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true

        }
        return super.onOptionsItemSelected(item)
    }





    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Perms.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Saf.onRequestSafResult(this, requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }


    /**
     * Handles the response of the ViewModel, in case everything went alright.
     *
     * @param vmAction Action sent from the ViewModel. It will never be null.
     * @param vmData Data sent from the ViewModel. It can be null.
     * @return True to clear the response after being sent to the observer. False to retain it.
     * If false, the response should be cleared using [com.stefanosiano.powerful_libraries.sama.viewModel.SamaViewModel.clearVmResponse] method.
     *//*
    open fun handleVmResponse(vmAction: VmResponse.VmAction, vmData: Any?): Boolean {
        //This method does nothing. It's here just to have a reference to the javadoc used by extending activities
        return true
    }*/

    /** Observes the vmResponse of the [vm]. It's just a simpler way to call [SamaViewModel.observeVmResponse] */
    protected fun <A> observeVmResponse(vm: SamaViewModel<A>, f: suspend (A, Any?) -> Boolean) where A: VmResponse.VmAction {
        registeredViewModels.add(vm)
        vm.observeVmResponse(this, f)
    }

    val samaIntent
        /** Returns the intent that started this activity as [SamaIntent], allowing the use of [SamaIntent.getExtraStatic] */
        get() = SamaIntent(super.getIntent())






    /** Observes a liveData until this object is destroyed, using a custom observer. Useful when liveData is not used in a lifecycleOwner */
    protected fun <T> observe(liveData: LiveData<T>): LiveData<T> = samaObserver.observe(liveData)
    /** Observes a liveData until this object is destroyed into an observable field. Does not update the observable if the value of the liveData is null */
    protected fun <T> observeAsOf(liveData: LiveData<T>): ObservableField<T> = samaObserver.observeAsOf(liveData)
    /** Observes a liveData until this object is destroyed into an observableF. Update the observable with [defaultValue] if the value of the liveData is null */
    protected fun <T> observeAsOf(liveData: LiveData<T>, defaultValue: T): ObservableF<T> = samaObserver.observeAsOf(liveData, defaultValue)
    /** Observes a liveData until this object is destroyed, using a custom observer */
    protected fun <T> observe(liveData: LiveData<T>, observerFunction: suspend (data: T) -> Unit): LiveData<T> = samaObserver.observe(liveData, observerFunction)

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed */
    protected fun <T> observe(o: ObservableList<T>, vararg obs: Observable, obFun: suspend (data: ObservableList<T>) -> Unit): Unit where T: Any = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableInt, defValue: R, vararg obs: Observable, obFun: suspend (data: Int) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableInt, vararg obs: Observable, obFun: suspend (data: Int) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableLong, defValue: R, vararg obs: Observable, obFun: suspend (data: Long) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableLong, vararg obs: Observable, obFun: suspend (data: Long) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableByte, defValue: R, vararg obs: Observable, obFun: suspend (data: Byte) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableByte, vararg obs: Observable, obFun: suspend (data: Byte) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableChar, defValue: R, vararg obs: Observable, obFun: suspend (data: Char) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableChar, vararg obs: Observable, obFun: suspend (data: Char) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableBoolean, defValue: R, vararg obs: Observable, obFun: suspend (data: Boolean) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableBoolean, vararg obs: Observable, obFun: suspend (data: Boolean) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableFloat, defValue: R, vararg obs: Observable, obFun: suspend (data: Float) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableFloat, vararg obs: Observable, obFun: suspend (data: Float) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R> observe(o: ObservableDouble, defValue: R, vararg obs: Observable, obFun: suspend (data: Double) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R> observe(o: ObservableDouble, vararg obs: Observable, obFun: suspend (data: Double) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableF] with initial value of [defValue] */
    protected fun <R, T> observe(o: ObservableField<T>, defValue: R, vararg obs: Observable, obFun: suspend (data: T) -> R): ObservableF<R> = samaObserver.observe(o, defValue, *obs) { obFun(it) }
    /** Observes [o] until this object is destroyed and calls [obFun] in the background, now and whenever [o] or any of [obs] change, with the current value of [o]. Does nothing if [o] is null or already changed. Returns an [ObservableField] with initial value of null */
    protected fun <R, T> observe(o: ObservableField<T>, vararg obs: Observable, obFun: suspend (data: T) -> R): ObservableField<R> = samaObserver.observe(o, *obs) { obFun(it) }

}
