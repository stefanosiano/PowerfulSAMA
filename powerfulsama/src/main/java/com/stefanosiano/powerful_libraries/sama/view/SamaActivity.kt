package com.stefanosiano.powerful_libraries.sama.view

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.stefanosiano.powerful_libraries.sama.*
import com.stefanosiano.powerful_libraries.sama.utils.Perms
import com.stefanosiano.powerful_libraries.sama.utils.PowerfulSama
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

//    private val debugReceiver = SamaDebugReceiver()

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
        /*
        if(PowerfulSama.isAppDebug) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(ActionDbCopy)
            tryOrPrint { registerReceiver(debugReceiver, intentFilter) }
        }*/
        logVerbose("onStart")
        synchronized(observables) { observables.filter { !it.registered }.forEach { it.registered = true; it.ob.addOnPropertyChangedCallback(it.callback) } }
        synchronized(listObservables) { listObservables.filter { !it.registered }.forEach { it.registered = true; it.ob.addOnListChangedCallback(it.callback) } }
        synchronized(registeredViewModels) { registeredViewModels.forEach { it.restartObserving() } }
    }

    override fun onPause() {
        super.onPause()
        logVerbose("onPause")
    }

    override fun onStop() {
        super.onStop()
//        if(PowerfulSama.isAppDebug) { tryOrPrint { unregisterReceiver(debugReceiver) } }
        logVerbose("onStop")
        synchronized(observables) { observables.filter { it.registered }.forEach { it.registered = false; it.ob.removeOnPropertyChangedCallback(it.callback) } }
        synchronized(listObservables) { listObservables.filter { it.registered }.forEach { it.registered = false; it.ob.removeOnListChangedCallback(it.callback) } }
        synchronized(registeredViewModels) { registeredViewModels.forEach { it.stopObserving() } }
    }

    override fun onDestroy() {
        super.onDestroy()
        logVerbose("onDestroy")
        synchronized(observables) { observables.forEach { it.registered = false; it.ob.removeOnPropertyChangedCallback(it.callback) } }
        observables.clear()
        synchronized(listObservables) { listObservables.forEach { it.registered = false; it.ob.removeOnListChangedCallback(it.callback) } }
        listObservables.clear()
        registeredViewModels.clear()
        coroutineContext.cancel()
    }

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
        obs.forEach { ob ->
            observablesMap[obsId] = AtomicInteger(0)
            observables.add(SamaInnerObservable(ob, ob.onChange(this) {
                //increment value of observablesMap[obsId] -> only first call can run this function
                val id = observablesMap[obsId]?.incrementAndGet() ?: 1
                if(id != 1) return@onChange
                o.let { logVerbose(it.toString()); obFun(it) }
                //clear value of observablesMap[obsId] -> everyone can run this function
                observablesMap[obsId]?.set(0)
            }))
        }

        val c = o.onAnyChange {
            launchOrNow(this) {
                observablesMap[obsId]?.set(2)
                logVerbose(o.toString())
                obFun(it)
                observablesMap[obsId]?.set(0)
            }
        }
        listObservables.add(SamaInnerListObservable(o as ObservableList<Any>, c as ObservableList.OnListChangedCallback<ObservableList<Any>>))
        if(!skipFirst)
            launchOrNow(this) { obFun(o) }
    }


    /** Observes a liveData until the ViewModel is destroyed, using a custom observer */
    @Suppress("unchecked_cast")
    protected fun <T> observe(liveData: LiveData<T>, observerFunction: suspend (data: T) -> Unit): LiveData<T> {
        liveData.observeLd(this) { launchOrNow(this) { observerFunction(it as? T ?: return@launchOrNow) } }
        return liveData
    }

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

        obs.forEach { ob ->
            observablesMap[obsId] = AtomicInteger(0)
            observables.add(SamaInnerObservable(ob, ob.onChange(this) {
                //increment value of observablesMap[obsId] -> only first call can run this function
                val id = observablesMap[obsId]?.incrementAndGet() ?: 1
                if(id != 1) return@onChange
                obValue()?.let { logVerbose(it.toString()); obFun(it) }
                //clear value of observablesMap[obsId] -> everyone can run this function
                observablesMap[obsId]?.set(0)
            }))
        }
        //sets the function to call when using an observable: it sets the observablesMap[obsId] to 2 (it won't be called by obs), run obFun and finally set observablesMap[obsId] to 0 (callable by everyone)
        when(o) {
            is ObservableInt -> {
                observables.add(SamaInnerObservable(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableShort -> {
                observables.add(SamaInnerObservable(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableLong -> {
                observables.add(SamaInnerObservable(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableFloat -> {
                observables.add(SamaInnerObservable(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableDouble -> {
                observables.add(SamaInnerObservable(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableBoolean -> {
                observables.add(SamaInnerObservable(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableByte -> {
                observables.add(SamaInnerObservable(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
            is ObservableField<*> -> {
                observables.add(SamaInnerObservable(o, o.addOnChangedAndNow (this, skipFirst) {
                    observablesMap[obsId]?.set(2)
                    obValue()?.let { data -> if (data == it) { logVerbose(data.toString()); obFun(data) } }
                    observablesMap[obsId]?.set(0)
                }))
            }
        }
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



    private inner class SamaInnerObservable (val ob: Observable, val callback: Observable.OnPropertyChangedCallback, var registered: Boolean = true)
    private inner class SamaInnerListObservable (val ob: ObservableList<Any>, val callback: ObservableList.OnListChangedCallback<ObservableList<Any>>, var registered: Boolean = true)


}
