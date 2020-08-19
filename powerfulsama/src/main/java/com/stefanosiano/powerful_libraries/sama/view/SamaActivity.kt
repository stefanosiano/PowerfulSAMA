package com.stefanosiano.powerful_libraries.sama.view

import android.os.Bundle
import android.os.PersistableBundle
import android.util.SparseArray
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.stefanosiano.powerful_libraries.sama.*
import com.stefanosiano.powerful_libraries.sama.utils.ObservableF
import com.stefanosiano.powerful_libraries.sama.utils.Perms
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama
import com.stefanosiano.powerful_libraries.sama.utils.SamaActivityCallback
import com.stefanosiano.powerful_libraries.sama.viewModel.SamaViewModel
import com.stefanosiano.powerful_libraries.sama.viewModel.VmResponse
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Abstract Activity for all Activities to extend */
abstract class SamaActivity : AppCompatActivity(), CoroutineScope {
    private val coroutineJob: Job = SupervisorJob()
    override val coroutineContext = coroutineSamaHandler(coroutineJob)

    /** List of observable callbacks that will be observed until the viewModel is destroyed */
    private val observables = ArrayList<SamaInnerObservable>()
    /** List of observable lists callbacks that will be observed until the viewModel is destroyed */
    private val listObservables = ArrayList<SamaInnerListObservable>()

    private val observablesMap = ConcurrentHashMap<Long, AtomicInteger>()
    private val observablesId = AtomicLong(0)

    private val registeredViewModels = ArrayList<SamaViewModel<*>>()

    private val registeredCallbacks = ArrayList<SamaActivityCallback>()

    private val managedDialog = SparseArray<SamaDialogFragment<*>>()

    /** flag to know if the activity was stopped */
    private var isStopped = false

    /** Delay in milliseconds after which a function in "observe(ob, ob, ob...)" can be called again.
     * Used to avoid calling the same method multiple times due to observing multiple variables */
    protected var multiObservableDelay: Long = 100L


    companion object {
        /** Request codes used to pass to activity's onRequestPermissionsResult method */
        internal val samaRequestCodes = AtomicInteger(42000)
    }

//    private val debugReceiver = SamaDebugReceiver()

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
        isStopped = false
        super.onStart()
        /*
        if(PowerfulSama.isAppDebug) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(ActionDbCopy)
            tryOrPrint { registerReceiver(debugReceiver, intentFilter) }
        }*/
        logVerbose("onStart")

        synchronized(observables) { observables.filter { !it.registered }.forEach { it.registered = true; it.ob.addOnPropertyChangedCallback(it.callback); launch { it.f() } } }
        synchronized(listObservables) { listObservables.filter { !it.registered }.forEach { it.registered = true; it.ob.addOnListChangedCallback(it.callback); launch { it.f() } } }
        synchronized(registeredViewModels) { registeredViewModels.forEach { it.restartObserving() } }
        synchronized(registeredCallbacks) { registeredCallbacks.forEach { it.onStart(this) } }
    }

    override fun onPause() {
        super.onPause()
        logVerbose("onPause")
        synchronized(registeredCallbacks) { registeredCallbacks.forEach { it.onPause(this) } }
    }

    override fun onStop() {
        isStopped = true
        super.onStop()
//        if(PowerfulSama.isAppDebug) { tryOrPrint { unregisterReceiver(debugReceiver) } }
        logVerbose("onStop")
        synchronized(observables) { observables.filter { it.registered }.forEach { it.registered = false; it.ob.removeOnPropertyChangedCallback(it.callback) } }
        synchronized(listObservables) { listObservables.filter { it.registered }.forEach { it.registered = false; it.ob.removeOnListChangedCallback(it.callback) } }
        synchronized(registeredViewModels) { registeredViewModels.forEach { it.stopObserving() } }
        synchronized(registeredCallbacks) { registeredCallbacks.forEach { it.onStop(this) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        logVerbose("onDestroy")
        synchronized(observables) { observables.forEach { it.registered = false; it.ob.removeOnPropertyChangedCallback(it.callback) } }
        observables.clear()
        synchronized(listObservables) { listObservables.forEach { it.registered = false; it.ob.removeOnListChangedCallback(it.callback) } }
        listObservables.clear()
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
    fun <T> manageDialog(f: () -> T): T where T: SamaDialogFragment<*> =
        f().also { dialog -> managedDialog.put(dialog.getUidInternal(), dialog) }

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











    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    @Suppress("UNCHECKED_CAST")
    protected fun <T> observe(o: ObservableList<T>, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: ObservableList<T>) -> Unit): Unit where T: Any {
        val obsId = observablesId.incrementAndGet()
        observablesMap[obsId] = AtomicInteger(0)

        val f: suspend () -> Unit = {
            observablesMap[obsId]?.set(2)
            o.let { logVerbose(it.toString()); obFun(it) }
            if(multiObservableDelay > 0)
                delay(multiObservableDelay)
            observablesMap[obsId]?.set(0)
        }

        obs.forEach { ob ->
            observables.add(SamaInnerObservable(ob, f, ob.onChange(this) {
                //increment value of observablesMap[obsId] -> only first call can run this function
                val id = observablesMap[obsId]?.incrementAndGet() ?: 1
                if(id != 1) return@onChange
                o.let { logVerbose(it.toString()); obFun(it) }
                //clear value of observablesMap[obsId] -> everyone can run this function
                if(multiObservableDelay > 0)
                    delay(multiObservableDelay)
                observablesMap[obsId]?.set(0)
            }))
        }

        val c = o.onAnyChange {
            launchOrNow(this) {
                //increment value of observablesMap[obsId] -> only first call can run this function
                val id = observablesMap[obsId]?.incrementAndGet() ?: 1
                if(id != 1) return@launchOrNow
                logVerbose(o.toString())
                obFun(it)
                //clear value of observablesMap[obsId] -> everyone can run this function
                if(multiObservableDelay > 0)
                    delay(multiObservableDelay)
                observablesMap[obsId]?.set(0)
            }
        }
        listObservables.add(SamaInnerListObservable(o as ObservableList<Any>, f, c as ObservableList.OnListChangedCallback<ObservableList<Any>>))
        if(!skipFirst)
            launchOrNow(this) { obFun(o) }
    }


    /** Observes a liveData until the ViewModel is destroyed, using a custom observer */
    @Suppress("unchecked_cast")
    protected fun <T> observe(liveData: LiveData<T>, observerFunction: suspend (data: T) -> Unit): LiveData<T> {
        liveData.observeLd(this) { launchOrNow(this) { observerFunction(it as? T ?: return@launchOrNow) } }
        return liveData
    }


    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableByte, f: (Byte) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableInt, f: (Int) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableShort, f: (Short) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableLong, f: (Long) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableFloat, f: (Float) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableDouble, f: (Double) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableF] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeInto(ob: ObservableBoolean, f: (Boolean) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <T, R> observeInto(ob: ObservableF<T>, f: (T) -> R): ObservableF<R> = ObservableF<R>(f(ob.get())).also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableByte, f: (Byte) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableInt, f: (Int) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableShort, f: (Short) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableLong, f: (Long) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableFloat, f: (Float) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableDouble, f: (Double) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableF] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <R> observeIntoN(ob: ObservableBoolean, f: (Boolean) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableField] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <T, R> observeIntoN(ob: ObservableField<T>, f: (T?) -> R?): ObservableField<R> = ObservableField<R>().also { observe(ob) { o -> it.set(f(o)) } }

    /** Observes [ob] until the ViewModel is destroyed, mapping it to an [ObservableF] by calling [f] (in the background) now and whenever [ob] changes passing the current value of [ob] */
    protected fun <T, R> observeIntoN(ob: LiveData<T>, f: (T?) -> R?): ObservableField<R> = ObservableField<R>(f(ob.value)).also { observe(ob) { o -> it.set(f(o)) } }


    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableByte, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Byte) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableInt, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Int) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableShort, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Short) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableLong, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Long) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableFloat, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Float) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)

    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableDouble, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Double) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)


    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun observe(o: ObservableBoolean, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: Boolean) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)


    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background) if [skipFirst] is not set.
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing [o] is null or already changed.
     * If multiple [obs] change at the same time, [obFun] is called only once */
    protected fun <T> observe(o: ObservableField<T>, skipFirst: Boolean = false, vararg obs: Observable, obFun: suspend (data: T) -> Unit): Unit = observePrivate(o, { o.get() }, obFun, skipFirst, *obs)


    /** Observes a liveData until the ViewModel is destroyed and transforms it into an observable field.
     * Does not update the observable if the value of the liveData is null */
    protected fun <T> observeAsOf(liveData: LiveData<T>, defaultValue: T? = null): ObservableField<T> {
        val observable = ObservableField<T>()
        observe(liveData) { observable.set(it ?: return@observe) }
        observable.set(liveData.value ?: defaultValue ?: return observable)
        return observable
    }


    /** Observes [o] until the ViewModel is destroyed, using a custom observer, and calls [obFun] (in the background).
     * Whenever [o] or any of [obs] change, [obFun] is called with the current value of [o]. Does nothing if the value of [o] is null or already changed */
    private fun <T> observePrivate(o: Observable, obValue: () -> T?, obFun: suspend (data: T) -> Unit, skipFirst: Boolean, vararg obs: Observable) {
        val obsId = observablesId.incrementAndGet()

        val f: suspend () -> Unit = {
            observablesMap[obsId]?.set(2)
            obValue()?.let { logVerbose(it.toString()); obFun(it) }
            if(multiObservableDelay > 0)
                delay(multiObservableDelay)
            observablesMap[obsId]?.set(0)
        }

        obs.forEach { ob ->
            observablesMap[obsId] = AtomicInteger(0)
            observables.add(SamaInnerObservable(ob, f, ob.onChange(this) {
                //increment value of observablesMap[obsId] -> only first call can run this function
                val id = observablesMap[obsId]?.incrementAndGet() ?: 1
                if(id != 1) return@onChange
                obValue()?.let { logVerbose(it.toString()); obFun(it) }
                //clear value of observablesMap[obsId] -> everyone can run this function
                if(multiObservableDelay > 0)
                    delay(multiObservableDelay)
                observablesMap[obsId]?.set(0)
            }))
        }
        observables.add(SamaInnerObservable(o, f, o.addOnChangedAndNowBase (this, skipFirst) {
            //increment value of observablesMap[obsId] -> only first call can run this function
            val id = observablesMap[obsId]?.incrementAndGet() ?: 1
            if(id != 1) return@addOnChangedAndNowBase
            obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
            //clear value of observablesMap[obsId] -> everyone can run this function
            if(multiObservableDelay > 0)
                delay(multiObservableDelay)
            observablesMap[obsId]?.set(0)
        }))
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



    private inner class SamaInnerObservable (val ob: Observable, val f: suspend () -> Unit, val callback: Observable.OnPropertyChangedCallback, var registered: Boolean = true)
    private inner class SamaInnerListObservable (val ob: ObservableList<Any>, val f: suspend () -> Unit, val callback: ObservableList.OnListChangedCallback<ObservableList<Any>>, var registered: Boolean = true)


}
